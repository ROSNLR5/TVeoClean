package com.example.core.engine

import com.example.core.adb.AdbTransportClient
import com.example.core.restore.IntegrityHashUtil
import com.example.core.restore.LocalSnapshotRecord
import com.example.core.restore.SnapshotScope
import com.example.ui.i18n.AppLanguage
import java.util.UUID

data class ChangePlanItem(
    val packageName: String,
    val titleEs: String,
    val titleEn: String,
    val wasEnabled: Boolean,
    val targetEnabled: Boolean,
    val riskSummaryEs: String,
    val riskSummaryEn: String,
    val plannedCommand: String
) {
    fun localizedTitle(lang: AppLanguage): String = if (lang == AppLanguage.ES) titleEs else titleEn
    fun localizedRisk(lang: AppLanguage): String = if (lang == AppLanguage.ES) riskSummaryEs else riskSummaryEn
}

data class ChangePlan(
    val deviceLabel: String,
    val activeHomePackage: String,
    val items: List<ChangePlanItem>
)

sealed interface BatchExecutionOutcome {
    data class Succeeded(
        val snapshot: LocalSnapshotRecord,
        val verifiedPackages: List<String>
    ) : BatchExecutionOutcome

    data class PartiallySucceeded(
        val snapshot: LocalSnapshotRecord,
        val verifiedPackages: List<String>,
        val failedPackage: String,
        val reasonEs: String,
        val reasonEn: String
    ) : BatchExecutionOutcome

    data class RejectedBeforeStart(
        val reasonEs: String,
        val reasonEn: String
    ) : BatchExecutionOutcome
}

sealed interface DifferentialRollbackOutcome {
    data class RolledBack(
        val restoredCount: Int,
        val alreadyMatchingCount: Int
    ) : DifferentialRollbackOutcome

    data class PartialOrFailed(
        val restoredCount: Int,
        val failedPackage: String,
        val reasonEs: String,
        val reasonEn: String
    ) : DifferentialRollbackOutcome
}

/**
 * Transactional Reversible Change Engine (Sections 7, 11 & 13 of Master Specification).
 *
 * Guarantees:
 * - Never uninstalls packages (`pm disable-user --user 0` / `pm enable --user 0` only).
 * - Unconditionally blocks protected critical packages and the TV's active HOME launcher.
 * - Creates a SHA-256 verified snapshot BEFORE modifying the first package.
 * - Executes in small batches (3 items) and re-queries live `pm list packages` to verify actual TV state.
 * - Halts immediately on any batch verification mismatch and offers differential rollback.
 */
class TransactionalCleanEngine(
    private val batchSize: Int = 3
) {

    fun buildPlanFromSelection(
        report: ReadOnlyDeviceReport,
        selectedPackagesToToggle: Set<String>
    ): ChangePlan {
        val items = selectedPackagesToToggle.mapNotNull { pkg ->
            // Unconditionally exclude protected or active HOME packages even if caller passed them
            if (ProtectedPackageRegistry.isProtected(pkg) || pkg == report.currentHomePackage) {
                return@mapNotNull null
            }
            val inspected = report.inspectedPackages.find { it.packageName == pkg } ?: return@mapNotNull null
            val targetEnabled = !inspected.isCurrentlyEnabled
            val cmd = if (targetEnabled) {
                "pm enable --user 0 $pkg"
            } else {
                "pm disable-user --user 0 $pkg"
            }
            ChangePlanItem(
                packageName = pkg,
                titleEs = inspected.titleEs,
                titleEn = inspected.titleEn,
                wasEnabled = inspected.isCurrentlyEnabled,
                targetEnabled = targetEnabled,
                riskSummaryEs = inspected.impactAndRiskEs,
                riskSummaryEn = inspected.impactAndRiskEn,
                plannedCommand = cmd
            )
        }
        return ChangePlan(
            deviceLabel = "${report.manufacturer} ${report.model}",
            activeHomePackage = report.currentHomePackage,
            items = items
        )
    }

    suspend fun executeConfirmedPlan(
        transport: AdbTransportClient,
        plan: ChangePlan,
        onPersistSnapshotBeforeApply: suspend (LocalSnapshotRecord) -> Unit
    ): BatchExecutionOutcome {
        if (!transport.isConnected) {
            return BatchExecutionOutcome.RejectedBeforeStart(
                reasonEs = "No hay sesión ADB activa con el televisor. No se modificó nada.",
                reasonEn = "No active ADB session with the TV. Nothing was modified."
            )
        }
        if (plan.items.isEmpty()) {
            return BatchExecutionOutcome.RejectedBeforeStart(
                reasonEs = "El plan no contiene paquetes seleccionables para modificar.",
                reasonEn = "The plan contains no selectable packages to modify."
            )
        }

        // Verify once more that zero protected packages exist in plan
        val illegalItem = plan.items.firstOrNull {
            ProtectedPackageRegistry.isProtected(it.packageName) || it.packageName == plan.activeHomePackage
        }
        if (illegalItem != null) {
            return BatchExecutionOutcome.RejectedBeforeStart(
                reasonEs = "Operación detenida: '${illegalItem.packageName}' está protegido contra desactivación.",
                reasonEn = "Operation blocked: '${illegalItem.packageName}' is guarded against disabling."
            )
        }

        // 1. Capture live state of target packages BEFORE mutating anything to build exact Snapshot
        val liveEnabledSet = queryLiveEnabledPackages(transport).getOrElse {
            return BatchExecutionOutcome.RejectedBeforeStart(
                reasonEs = "No se pudo verificar el inventario previo del televisor. No se aplicó ningún cambio.",
                reasonEn = "Could not verify prior TV package inventory. Zero changes applied."
            )
        }

        val priorStates = plan.items.associate { item ->
            item.packageName to (item.packageName in liveEnabledSet)
        }
        val snapshotId = "snap-tv-${UUID.randomUUID().toString().take(8)}"
        val serializedPrior = priorStates.entries.sortedBy { it.key }.joinToString("|") { "${it.key}=${it.value}" }
        val integrity = IntegrityHashUtil.sha256Hex("tv-packages|device=${plan.deviceLabel}|$serializedPrior")

        val snapshot = LocalSnapshotRecord(
            id = snapshotId,
            createdAtEpochMs = System.currentTimeMillis(),
            titleEs = "Antes de plan en ${plan.deviceLabel} (${plan.items.size} paquetes)",
            titleEn = "Before plan on ${plan.deviceLabel} (${plan.items.size} packages)",
            scope = SnapshotScope.TV_PACKAGES,
            deviceLabel = plan.deviceLabel,
            previousValueSummaryEs = "${plan.items.size} paquetes registrados (${priorStates.values.count { it }} activos antes del cambio)",
            previousValueSummaryEn = "${plan.items.size} packages snapshotted (${priorStates.values.count { it }} enabled prior to change)",
            appliedPackId = null,
            tvPackageEnabledStates = priorStates,
            integritySha256 = integrity,
            canRevertOffline = false
        )

        // Persist snapshot BEFORE executing the first command
        onPersistSnapshotBeforeApply(snapshot)

        val verifiedPackages = mutableListOf<String>()

        // 2. Execute in small bounded batches and re-read real TV state after every batch
        for (batch in plan.items.chunked(batchSize)) {
            for (item in batch) {
                val cmdResult = transport.setPackageEnabledState(
                    packageName = item.packageName,
                    enabled = item.targetEnabled,
                    activeHomePackage = plan.activeHomePackage
                )
                if (cmdResult.isFailure) {
                    return BatchExecutionOutcome.PartiallySucceeded(
                        snapshot = snapshot,
                        verifiedPackages = verifiedPackages,
                        failedPackage = item.packageName,
                        reasonEs = "La operación se detuvo en '${item.packageName}'. ${verifiedPackages.size} cambios verificados; puedes revertir desde el snapshot $snapshotId.",
                        reasonEn = "Operation halted on '${item.packageName}'. ${verifiedPackages.size} changes verified; you can revert from snapshot $snapshotId."
                    )
                }
            }

            // Re-read real state on TV (never trust command stdout/exit code alone!)
            val postBatchEnabled = queryLiveEnabledPackages(transport).getOrElse {
                return BatchExecutionOutcome.PartiallySucceeded(
                    snapshot = snapshot,
                    verifiedPackages = verifiedPackages,
                    failedPackage = batch.first().packageName,
                    reasonEs = "Se perdió la conexión al verificar el lote. Usa el snapshot $snapshotId para restaurar.",
                    reasonEn = "Connection lost while verifying batch. Use snapshot $snapshotId to restore."
                )
            }

            for (item in batch) {
                val isNowEnabled = item.packageName in postBatchEnabled
                if (isNowEnabled != item.targetEnabled) {
                    return BatchExecutionOutcome.PartiallySucceeded(
                        snapshot = snapshot,
                        verifiedPackages = verifiedPackages,
                        failedPackage = item.packageName,
                        reasonEs = "El televisor mantuvo '${item.packageName}' en su estado anterior (protegido por firmware). Se detuvieron los lotes restantes.",
                        reasonEn = "The TV kept '${item.packageName}' in its previous state (firmware-locked). Remaining batches were halted."
                    )
                }
                verifiedPackages.add(item.packageName)
            }
        }

        return BatchExecutionOutcome.Succeeded(
            snapshot = snapshot,
            verifiedPackages = verifiedPackages
        )
    }

    /**
     * Differential Rollback: Queries current live state on TV and only runs operations
     * for packages whose current state differs from `snapshot.tvPackageEnabledStates`.
     */
    suspend fun rollbackSnapshotOnTv(
        transport: AdbTransportClient,
        snapshot: LocalSnapshotRecord,
        activeHomePackage: String?
    ): DifferentialRollbackOutcome {
        if (!transport.isConnected) {
            return DifferentialRollbackOutcome.PartialOrFailed(
                restoredCount = 0,
                failedPackage = "",
                reasonEs = "Restauración preparada; falta conectar la TV.",
                reasonEn = "Restore prepared; waiting for TV connection."
            )
        }

        val currentEnabled = queryLiveEnabledPackages(transport).getOrElse {
            return DifferentialRollbackOutcome.PartialOrFailed(
                restoredCount = 0,
                failedPackage = "",
                reasonEs = "No se pudo leer el estado actual del televisor antes de restaurar.",
                reasonEn = "Could not read current TV state prior to rollback."
            )
        }

        var restoredCount = 0
        var alreadyMatching = 0

        for ((pkg, desiredEnabled) in snapshot.tvPackageEnabledStates) {
            val currentlyEnabled = pkg in currentEnabled
            if (currentlyEnabled == desiredEnabled) {
                alreadyMatching++
                continue
            }
            val res = transport.setPackageEnabledState(
                packageName = pkg,
                enabled = desiredEnabled,
                activeHomePackage = activeHomePackage
            )
            if (res.isFailure) {
                return DifferentialRollbackOutcome.PartialOrFailed(
                    restoredCount = restoredCount,
                    failedPackage = pkg,
                    reasonEs = "Se restauraron $restoredCount paquetes, pero '$pkg' requirió revisión.",
                    reasonEn = "Restored $restoredCount packages, but '$pkg' required review."
                )
            }
            restoredCount++
        }

        // Verify post-rollback state
        val afterEnabled = queryLiveEnabledPackages(transport).getOrNull()
        if (afterEnabled != null) {
            val mismatch = snapshot.tvPackageEnabledStates.entries.firstOrNull { (pkg, desired) ->
                (pkg in afterEnabled) != desired
            }
            if (mismatch != null) {
                return DifferentialRollbackOutcome.PartialOrFailed(
                    restoredCount = restoredCount,
                    failedPackage = mismatch.key,
                    reasonEs = "Tras restaurar, '${mismatch.key}' permaneció bloqueado por el sistema.",
                    reasonEn = "After rollback, '${mismatch.key}' remained locked by system firmware."
                )
            }
        }

        return DifferentialRollbackOutcome.RolledBack(
            restoredCount = restoredCount,
            alreadyMatchingCount = alreadyMatching
        )
    }

    private suspend fun queryLiveEnabledPackages(transport: AdbTransportClient): Result<Set<String>> {
        return transport.executeReadOnlyShell("pm list packages -e").map { raw ->
            raw.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
    }
}

package com.example.core.engine

import com.example.core.adb.AdbTransportClient
import com.example.core.platform.CapabilitySupportLevel
import com.example.core.platform.TvPlatformFamily
import com.example.ui.i18n.AppLanguage

enum class PackageClassification(val badgeEs: String, val badgeEn: String) {
    PROTECTED_CRITICAL("Protegido crítico", "Critical guarded"),
    KNOWN_SAFE_RECOMMENDATION("Recomendación reversible", "Reversible recommendation"),
    UNKNOWN_CONSERVATIVE("No verificado (Conservador)", "Unverified (Conservative)");

    fun localizedBadge(lang: AppLanguage): String = if (lang == AppLanguage.ES) badgeEs else badgeEn
}

data class InspectedPackageItem(
    val packageName: String,
    val isCurrentlyEnabled: Boolean,
    val classification: PackageClassification,
    val titleEs: String,
    val titleEn: String,
    val reasonEs: String,
    val reasonEn: String,
    val impactAndRiskEs: String,
    val impactAndRiskEn: String,
    val readEvidence: String,
    val restoreMethodCommand: String
) {
    fun localizedTitle(lang: AppLanguage): String = if (lang == AppLanguage.ES) titleEs else titleEn
    fun localizedReason(lang: AppLanguage): String = if (lang == AppLanguage.ES) reasonEs else reasonEn
    fun localizedImpact(lang: AppLanguage): String =
        if (lang == AppLanguage.ES) impactAndRiskEs else impactAndRiskEn
}

data class ReadOnlyDeviceReport(
    val endpoint: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val detectedFamily: TvPlatformFamily,
    val currentHomePackage: String,
    val totalPackagesCount: Int,
    val enabledPackagesCount: Int,
    val disabledPackagesCount: Int,
    val protectedObservedCount: Int,
    val launcherSwitchCapability: CapabilitySupportLevel,
    val inspectedPackages: List<InspectedPackageItem>,
    val inspectedAtEpochMs: Long
)

/**
 * 100% Read-Only TV Inspector (Section 7 of Master Specification).
 *
 * Executes strictly read-only ADB queries (`getprop`, `pm list packages -e`, `pm list packages -d`,
 * `cmd package resolve-activity`) and NEVER alters a single bit on the target television.
 */
class ReadOnlyTvAnalyzer {

    private data class KnownCuratedEntry(
        val titleEs: String,
        val titleEn: String,
        val reasonEs: String,
        val reasonEn: String,
        val impactEs: String,
        val impactEn: String
    )

    private val curatedReversibleCatalog: Map<String, KnownCuratedEntry> = mapOf(
        "com.google.android.tungsten.setupwraith" to KnownCuratedEntry(
            titleEs = "Asistente secundario de sugerencias",
            titleEn = "Secondary setup suggestions helper",
            reasonEs = "Servicio auxiliar post-instalación no requerido para el uso normal del televisor.",
            reasonEn = "Post-setup auxiliary service not required for normal TV operation.",
            impactEs = "Riesgo bajo · Libera procesos en segundo plano sin afectar el inicio ni las apps.",
            impactEn = "Low risk · Frees background resources without affecting Home or streaming apps."
        ),
        "com.android.tv.feedback" to KnownCuratedEntry(
            titleEs = "Formulario de comentarios del fabricante",
            titleEn = "Vendor feedback reporter",
            reasonEs = "Componente opcional para enviar encuestas al fabricante.",
            reasonEn = "Optional component used to submit vendor surveys.",
            impactEs = "Riesgo bajo · Totalmente reversible con pm enable.",
            impactEn = "Low risk · Fully reversible via pm enable."
        ),
        "com.google.android.leanbacklauncher.recommendations" to KnownCuratedEntry(
            titleEs = "Carrusel de recomendaciones promocionales",
            titleEn = "Promotional recommendations rail",
            reasonEs = "Alimenta filas promocionales en launchers Android TV clásicos.",
            reasonEn = "Populates promotional rows on classic Android TV launchers.",
            impactEs = "Riesgo bajo-medio · Oculta sugerencias automáticas en Android TV Home.",
            impactEn = "Low-medium risk · Hides automatic suggestions on Android TV Home."
        ),
        "com.sony.dtv.promos" to KnownCuratedEntry(
            titleEs = "Canal promocional Sony Bravia",
            titleEn = "Sony Bravia promotional channel",
            reasonEs = "Módulo promocional específico del fabricante detectado en perfil base.",
            reasonEn = "OEM promotional module matched in baseline profile.",
            impactEs = "Riesgo bajo · No afecta calibración de imagen ni entradas HDMI.",
            impactEn = "Low risk · Does not affect picture calibration or HDMI inputs."
        ),
        "com.tcl.partnercustomizer" to KnownCuratedEntry(
            titleEs = "Personalizador promocional de socio TCL",
            titleEn = "TCL partner promo customizer",
            reasonEs = "Añade accesos promocionales preinstalados de operador/fabricante.",
            reasonEn = "Adds preloaded partner promotional shortcuts.",
            impactEs = "Riesgo bajo · Reversible instantáneamente.",
            impactEn = "Low risk · Instantly reversible."
        )
    )

    suspend fun analyzeConnectedTv(transport: AdbTransportClient): Result<ReadOnlyDeviceReport> {
        val endpoint = transport.connectedEndpoint
            ?: return Result.failure(IllegalStateException("Sin conexión ADB activa."))

        val brand = transport.executeReadOnlyShell("getprop ro.product.manufacturer")
            .getOrElse { return Result.failure(it) }
            .trim()
            .ifEmpty { "Android TV" }

        val model = transport.executeReadOnlyShell("getprop ro.product.model")
            .getOrElse { return Result.failure(it) }
            .trim()
            .ifEmpty { "Modelo desconocido" }

        val release = transport.executeReadOnlyShell("getprop ro.build.version.release")
            .getOrDefault("")
            .trim()
            .ifEmpty { "?" }

        val sdkRaw = transport.executeReadOnlyShell("getprop ro.build.version.sdk")
            .getOrDefault("")
            .trim()
        val sdkInt = sdkRaw.toIntOrNull() ?: 0

        val enabledOut = transport.executeReadOnlyShell("pm list packages -e")
            .getOrElse { return Result.failure(it) }

        val disabledOut = transport.executeReadOnlyShell("pm list packages -d")
            .getOrDefault("")

        val homeResolveOut = transport.executeReadOnlyShell(
            "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN"
        ).getOrDefault("")

        val enabledSet = parsePmPackageList(enabledOut)
        val disabledSet = parsePmPackageList(disabledOut)
        val allPackages = (enabledSet + disabledSet).sorted()

        val detectedFamily = classifyFamily(brand, model, allPackages)
        val currentHome = parseResolvedHomePackage(homeResolveOut, allPackages)

        val launcherCapability = when (detectedFamily) {
            TvPlatformFamily.AMAZON_FIRE_OS,
            TvPlatformFamily.CHROMECAST_STREAMER -> CapabilitySupportLevel.BLOCKED_FOR_SAFETY
            TvPlatformFamily.ANDROID_GOOGLE_TV -> CapabilitySupportLevel.SUPPORTED
            else -> CapabilitySupportLevel.UNAVAILABLE
        }

        val inspectedItems = allPackages.map { pkg ->
            val isEnabled = pkg in enabledSet
            val protection = ProtectedPackageRegistry.inspectProtection(pkg)
            val curated = curatedReversibleCatalog[pkg]
            val evidenceTag = if (isEnabled) "pm list packages -e" else "pm list packages -d"

            when {
                protection != null || pkg == currentHome -> InspectedPackageItem(
                    packageName = pkg,
                    isCurrentlyEnabled = isEnabled,
                    classification = PackageClassification.PROTECTED_CRITICAL,
                    titleEs = protection?.categoryEs ?: "Launcher HOME activo",
                    titleEn = if (pkg == currentHome && protection == null) "Active HOME Launcher" else "Protected system package",
                    reasonEs = protection?.reasonEs ?: "Launcher predeterminado actual del televisor; protegido contra pérdida de inicio.",
                    reasonEn = "Critical system or active HOME package guarded unconditionally against soft-brick.",
                    impactAndRiskEs = "BLOQUEADO · No desactivar nunca sin ruta de retorno verificada.",
                    impactAndRiskEn = "GUARDED · Unconditionally blocked from disabling.",
                    readEvidence = "$evidenceTag · Guardia v${ProtectedPackageRegistry.REGISTRY_VERSION}",
                    restoreMethodCommand = "Protegido permanentemente"
                )
                curated != null -> InspectedPackageItem(
                    packageName = pkg,
                    isCurrentlyEnabled = isEnabled,
                    classification = PackageClassification.KNOWN_SAFE_RECOMMENDATION,
                    titleEs = curated.titleEs,
                    titleEn = curated.titleEn,
                    reasonEs = curated.reasonEs,
                    reasonEn = curated.reasonEn,
                    impactAndRiskEs = curated.impactEs,
                    impactAndRiskEn = curated.impactEn,
                    readEvidence = "$evidenceTag · Perfil base conservador v1.0",
                    restoreMethodCommand = "pm enable --user 0 $pkg"
                )
                else -> InspectedPackageItem(
                    packageName = pkg,
                    isCurrentlyEnabled = isEnabled,
                    classification = PackageClassification.UNKNOWN_CONSERVATIVE,
                    titleEs = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    titleEn = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    reasonEs = "Sin coincidencia en perfil verificado; un nombre de paquete por sí solo no prueba que sea bloatware.",
                    reasonEn = "Not matched in verified profile; package name alone never proves bloatware.",
                    impactAndRiskEs = "Incertidumbre marcada · No se selecciona por defecto.",
                    impactAndRiskEn = "Marked conservative · Never pre-selected by default.",
                    readEvidence = "$evidenceTag · Modo conservador",
                    restoreMethodCommand = "pm enable --user 0 $pkg"
                )
            }
        }.sortedBy { item ->
            when (item.classification) {
                PackageClassification.KNOWN_SAFE_RECOMMENDATION -> 0
                PackageClassification.PROTECTED_CRITICAL -> 1
                PackageClassification.UNKNOWN_CONSERVATIVE -> 2
            }
        }

        val protectedCount = inspectedItems.count { it.classification == PackageClassification.PROTECTED_CRITICAL }

        return Result.success(
            ReadOnlyDeviceReport(
                endpoint = endpoint,
                manufacturer = brand,
                model = model,
                androidVersion = release,
                sdkInt = sdkInt,
                detectedFamily = detectedFamily,
                currentHomePackage = currentHome,
                totalPackagesCount = allPackages.size,
                enabledPackagesCount = enabledSet.size,
                disabledPackagesCount = disabledSet.size,
                protectedObservedCount = protectedCount,
                launcherSwitchCapability = launcherCapability,
                inspectedPackages = inspectedItems,
                inspectedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    private fun parsePmPackageList(rawOutput: String): Set<String> {
        return rawOutput.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun classifyFamily(
        brand: String,
        model: String,
        packages: List<String>
    ): TvPlatformFamily {
        val b = brand.lowercase()
        val m = model.lowercase()
        if (b.contains("amazon") || packages.any { it.startsWith("com.amazon.tv") }) {
            return TvPlatformFamily.AMAZON_FIRE_OS
        }
        if (m.contains("chromecast") || m.contains("google tv streamer") || m.contains("sabrina")) {
            return TvPlatformFamily.CHROMECAST_STREAMER
        }
        return TvPlatformFamily.ANDROID_GOOGLE_TV
    }

    private fun parseResolvedHomePackage(resolveOutput: String, allPackages: List<String>): String {
        val slashLine = resolveOutput.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.contains("/") && !it.startsWith("priority=") }
        if (slashLine != null) {
            return slashLine.substringBefore("/").trim()
        }
        return when {
            "com.google.android.apps.tv.launcherx" in allPackages -> "com.google.android.apps.tv.launcherx"
            "com.google.android.tvlauncher" in allPackages -> "com.google.android.tvlauncher"
            else -> "No determinado"
        }
    }
}

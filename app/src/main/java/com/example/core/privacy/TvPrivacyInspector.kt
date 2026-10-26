package com.example.core.privacy

import com.example.core.adb.AdbTransportClient
import com.example.core.engine.PackageClassification
import com.example.core.engine.ReadOnlyDeviceReport
import com.example.core.restore.IntegrityHashUtil
import com.example.ui.i18n.AppLanguage

enum class SignalCertainty(val labelEs: String, val labelEn: String) {
    VERIFIED_FACT("Hecho consultado por ADB", "ADB-verified fact"),
    CONSERVATIVE_OBSERVATION("Observación con límite", "Bounded observation"),
    NOT_VERIFIABLE_VIA_ADB("No comprobable por ADB", "Not verifiable via ADB");

    fun localized(lang: AppLanguage): String = if (lang == AppLanguage.ES) labelEs else labelEn
}

data class PrivacySignalEntry(
    val titleEs: String,
    val titleEn: String,
    val observedValueEs: String,
    val observedValueEn: String,
    val evidenceSource: String,
    val certainty: SignalCertainty
) {
    fun localizedTitle(lang: AppLanguage): String = if (lang == AppLanguage.ES) titleEs else titleEn
    fun localizedValue(lang: AppLanguage): String = if (lang == AppLanguage.ES) observedValueEs else observedValueEn
}

data class TvPrivacyReport(
    val deviceLabel: String,
    val generatedAtEpochMs: Long,
    val signals: List<PrivacySignalEntry>
) {
    fun exportRedactedReport(lang: AppLanguage): String {
        val raw = buildString {
            appendLine("=== TVeoClean — ${if (lang == AppLanguage.ES) "Informe de Privacidad Verificable" else "Verifiable Privacy Report"} ===")
            appendLine("TV: $deviceLabel")
            signals.forEach { sig ->
                appendLine("- [${sig.certainty.localized(lang)}] ${sig.localizedTitle(lang)}: ${sig.localizedValue(lang)} (${sig.evidenceSource})")
            }
        }
        return IntegrityHashUtil.redactSensitiveDiagnostics(raw)
    }
}

/**
 * Evidence-Based Privacy Inspector (Section 15 of Master Specification).
 *
 * Separates verified device facts from conservative inferences and explicitly marks
 * hardware/firmware signals (such as physical microphone switches or panel-level ACR)
 * as NOT verifiable via ADB rather than making false privacy promises.
 */
class TvPrivacyInspector {

    suspend fun inspectConnectedTvPrivacy(
        transport: AdbTransportClient,
        latestReport: ReadOnlyDeviceReport?
    ): Result<TvPrivacyReport> {
        if (!transport.isConnected) {
            return Result.failure(IllegalStateException("Sin sesión ADB activa."))
        }

        val adbEnabled = transport.executeReadOnlyShell("settings get global adb_enabled")
            .getOrDefault("unknown").trim()
        val packageVerifier = transport.executeReadOnlyShell("settings get global package_verifier_enable")
            .getOrDefault("unknown").trim()
        val buildType = transport.executeReadOnlyShell("getprop ro.build.type")
            .getOrDefault("unknown").trim()

        val activeOptionalPromoCount = latestReport?.inspectedPackages?.count {
            it.classification == PackageClassification.KNOWN_SAFE_RECOMMENDATION && it.isCurrentlyEnabled
        } ?: 0
        val disabledOptionalPromoCount = latestReport?.inspectedPackages?.count {
            it.classification == PackageClassification.KNOWN_SAFE_RECOMMENDATION && !it.isCurrentlyEnabled
        } ?: 0

        val signals = listOf(
            PrivacySignalEntry(
                titleEs = "Depuración ADB por red local",
                titleEn = "Local network ADB debugging",
                observedValueEs = if (adbEnabled == "1") "Activa (puedes desactivarla en Opciones de desarrollador al terminar)" else "Valor reportado: $adbEnabled",
                observedValueEn = if (adbEnabled == "1") "Enabled (you can turn it off in Developer options when finished)" else "Reported value: $adbEnabled",
                evidenceSource = "settings get global adb_enabled",
                certainty = SignalCertainty.VERIFIED_FACT
            ),
            PrivacySignalEntry(
                titleEs = "Verificador de paquetes Android",
                titleEn = "Android package verifier",
                observedValueEs = if (packageVerifier == "1" || packageVerifier == "null") "Habilitado / predeterminado de fábrica ($packageVerifier)" else "Deshabilitado ($packageVerifier)",
                observedValueEn = if (packageVerifier == "1" || packageVerifier == "null") "Enabled / factory default ($packageVerifier)" else "Disabled ($packageVerifier)",
                evidenceSource = "settings get global package_verifier_enable",
                certainty = SignalCertainty.VERIFIED_FACT
            ),
            PrivacySignalEntry(
                titleEs = "Tipo de compilación del firmware",
                titleEn = "Firmware build type",
                observedValueEs = if (buildType == "user") "Producción oficial firmada ('user')" else "Compilación '$buildType'",
                observedValueEn = if (buildType == "user") "Signed production build ('user')" else "Build '$buildType'",
                evidenceSource = "getprop ro.build.type",
                certainty = SignalCertainty.VERIFIED_FACT
            ),
            PrivacySignalEntry(
                titleEs = "Módulos promocionales/encuestas conocidos",
                titleEn = "Known promotional/feedback modules",
                observedValueEs = "$activeOptionalPromoCount activos · $disabledOptionalPromoCount desactivados reversiblemente (solo sobre catálogo verificado; no garantiza ausencia total de telemetría del SO).",
                observedValueEn = "$activeOptionalPromoCount enabled · $disabledOptionalPromoCount reversibly disabled (verified catalog only; does not claim zero OS telemetry).",
                evidenceSource = "pm list packages -e / -d",
                certainty = SignalCertainty.CONSERVATIVE_OBSERVATION
            ),
            PrivacySignalEntry(
                titleEs = "Reconocimiento automático de contenido (ACR) y micrófono físico",
                titleEn = "Automatic Content Recognition (ACR) & physical mic",
                observedValueEs = "No comprobable mediante ADB sin privilegios del fabricante. Revísalo manualmente en Ajustes → Privacidad de tu televisor.",
                observedValueEn = "Cannot be verified reliably via standard ADB. Review manually under Settings → Privacy on your TV.",
                evidenceSource = "Límite arquitectónico de Android TV",
                certainty = SignalCertainty.NOT_VERIFIABLE_VIA_ADB
            )
        )

        val label = if (latestReport != null) {
            "${latestReport.manufacturer} ${latestReport.model}"
        } else {
            transport.connectedEndpoint ?: "Android TV"
        }

        return Result.success(
            TvPrivacyReport(
                deviceLabel = IntegrityHashUtil.redactSensitiveDiagnostics(label),
                generatedAtEpochMs = System.currentTimeMillis(),
                signals = signals
            )
        )
    }
}

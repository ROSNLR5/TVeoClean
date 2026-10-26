package com.example.core.restore

import com.example.ui.i18n.AppLanguage
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SnapshotScope(val labelEs: String, val labelEn: String) {
    MOBILE_UI("Apariencia App Móvil", "Mobile App Appearance"),
    TV_PACKAGES("Estado de Paquetes TV", "TV Package State"),
    TV_LAUNCHER_THEME("Tema Launcher TV", "TV Launcher Theme");

    fun localizedLabel(lang: AppLanguage): String = if (lang == AppLanguage.ES) labelEs else labelEn
}

data class LocalSnapshotRecord(
    val id: String,
    val createdAtEpochMs: Long,
    val titleEs: String,
    val titleEn: String = titleEs,
    val scope: SnapshotScope,
    val deviceLabel: String,
    val previousValueSummaryEs: String,
    val previousValueSummaryEn: String = previousValueSummaryEs,
    val appliedPackId: String?,
    val tvPackageEnabledStates: Map<String, Boolean> = emptyMap(),
    val integritySha256: String,
    val canRevertOffline: Boolean
) {
    fun localizedTitle(lang: AppLanguage): String = if (lang == AppLanguage.ES) titleEs else titleEn
    fun localizedPreviousSummary(lang: AppLanguage): String =
        if (lang == AppLanguage.ES) previousValueSummaryEs else previousValueSummaryEn

    fun formattedDate(lang: AppLanguage): String {
        val locale = if (lang == AppLanguage.ES) Locale("es", "ES") else Locale.ENGLISH
        val formatter = SimpleDateFormat("dd MMM yyyy · HH:mm", locale)
        return formatter.format(Date(createdAtEpochMs))
    }

    /**
     * Generates reproducible, secret-free ADB recovery commands for user export.
     */
    fun exportReproducibleAdbRecoveryCommands(): String {
        if (tvPackageEnabledStates.isEmpty()) {
            return "# Snapshot local ($id) — Alcance: ${scope.labelEs}\n# No requiere comandos ADB en televisor."
        }
        return buildString {
            appendLine("# TVeoClean — Instrucciones reproducibles de recuperación ADB")
            appendLine("# Snapshot ID: $id | SHA-256: $integritySha256")
            tvPackageEnabledStates.toSortedMap().forEach { (pkg, shouldBeEnabled) ->
                if (shouldBeEnabled) {
                    appendLine("adb shell pm enable --user 0 $pkg")
                } else {
                    appendLine("adb shell pm disable-user --user 0 $pkg")
                }
            }
        }.trim()
    }
}

data class AuditLogEntry(
    val id: String,
    val timestampEpochMs: Long,
    val operationEs: String,
    val operationEn: String = operationEs,
    val targetLabel: String,
    val outcomeEs: String,
    val outcomeEn: String = outcomeEs,
    val snapshotRefId: String? = null
) {
    fun localizedOperation(lang: AppLanguage): String = if (lang == AppLanguage.ES) operationEs else operationEn
    fun localizedOutcome(lang: AppLanguage): String = if (lang == AppLanguage.ES) outcomeEs else outcomeEn

    fun formattedTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        return formatter.format(Date(timestampEpochMs))
    }
}

object IntegrityHashUtil {
    fun sha256Hex(payload: String): String {
        return sha256BytesHex(payload.toByteArray(Charsets.UTF_8))
    }

    fun sha256BytesHex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    fun redactSensitiveDiagnostics(rawText: String): String {
        val ipv4Pattern = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}(?::\d{2,5})?\b""")
        val pinPattern = Regex("""\bPIN[:\s]+\d{6}\b""", RegexOption.IGNORE_CASE)
        return rawText
            .replace(ipv4Pattern, "[IP-LOCAL-REDACTADA]")
            .replace(pinPattern, "PIN: [REDACTADO]")
    }
}

package com.example.core.packs

import com.example.ui.i18n.AppLanguage

object PackSecurityLimits {
    const val SCHEMA_VERSION_1_0 = "1.0"
    const val MAX_MANIFEST_BYTES: Long = 256L * 1024L
    const val MAX_SINGLE_IMAGE_BYTES: Long = 10L * 1024L * 1024L
    const val MAX_IMAGES_PER_PACK: Int = 100
    const val MAX_TOTAL_DATA_PACK_BYTES: Long = 50L * 1024L * 1024L
    const val MAX_RELATIVE_PATH_DEPTH: Int = 4

    val ALLOWED_GITHUB_ENTRY_HOSTS = setOf("github.com")
    val ALLOWED_GITHUB_API_HOSTS = setOf("api.github.com")
    val ALLOWED_GITHUB_RAW_HOSTS = setOf("raw.githubusercontent.com")

    val FORBIDDEN_DECLARATIVE_EXTENSIONS = setOf(
        "sh", "bat", "cmd", "ps1", "py", "js", "ts", "jar", "so", "dex", "elf", "exe", "html", "htm", "svg"
    )
}

enum class PackType(val wireName: String, val labelEs: String, val labelEn: String) {
    THEME("theme", "Tema visual", "Visual theme"),
    TWEAK("tweak", "Ajuste declarativo", "Declarative tweak"),
    DEBLOAT_PROFILE("debloat-profile", "Perfil de limpieza", "Cleanup profile"),
    PROFILE("profile", "Perfil de compatibilidad", "Compatibility profile"),
    ADDON("addon", "Complemento", "Add-on");

    fun localizedLabel(lang: AppLanguage): String = if (lang == AppLanguage.ES) labelEs else labelEn
}

enum class PackTarget(
    val wireName: String,
    val labelEs: String,
    val labelEn: String,
    val affectsTv: Boolean
) {
    MOBILE_UI("mobile-ui", "App móvil", "Mobile app", affectsTv = false),
    TV_LAUNCHER("tv-launcher", "Launcher TV", "TV Launcher", affectsTv = true),
    TV_PROFILE("tv-profile", "Perfil TV", "TV Profile", affectsTv = true),
    TV_ADDON("tv-addon", "Addon TV", "TV Add-on", affectsTv = true);

    fun localizedLabel(lang: AppLanguage): String = if (lang == AppLanguage.ES) labelEs else labelEn
}

enum class PackTrustTier(
    val titleEs: String,
    val titleEn: String,
    val badgeSummaryEs: String,
    val requiresExtraUnverifiedConsent: Boolean
) {
    OFFICIAL(
        titleEs = "Oficial",
        titleEn = "Official",
        badgeSummaryEs = "Incluido y verificado por clave interna de TVeoClean.",
        requiresExtraUnverifiedConsent = false
    ),
    VERIFIED(
        titleEs = "Verificado",
        titleEn = "Verified",
        badgeSummaryEs = "Curado y firmado criptográficamente en índice comunitario confiable.",
        requiresExtraUnverifiedConsent = false
    ),
    UNVERIFIED_LINK(
        titleEs = "Sin verificar",
        titleEn = "Unverified",
        badgeSummaryEs = "Fijado a commit SHA para reproducibilidad, pero sin firma de autor verificada.",
        requiresExtraUnverifiedConsent = true
    );

    fun localizedTitle(lang: AppLanguage): String = if (lang == AppLanguage.ES) titleEs else titleEn
}

data class TvLauncherThemePreviewSpec(
    val backgroundHex: String,
    val surfaceHex: String,
    val accentHex: String,
    val glowHex: String,
    val columns: Int,
    val dockApps: List<String>,
    val fillDock: Boolean,
    val ambientMotionEnabled: Boolean
)

data class DebloatRecommendationItem(
    val packageName: String,
    val reasonEs: String,
    val reasonEn: String,
    val riskLevelEs: String
) {
    fun localizedReason(lang: AppLanguage): String = if (lang == AppLanguage.ES) reasonEs else reasonEn
}

data class PackDiffEntry(
    val propertyLabelEs: String,
    val propertyLabelEn: String,
    val currentValue: String,
    val proposedValue: String
) {
    fun localizedLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.ES) propertyLabelEs else propertyLabelEn
}

data class TveoPackSummary(
    val id: String,
    val schema: String = PackSecurityLimits.SCHEMA_VERSION_1_0,
    val nameEs: String,
    val nameEn: String = nameEs,
    val version: String,
    val descriptionEs: String,
    val descriptionEn: String = descriptionEs,
    val author: String,
    val license: String,
    val creditsEs: String,
    val type: PackType,
    val target: PackTarget,
    val trustTier: PackTrustTier,
    val sourceRepo: String,
    val pinnedCommitSha: String,
    val sha256Manifest: String,
    val compatibilityLabelEs: String,
    val isLegacyConverted: Boolean = false,
    val omittedLegacyKeys: List<String> = emptyList(),
    val themePreview: TvLauncherThemePreviewSpec? = null,
    val mobilePrimaryDarkHex: String? = null,
    val mobilePrimaryLightHex: String? = null,
    val mobileSecondaryDarkHex: String? = null,
    val mobileSecondaryLightHex: String? = null,
    val debloatItems: List<DebloatRecommendationItem> = emptyList(),
    val diffsFromDefault: List<PackDiffEntry> = emptyList()
) {
    val name: String get() = nameEs
    fun localizedName(lang: AppLanguage): String = if (lang == AppLanguage.ES) nameEs else nameEn
    fun localizedDescription(lang: AppLanguage): String =
        if (lang == AppLanguage.ES) descriptionEs else descriptionEn
}

object StrictGitHubUrlParser {

    data class ParsedGitHubRef(
        val originalUrl: String,
        val owner: String,
        val repo: String,
        val refOrDefault: String,
        val relativePath: String
    )

    sealed interface ParseOutcome {
        data class Valid(val parsed: ParsedGitHubRef) : ParseOutcome
        data class Invalid(val messageEs: String, val messageEn: String = messageEs) : ParseOutcome {
            fun localizedMessage(lang: AppLanguage): String =
                if (lang == AppLanguage.ES) messageEs else messageEn
        }
    }

    private val safeSlug = Regex("""^[A-Za-z0-9_.\-]+$""")

    fun parse(rawInput: String): ParseOutcome {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParseOutcome.Invalid(
                messageEs = "Pega una URL HTTPS de github.com hacia un repositorio o carpeta de paquete.",
                messageEn = "Paste an HTTPS github.com URL pointing to a repository or package folder."
            )
        }
        val uri = runCatching { java.net.URI(trimmed) }.getOrNull()
            ?: return ParseOutcome.Invalid(
                messageEs = "El enlace tiene un formato malformado. No se realizó ninguna conexión.",
                messageEn = "Malformed URL syntax. No network connection was attempted."
            )

        if (uri.scheme?.lowercase() != "https") {
            return ParseOutcome.Invalid(
                messageEs = "Solo se admiten enlaces seguros HTTPS. Se rechazó '${uri.scheme ?: "vacío"}'.",
                messageEn = "Only secure HTTPS links are permitted. Scheme '${uri.scheme ?: "empty"}' was rejected."
            )
        }
        if (uri.userInfo != null) {
            return ParseOutcome.Invalid(
                messageEs = "Se rechazó el enlace porque contiene credenciales embebidas en la URL.",
                messageEn = "Rejected URL containing embedded user credentials."
            )
        }
        if (uri.port != -1 && uri.port != 443) {
            return ParseOutcome.Invalid(
                messageEs = "Puerto no estándar (${uri.port}) rechazado. Usa HTTPS estándar en github.com.",
                messageEn = "Non-standard port (${uri.port}) rejected. Only port 443 on github.com is allowed."
            )
        }
        val host = uri.host?.lowercase() ?: ""
        if (host !in PackSecurityLimits.ALLOWED_GITHUB_ENTRY_HOSTS) {
            return ParseOutcome.Invalid(
                messageEs = "Dominio no permitido ('$host'). Solo se aceptan enlaces de github.com.",
                messageEn = "Disallowed host ('$host'). Only direct github.com links are accepted."
            )
        }

        val segments = (uri.path ?: "")
            .split("/")
            .filter { it.isNotBlank() }

        if (segments.size < 2) {
            return ParseOutcome.Invalid(
                messageEs = "El enlace debe incluir propietario y repositorio (ej. https://github.com/autor/repo).",
                messageEn = "URL must include owner and repository (e.g. https://github.com/owner/repo)."
            )
        }

        val owner = segments[0]
        val repo = segments[1].removeSuffix(".git")
        if (!safeSlug.matches(owner) || !safeSlug.matches(repo)) {
            return ParseOutcome.Invalid(
                messageEs = "El propietario o repositorio contiene caracteres no válidos.",
                messageEn = "Owner or repository contains disallowed characters."
            )
        }

        if (segments.size == 2) {
            return ParseOutcome.Valid(
                ParsedGitHubRef(
                    originalUrl = trimmed,
                    owner = owner,
                    repo = repo,
                    refOrDefault = "HEAD",
                    relativePath = ""
                )
            )
        }

        val mode = segments[2]
        if (mode != "tree" && mode != "blob") {
            return ParseOutcome.Invalid(
                messageEs = "Usa un enlace a la raíz del repositorio o una ruta /tree/<rama-o-sha>/<carpeta>.",
                messageEn = "Use a repository root link or a /tree/<branch-or-sha>/<folder> path."
            )
        }
        if (segments.size < 4) {
            return ParseOutcome.Invalid(
                messageEs = "Falta la rama, tag o commit SHA tras '/$mode/'.",
                messageEn = "Missing branch, tag, or commit SHA after '/$mode/'."
            )
        }

        val ref = segments[3]
        val subPathSegments = segments.drop(4)
        if (subPathSegments.any { it == ".." || it == "." || it.startsWith("/") }) {
            return ParseOutcome.Invalid(
                messageEs = "Se detectó recorrido de directorios ('..') en la ruta. Importación bloqueada.",
                messageEn = "Path traversal ('..') detected in URL path. Import blocked."
            )
        }
        if (subPathSegments.size > PackSecurityLimits.MAX_RELATIVE_PATH_DEPTH) {
            return ParseOutcome.Invalid(
                messageEs = "La ruta supera la profundidad máxima permitida (${PackSecurityLimits.MAX_RELATIVE_PATH_DEPTH} niveles).",
                messageEn = "Path exceeds maximum allowed depth (${PackSecurityLimits.MAX_RELATIVE_PATH_DEPTH} levels)."
            )
        }

        val lastSegmentExt = subPathSegments.lastOrNull()?.substringAfterLast('.', "")?.lowercase() ?: ""
        if (lastSegmentExt in PackSecurityLimits.FORBIDDEN_DECLARATIVE_EXTENSIONS) {
            return ParseOutcome.Invalid(
                messageEs = "Archivo ejecutable o activo no permitido (.$lastSegmentExt).",
                messageEn = "Executable or active file extension (.$lastSegmentExt) is forbidden."
            )
        }

        return ParseOutcome.Valid(
            ParsedGitHubRef(
                originalUrl = trimmed,
                owner = owner,
                repo = repo,
                refOrDefault = ref,
                relativePath = subPathSegments.joinToString("/")
            )
        )
    }
}

package com.example.core.packs

import com.example.core.restore.IntegrityHashUtil
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

sealed interface GitHubImportResult {
    data class Imported(
        val pack: TveoPackSummary,
        val storagePath: String
    ) : GitHubImportResult

    data class Rejected(
        val messageEs: String,
        val messageEn: String
    ) : GitHubImportResult
}

/**
 * Production GitHub Pack Importer (Sections 9 & 10 of Master Specification).
 *
 * Security pipeline:
 * 1. Parses HTTPS `github.com` URL strictly via `StrictGitHubUrlParser`.
 * 2. Resolves mutable branch/tag (`main`, `HEAD`) via `api.github.com` to an immutable 40-char Commit SHA.
 * 3. Disables cross-domain redirects (`followRedirects = false`) and enforces allowed hosts
 *    (`api.github.com` and `raw.githubusercontent.com` ONLY).
 * 4. Rejects executable scripts/binaries (`.sh`, `.py`, `.js`, `.so`, `.dex`, `.svg`, `.html`) and oversized files.
 * 5. Parses `tveo.json` (verifying declared SHA-256 file hashes!) OR converts legacy Clean Your TV `config.json`
 *    locally without modifying the remote repository.
 * 6. Marks imported link honestly as `PackTrustTier.UNVERIFIED_LINK` and persists into isolated app storage.
 */
class GitHubPackImporter(
    private val baseStorageDir: File,
    private val httpClient: OkHttpClient = defaultPinnedClient(),
    private val apiBaseUrl: String = "https://api.github.com",
    private val rawBaseUrl: String = "https://raw.githubusercontent.com",
    private val allowLoopbackForTest: Boolean = false,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private val COMMIT_SHA_REGEX = Regex("""^[0-9a-fA-F]{40}$""")
        private val HEX_COLOR_REGEX = Regex("""^#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$""")

        private val KNOWN_LEGACY_KEYS = setOf(
            "name", "author", "version", "description", "license",
            "background", "surface", "accent", "glow",
            "columns", "dock", "dockApps", "fillDock", "ambientMotion", "motion"
        )

        fun defaultPinnedClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
        }
    }

    suspend fun importFromUrl(rawUrl: String): GitHubImportResult = withContext(ioDispatcher) {
        val parseOutcome = StrictGitHubUrlParser.parse(rawUrl)
        if (parseOutcome is StrictGitHubUrlParser.ParseOutcome.Invalid) {
            return@withContext GitHubImportResult.Rejected(
                messageEs = parseOutcome.messageEs,
                messageEn = parseOutcome.messageEn
            )
        }
        val refInfo = (parseOutcome as StrictGitHubUrlParser.ParseOutcome.Valid).parsed

        // Step 2: Resolve branch/tag/HEAD to an immutable 40-char Commit SHA
        val commitSha = if (COMMIT_SHA_REGEX.matches(refInfo.refOrDefault)) {
            refInfo.refOrDefault.lowercase()
        } else {
            val commitUrl = "$apiBaseUrl/repos/${refInfo.owner}/${refInfo.repo}/commits/${refInfo.refOrDefault}"
            val commitJsonBytes = fetchBoundedBytes(
                url = commitUrl,
                expectedAllowedHosts = PackSecurityLimits.ALLOWED_GITHUB_API_HOSTS,
                maxBytes = PackSecurityLimits.MAX_MANIFEST_BYTES,
                acceptHeader = "application/vnd.github+json"
            ).getOrElse { err ->
                return@withContext mapHttpError(err)
            }
            val sha = runCatching {
                JSONObject(commitJsonBytes.toString(Charsets.UTF_8)).optString("sha", "").lowercase()
            }.getOrDefault("")
            if (!COMMIT_SHA_REGEX.matches(sha)) {
                return@withContext GitHubImportResult.Rejected(
                    messageEs = "No se pudo fijar el repositorio a un commit SHA de 40 caracteres inmutable. No se descargó nada.",
                    messageEn = "Could not pin the repository to an immutable 40-character commit SHA. Nothing was downloaded."
                )
            }
            sha
        }

        // Step 3: List directory contents pinned to ?ref={commitSha}
        val normalizedDirPath = if (refInfo.relativePath.endsWith(".json")) {
            refInfo.relativePath.substringBeforeLast('/', "")
        } else {
            refInfo.relativePath
        }
        val contentsPathPart = if (normalizedDirPath.isEmpty()) "" else "/$normalizedDirPath"
        val contentsUrl = "$apiBaseUrl/repos/${refInfo.owner}/${refInfo.repo}/contents$contentsPathPart?ref=$commitSha"

        val dirJsonBytes = fetchBoundedBytes(
            url = contentsUrl,
            expectedAllowedHosts = PackSecurityLimits.ALLOWED_GITHUB_API_HOSTS,
            maxBytes = PackSecurityLimits.MAX_MANIFEST_BYTES,
            acceptHeader = "application/vnd.github+json"
        ).getOrElse { err ->
            return@withContext mapHttpError(err)
        }

        val dirEntries = parseDirectoryEntries(dirJsonBytes.toString(Charsets.UTF_8)).getOrElse {
            return@withContext GitHubImportResult.Rejected(
                messageEs = "La ruta seleccionada no contiene una carpeta de paquete válida en GitHub.",
                messageEn = "The selected path does not contain a valid package directory on GitHub."
            )
        }

        // Security scan: block any executable or forbidden script present in the pack directory
        for (entry in dirEntries) {
            if (entry.type == "symlink") {
                return@withContext GitHubImportResult.Rejected(
                    messageEs = "Se detectó un enlace simbólico ('${entry.name}') en el paquete. Importación cancelada por seguridad.",
                    messageEn = "Symbolic link ('${entry.name}') detected inside package. Import aborted for security."
                )
            }
            val ext = entry.name.substringAfterLast('.', "").lowercase()
            if (ext in PackSecurityLimits.FORBIDDEN_DECLARATIVE_EXTENSIONS) {
                return@withContext GitHubImportResult.Rejected(
                    messageEs = "Paquete rechazado: contiene archivo ejecutable o activo no permitido ('${entry.name}').",
                    messageEn = "Package rejected: contains forbidden executable or active file ('${entry.name}')."
                )
            }
        }

        val tveoEntry = dirEntries.find { it.name == "tveo.json" && it.type == "file" }
        val legacyEntry = dirEntries.find { it.name == "config.json" && it.type == "file" }

        if (tveoEntry == null && legacyEntry == null) {
            return@withContext GitHubImportResult.Rejected(
                messageEs = "No se encontró 'tveo.json' ni un tema heredado 'config.json' en '${normalizedDirPath.ifEmpty { "/" }}'.",
                messageEn = "Neither 'tveo.json' nor legacy 'config.json' was found at '${normalizedDirPath.ifEmpty { "/" }}'."
            )
        }

        return@withContext if (tveoEntry != null) {
            importFormalTveoManifest(refInfo, commitSha, normalizedDirPath, tveoEntry)
        } else {
            importLegacyCleanYourTvTheme(refInfo, commitSha, normalizedDirPath, legacyEntry!!, dirEntries)
        }
    }

    private fun importFormalTveoManifest(
        refInfo: StrictGitHubUrlParser.ParsedGitHubRef,
        commitSha: String,
        dirPath: String,
        manifestEntry: DirEntry
    ): GitHubImportResult {
        if (manifestEntry.size > PackSecurityLimits.MAX_MANIFEST_BYTES) {
            return GitHubImportResult.Rejected(
                messageEs = "tveo.json supera el límite máximo permitido de 256 KB.",
                messageEn = "tveo.json exceeds the 256 KB maximum allowed size."
            )
        }
        val rawManifestUrl = buildPinnedRawUrl(refInfo.owner, refInfo.repo, commitSha, manifestEntry.path)
        val manifestBytes = fetchBoundedBytes(
            url = rawManifestUrl,
            expectedAllowedHosts = PackSecurityLimits.ALLOWED_GITHUB_RAW_HOSTS,
            maxBytes = PackSecurityLimits.MAX_MANIFEST_BYTES
        ).getOrElse { return mapHttpError(it) }

        val manifestSha256 = IntegrityHashUtil.sha256BytesHex(manifestBytes)
        val json = runCatching { JSONObject(manifestBytes.toString(Charsets.UTF_8)) }.getOrElse {
            return GitHubImportResult.Rejected(
                messageEs = "El archivo tveo.json está malformado y no es JSON válido.",
                messageEn = "tveo.json is malformed and not valid JSON."
            )
        }

        // Security check: reject any shell/script field in manifest
        if (json.has("shell") || json.has("commands") || json.has("script") || json.has("exec")) {
            return GitHubImportResult.Rejected(
                messageEs = "Manifiesto rechazado: intenta declarar comandos shell o scripts ejecutables.",
                messageEn = "Manifest rejected: attempts to declare shell commands or executable scripts."
            )
        }

        val schema = json.optString("schema", "")
        val rawId = json.optString("id", "").trim()
        val name = json.optString("name", "").trim()
        val version = json.optString("version", "").trim()
        val typeWire = json.optString("type", "").trim()
        val targetWire = json.optString("target", "").trim()

        if (schema != PackSecurityLimits.SCHEMA_VERSION_1_0 || rawId.isEmpty() || name.isEmpty() || version.isEmpty()) {
            return GitHubImportResult.Rejected(
                messageEs = "tveo.json incompleto o con versión de esquema no soportada (se requiere schema '1.0', id, name y version).",
                messageEn = "Incomplete tveo.json or unsupported schema version (requires schema '1.0', id, name, and version)."
            )
        }

        val packType = PackType.entries.find { it.wireName == typeWire }
            ?: return GitHubImportResult.Rejected(
                messageEs = "Tipo de paquete desconocido ('$typeWire') en tveo.json.",
                messageEn = "Unknown package type ('$typeWire') in tveo.json."
            )

        val packTarget = PackTarget.entries.find { it.wireName == targetWire }
            ?: return GitHubImportResult.Rejected(
                messageEs = "Destino de paquete desconocido ('$targetWire') en tveo.json.",
                messageEn = "Unknown package target ('$targetWire') in tveo.json."
            )

        if (packType == PackType.ADDON) {
            return GitHubImportResult.Rejected(
                messageEs = "Los complementos ejecutables APK requieren firma en catálogo verificado; no se instalan desde enlaces sin verificar.",
                messageEn = "Executable APK add-ons require verified catalog certificate pinning; unverified APK links are blocked."
            )
        }

        val safePackId = sanitizeStorageSlug(rawId)
        val packDir = File(baseStorageDir, "packs/$safePackId/$commitSha").apply { mkdirs() }

        // Validate and download declared files checking exact SHA-256 integrity
        val filesArray = json.optJSONArray("files") ?: JSONArray()
        var totalBytes = manifestBytes.size.toLong()
        var imageCount = 0

        for (i in 0 until filesArray.length()) {
            val fObj = filesArray.optJSONObject(i) ?: continue
            val relPath = fObj.optString("path", "").trim()
            val expectedSha = fObj.optString("sha256", "").trim().lowercase()
            if (!isSafeRelativeAssetPath(relPath)) {
                packDir.deleteRecursively()
                return GitHubImportResult.Rejected(
                    messageEs = "Ruta relativa insegura o prohibida en files[]: '$relPath'.",
                    messageEn = "Unsafe or forbidden relative path in files[]: '$relPath'."
                )
            }
            val ext = relPath.substringAfterLast('.', "").lowercase()
            val isImage = ext in setOf("jpg", "jpeg", "png", "webp")
            if (isImage) {
                imageCount++
                if (imageCount > PackSecurityLimits.MAX_IMAGES_PER_PACK) {
                    packDir.deleteRecursively()
                    return GitHubImportResult.Rejected(
                        messageEs = "El paquete supera el máximo de ${PackSecurityLimits.MAX_IMAGES_PER_PACK} imágenes permitidas.",
                        messageEn = "Package exceeds the maximum of ${PackSecurityLimits.MAX_IMAGES_PER_PACK} allowed images."
                    )
                }
            }
            val fullRepoPath = if (dirPath.isEmpty()) relPath else "$dirPath/$relPath"
            val assetBytes = fetchBoundedBytes(
                url = buildPinnedRawUrl(refInfo.owner, refInfo.repo, commitSha, fullRepoPath),
                expectedAllowedHosts = PackSecurityLimits.ALLOWED_GITHUB_RAW_HOSTS,
                maxBytes = if (isImage) PackSecurityLimits.MAX_SINGLE_IMAGE_BYTES else PackSecurityLimits.MAX_MANIFEST_BYTES
            ).getOrElse {
                packDir.deleteRecursively()
                return mapHttpError(it)
            }

            totalBytes += assetBytes.size
            if (totalBytes > PackSecurityLimits.MAX_TOTAL_DATA_PACK_BYTES) {
                packDir.deleteRecursively()
                return GitHubImportResult.Rejected(
                    messageEs = "El tamaño total del paquete supera el límite seguro de 50 MB.",
                    messageEn = "Total package size exceeds the 50 MB safety cap."
                )
            }

            val actualSha = IntegrityHashUtil.sha256BytesHex(assetBytes)
            if (expectedSha.isEmpty() || actualSha != expectedSha) {
                packDir.deleteRecursively()
                return GitHubImportResult.Rejected(
                    messageEs = "El archivo '$relPath' no coincide con su SHA‑256 declarado. Se canceló la importación.",
                    messageEn = "File '$relPath' did not match its declared SHA‑256 hash. Import cancelled."
                )
            }

            val destFile = File(packDir, relPath)
            if (!destFile.canonicalPath.startsWith(packDir.canonicalPath)) {
                packDir.deleteRecursively()
                return GitHubImportResult.Rejected(
                    messageEs = "Intento de escape de directorio detectado ('zip-slip'). Importación bloqueada.",
                    messageEn = "Directory traversal attempt detected. Import blocked."
                )
            }
            destFile.parentFile?.mkdirs()
            destFile.writeBytes(assetBytes)
        }

        File(packDir, "tveo.json").writeBytes(manifestBytes)

        val themeObj = json.optJSONObject("theme")
        val preview = parseThemeSpec(themeObj)
        val debloats = parseDebloatRecommendations(json.optJSONArray("recommendations"))
        val diffs = buildDiffEntries(packTarget, preview, debloats)

        val summary = TveoPackSummary(
            id = rawId,
            schema = schema,
            nameEs = name,
            nameEn = name,
            version = version,
            descriptionEs = json.optString("description", "Paquete importado desde GitHub fijado por SHA."),
            descriptionEn = json.optString("description", "GitHub imported pack pinned by commit SHA."),
            author = json.optString("author", refInfo.owner),
            license = json.optString("license", "No declarada"),
            creditsEs = json.optString("credits", "Repositorio ${refInfo.owner}/${refInfo.repo}"),
            type = packType,
            target = packTarget,
            trustTier = PackTrustTier.UNVERIFIED_LINK,
            sourceRepo = "https://github.com/${refInfo.owner}/${refInfo.repo}",
            pinnedCommitSha = commitSha,
            sha256Manifest = manifestSha256,
            compatibilityLabelEs = "Importado de GitHub · SHA ${commitSha.take(8)}",
            isLegacyConverted = false,
            omittedLegacyKeys = emptyList(),
            themePreview = preview,
            mobilePrimaryDarkHex = themeObj?.optString("mobilePrimaryDark")?.takeIf { HEX_COLOR_REGEX.matches(it) },
            mobilePrimaryLightHex = themeObj?.optString("mobilePrimaryLight")?.takeIf { HEX_COLOR_REGEX.matches(it) },
            mobileSecondaryDarkHex = themeObj?.optString("mobileSecondaryDark")?.takeIf { HEX_COLOR_REGEX.matches(it) },
            mobileSecondaryLightHex = themeObj?.optString("mobileSecondaryLight")?.takeIf { HEX_COLOR_REGEX.matches(it) },
            debloatItems = debloats,
            diffsFromDefault = diffs
        )
        return GitHubImportResult.Imported(summary, packDir.absolutePath)
    }

    private fun importLegacyCleanYourTvTheme(
        refInfo: StrictGitHubUrlParser.ParsedGitHubRef,
        commitSha: String,
        dirPath: String,
        configEntry: DirEntry,
        siblings: List<DirEntry>
    ): GitHubImportResult {
        if (configEntry.size > PackSecurityLimits.MAX_MANIFEST_BYTES) {
            return GitHubImportResult.Rejected(
                messageEs = "config.json supera el límite máximo permitido de 256 KB.",
                messageEn = "config.json exceeds the 256 KB maximum allowed size."
            )
        }
        val configBytes = fetchBoundedBytes(
            url = buildPinnedRawUrl(refInfo.owner, refInfo.repo, commitSha, configEntry.path),
            expectedAllowedHosts = PackSecurityLimits.ALLOWED_GITHUB_RAW_HOSTS,
            maxBytes = PackSecurityLimits.MAX_MANIFEST_BYTES
        ).getOrElse { return mapHttpError(it) }

        val configSha = IntegrityHashUtil.sha256BytesHex(configBytes)
        val json = runCatching { JSONObject(configBytes.toString(Charsets.UTF_8)) }.getOrElse {
            return GitHubImportResult.Rejected(
                messageEs = "El archivo heredado config.json no es JSON válido.",
                messageEn = "Legacy config.json is not valid JSON."
            )
        }

        val omittedKeys = mutableListOf<String>()
        val iter = json.keys()
        while (iter.hasNext()) {
            val k = iter.next()
            if (k !in KNOWN_LEGACY_KEYS) {
                omittedKeys.add(k)
            }
        }

        val folderSlug = dirPath.substringAfterLast('/').ifBlank { refInfo.repo }
        val packId = "legacy.${sanitizeStorageSlug(refInfo.owner)}.${sanitizeStorageSlug(folderSlug)}"
        val packDir = File(baseStorageDir, "packs/$packId/$commitSha").apply { mkdirs() }
        File(packDir, "config.json").writeBytes(configBytes)

        // Optionally fetch wallpaper.jpg if present and within 10 MB limit
        val wallpaperEntry = siblings.find {
            it.type == "file" && it.name.lowercase() in setOf("wallpaper.jpg", "wallpaper.png", "wallpaper.webp")
        }
        if (wallpaperEntry != null && wallpaperEntry.size <= PackSecurityLimits.MAX_SINGLE_IMAGE_BYTES) {
            fetchBoundedBytes(
                url = buildPinnedRawUrl(refInfo.owner, refInfo.repo, commitSha, wallpaperEntry.path),
                expectedAllowedHosts = PackSecurityLimits.ALLOWED_GITHUB_RAW_HOSTS,
                maxBytes = PackSecurityLimits.MAX_SINGLE_IMAGE_BYTES
            ).onSuccess { imgBytes ->
                File(packDir, wallpaperEntry.name).writeBytes(imgBytes)
            }
        }

        val bg = json.optString("background", "#0F172A").let { if (HEX_COLOR_REGEX.matches(it)) it else "#0F172A" }
        val surface = json.optString("surface", "#1E293B").let { if (HEX_COLOR_REGEX.matches(it)) it else "#1E293B" }
        val accent = json.optString("accent", "#55E6C1").let { if (HEX_COLOR_REGEX.matches(it)) it else "#55E6C1" }
        val glow = json.optString("glow", "#8585FF").let { if (HEX_COLOR_REGEX.matches(it)) it else "#8585FF" }
        val cols = json.optInt("columns", 4).coerceIn(2, 6)
        val fillDock = json.optBoolean("fillDock", true)
        val motion = json.optBoolean("ambientMotion", json.optBoolean("motion", true))

        val dockList = mutableListOf<String>()
        (json.optJSONArray("dockApps") ?: json.optJSONArray("dock"))?.let { arr ->
            for (i in 0 until arr.length().coerceAtMost(8)) {
                val appLabel = arr.optString(i).trim()
                if (appLabel.isNotEmpty()) dockList.add(appLabel.take(24))
            }
        }
        if (dockList.isEmpty()) {
            dockList.addAll(listOf("TV", "Películas", "Música", "Ajustes"))
        }

        val preview = TvLauncherThemePreviewSpec(
            backgroundHex = bg,
            surfaceHex = surface,
            accentHex = accent,
            glowHex = glow,
            columns = cols,
            dockApps = dockList,
            fillDock = fillDock,
            ambientMotionEnabled = motion
        )

        val displayName = json.optString("name", folderSlug.replaceFirstChar { it.uppercase() })
        val summary = TveoPackSummary(
            id = packId,
            schema = PackSecurityLimits.SCHEMA_VERSION_1_0,
            nameEs = displayName,
            nameEn = displayName,
            version = json.optString("version", "1.0.0"),
            descriptionEs = json.optString(
                "description",
                "Tema heredado config.json convertido localmente a esquema declarativo seguro."
            ),
            descriptionEn = json.optString(
                "description",
                "Legacy config.json theme converted locally to safe declarative schema."
            ),
            author = json.optString("author", refInfo.owner),
            license = json.optString("license", "MIT / Comunidad"),
            creditsEs = "Importado sin modificar el repositorio original (${refInfo.owner}/${refInfo.repo})",
            type = PackType.THEME,
            target = PackTarget.TV_LAUNCHER,
            trustTier = PackTrustTier.UNVERIFIED_LINK,
            sourceRepo = "https://github.com/${refInfo.owner}/${refInfo.repo}",
            pinnedCommitSha = commitSha,
            sha256Manifest = configSha,
            compatibilityLabelEs = "Adaptador heredado config.json · SHA ${commitSha.take(8)}",
            isLegacyConverted = true,
            omittedLegacyKeys = omittedKeys,
            themePreview = preview,
            diffsFromDefault = buildDiffEntries(PackTarget.TV_LAUNCHER, preview, emptyList())
        )

        return GitHubImportResult.Imported(summary, packDir.absolutePath)
    }

    private fun parseThemeSpec(themeObj: JSONObject?): TvLauncherThemePreviewSpec? {
        if (themeObj == null) return null
        val bg = themeObj.optString("backgroundHex", "#0B1020").let { if (HEX_COLOR_REGEX.matches(it)) it else "#0B1020" }
        val sf = themeObj.optString("surfaceHex", "#121A2B").let { if (HEX_COLOR_REGEX.matches(it)) it else "#121A2B" }
        val ac = themeObj.optString("accentHex", "#55E6C1").let { if (HEX_COLOR_REGEX.matches(it)) it else "#55E6C1" }
        val gl = themeObj.optString("glowHex", "#8585FF").let { if (HEX_COLOR_REGEX.matches(it)) it else "#8585FF" }
        val cols = themeObj.optInt("columns", 4).coerceIn(2, 6)
        val dock = mutableListOf<String>()
        themeObj.optJSONArray("dockApps")?.let { arr ->
            for (i in 0 until arr.length().coerceAtMost(8)) {
                val v = arr.optString(i).trim()
                if (v.isNotEmpty()) dock.add(v.take(24))
            }
        }
        if (dock.isEmpty()) dock.addAll(listOf("Inicio", "Cine", "Apps", "Ajustes"))
        return TvLauncherThemePreviewSpec(
            backgroundHex = bg,
            surfaceHex = sf,
            accentHex = ac,
            glowHex = gl,
            columns = cols,
            dockApps = dock,
            fillDock = themeObj.optBoolean("fillDock", true),
            ambientMotionEnabled = themeObj.optBoolean("ambientMotionEnabled", true)
        )
    }

    private fun parseDebloatRecommendations(arr: JSONArray?): List<DebloatRecommendationItem> {
        if (arr == null) return emptyList()
        val list = mutableListOf<DebloatRecommendationItem>()
        for (i in 0 until arr.length().coerceAtMost(80)) {
            val obj = arr.optJSONObject(i) ?: continue
            val pkg = obj.optString("packageName", "").trim()
            if (pkg.isEmpty()) continue
            list.add(
                DebloatRecommendationItem(
                    packageName = pkg,
                    reasonEs = obj.optString("reason", "Declarado en perfil comunitario (sujeto a guardia protegida)."),
                    reasonEn = obj.optString("reason", "Declared in community profile (subject to protected guard)."),
                    riskLevelEs = obj.optString("risk", "Revisión manual")
                )
            )
        }
        return list
    }

    private fun buildDiffEntries(
        target: PackTarget,
        preview: TvLauncherThemePreviewSpec?,
        debloats: List<DebloatRecommendationItem>
    ): List<PackDiffEntry> {
        val diffs = mutableListOf<PackDiffEntry>()
        if (preview != null) {
            diffs.add(PackDiffEntry("Acento principal", "Primary accent", "#55E6C1", preview.accentHex))
            diffs.add(PackDiffEntry("Columnas de cuadrícula", "Grid columns", "4", preview.columns.toString()))
        }
        if (debloats.isNotEmpty()) {
            diffs.add(
                PackDiffEntry(
                    "Paquetes sugeridos (modo revisión)",
                    "Suggested packages (review mode)",
                    "0",
                    "${debloats.size} declarados"
                )
            )
        }
        if (diffs.isEmpty()) {
            diffs.add(PackDiffEntry("Destino", "Target", "Actual", target.wireName))
        }
        return diffs
    }

    private data class DirEntry(val name: String, val path: String, val type: String, val size: Long)

    private fun parseDirectoryEntries(rawJson: String): Result<List<DirEntry>> = runCatching {
        val trimmed = rawJson.trim()
        if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        DirEntry(
                            name = obj.getString("name"),
                            path = obj.getString("path"),
                            type = obj.optString("type", "file"),
                            size = obj.optLong("size", 0L)
                        )
                    )
                }
            }
        } else {
            val obj = JSONObject(trimmed)
            listOf(
                DirEntry(
                    name = obj.getString("name"),
                    path = obj.getString("path"),
                    type = obj.optString("type", "file"),
                    size = obj.optLong("size", 0L)
                )
            )
        }
    }

    private fun buildPinnedRawUrl(owner: String, repo: String, commitSha: String, filePath: String): String {
        val cleanPath = filePath.trimStart('/')
        return "$rawBaseUrl/$owner/$repo/$commitSha/$cleanPath"
    }

    private fun fetchBoundedBytes(
        url: String,
        expectedAllowedHosts: Set<String>,
        maxBytes: Long,
        acceptHeader: String = "*/*"
    ): Result<ByteArray> = runCatching {
        val parsed = java.net.URI(url)
        val host = parsed.host?.lowercase() ?: throw SecurityException("URL sin host válido.")
        val isTestLocalhost = allowLoopbackForTest && (host == "127.0.0.1" || host == "localhost")
        if (!isTestLocalhost) {
            if (parsed.scheme?.lowercase() != "https") {
                throw SecurityException("Se bloqueó una conexión no HTTPS hacia '$host'.")
            }
            if (host !in expectedAllowedHosts) {
                throw SecurityException("Dominio bloqueado ('$host') fuera de la lista blanca oficial de GitHub.")
            }
        }

        val req = Request.Builder()
            .url(url)
            .header("Accept", acceptHeader)
            .header("User-Agent", "TVeoClean-Android/1.0")
            .get()
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (resp.code in 300..399) {
                val loc = resp.header("Location").orEmpty()
                val redirectHost = runCatching { java.net.URI(loc).host?.lowercase() }.getOrNull()
                if (redirectHost != null && redirectHost !in expectedAllowedHosts && !(allowLoopbackForTest && redirectHost == "127.0.0.1")) {
                    throw SecurityException("Redirección bloqueada hacia dominio no autorizado ('$redirectHost').")
                }
                throw IOException("HTTP_REDIRECT_${resp.code}")
            }
            if (resp.code == 403 || resp.code == 429) {
                val remaining = resp.header("X-RateLimit-Remaining")
                if (resp.code == 429 || remaining == "0") {
                    throw IOException("GITHUB_RATE_LIMIT")
                }
            }
            if (resp.code == 404) {
                throw IOException("GITHUB_NOT_FOUND")
            }
            if (!resp.isSuccessful) {
                throw IOException("GITHUB_HTTP_${resp.code}")
            }

            val body = resp.body ?: throw IOException("Respuesta vacía desde GitHub.")
            val declaredLength = body.contentLength()
            if (declaredLength > maxBytes) {
                throw SecurityException("El archivo supera el tamaño máximo permitido ($declaredLength > $maxBytes bytes).")
            }

            val input = body.byteStream()
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > maxBytes) {
                    throw SecurityException("Descarga abortada: se superó el límite de $maxBytes bytes.")
                }
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    }

    private fun isSafeRelativeAssetPath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.contains("\\")) return false
        val segments = path.split("/")
        if (segments.size > PackSecurityLimits.MAX_RELATIVE_PATH_DEPTH) return false
        if (segments.any { it.isBlank() || it == "." || it == ".." }) return false
        val ext = segments.last().substringAfterLast('.', "").lowercase()
        if (ext in PackSecurityLimits.FORBIDDEN_DECLARATIVE_EXTENSIONS || ext == "apk") return false
        return true
    }

    private fun sanitizeStorageSlug(raw: String): String =
        raw.replace(Regex("""[^a-zA-Z0-9._\-]"""), "_").take(64)

    private fun mapHttpError(err: Throwable): GitHubImportResult.Rejected {
        val msg = err.message.orEmpty()
        return when {
            err is SecurityException -> GitHubImportResult.Rejected(
                messageEs = msg,
                messageEn = msg
            )
            msg.contains("GITHUB_RATE_LIMIT") -> GitHubImportResult.Rejected(
                messageEs = "GitHub alcanzó el límite temporal de consultas públicas (Rate Limit). No se modificó nada; espera unos minutos y vuelve a intentarlo.",
                messageEn = "GitHub public API rate limit reached. Nothing was changed; wait a few minutes and retry."
            )
            msg.contains("GITHUB_NOT_FOUND") -> GitHubImportResult.Rejected(
                messageEs = "No se encontró el repositorio, rama o carpeta en GitHub (404). Comprueba que sea público y exista.",
                messageEn = "Repository, branch, or folder not found on GitHub (404). Ensure it is public and valid."
            )
            else -> GitHubImportResult.Rejected(
                messageEs = "No pudimos verificar el paquete en GitHub ($msg). No se cambió nada.",
                messageEn = "Could not verify the package on GitHub ($msg). Nothing was changed."
            )
        }
    }
}

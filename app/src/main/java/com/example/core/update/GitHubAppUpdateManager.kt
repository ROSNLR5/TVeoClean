package com.example.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.core.restore.IntegrityHashUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

interface ApkArchiveInspector {
    val installedPackageName: String?
    val installedSigningCertSha256: String?
    fun inspectApkFile(apkFile: File): ApkArchiveMetadata?
}

class AndroidApkArchiveInspector(
    private val context: Context
) : ApkArchiveInspector {

    override val installedPackageName: String
        get() = context.packageName

    override val installedSigningCertSha256: String?
        get() = runCatching {
            val pm = context.packageManager
            val pkgInfo = getInstalledPackageInfoWithSigners(pm, context.packageName)
            extractFirstSignatureSha256(pkgInfo)
        }.getOrNull()

    override fun inspectApkFile(apkFile: File): ApkArchiveMetadata? {
        return runCatching {
            val pm = context.packageManager
            val archiveInfo = getArchivePackageInfoWithSigners(pm, apkFile.absolutePath) ?: return null
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.versionCode.toLong()
            }
            ApkArchiveMetadata(
                packageName = archiveInfo.packageName,
                versionName = archiveInfo.versionName,
                versionCode = versionCode,
                signingCertSha256 = extractFirstSignatureSha256(archiveInfo)
            )
        }.getOrNull()
    }

    private fun getInstalledPackageInfoWithSigners(pm: PackageManager, pkgName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                pkgName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
        }
    }

    private fun getArchivePackageInfoWithSigners(pm: PackageManager, archivePath: String): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                archivePath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNATURES)
        }
    }

    private fun extractFirstSignatureSha256(pkgInfo: PackageInfo): String? {
        val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pkgInfo.signingInfo
            val sigs = if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                null
            }
            sigs?.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.signatures?.firstOrNull()?.toByteArray()
        }
        return certBytes?.let { IntegrityHashUtil.sha256BytesHex(it) }
    }
}

interface AppUpdateRepository {
    val currentVersionName: String
    suspend fun checkForUpdates(includePreReleases: Boolean = false): AppUpdateCheckOutcome
    suspend fun downloadAndVerifyUpdate(
        release: AppUpdateReleaseInfo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): AppUpdateDownloadOutcome
    fun canRequestPackageInstalls(): Boolean
    fun launchApkInstaller(verifiedApk: VerifiedDownloadedApk): Result<Unit>
    fun openUnknownSourcesSettings(): Result<Unit>
}

/**
 * Safe offline/test default so unit tests that do not test update networking never perform external requests.
 */
class NoOpAppUpdateRepository(
    override val currentVersionName: String = BuildConfig.VERSION_NAME
) : AppUpdateRepository {
    override suspend fun checkForUpdates(includePreReleases: Boolean): AppUpdateCheckOutcome {
        return AppUpdateCheckOutcome.UpToDate(
            currentVersion = currentVersionName,
            latestPublishedVersion = null,
            hasPublishedReleases = false
        )
    }

    override suspend fun downloadAndVerifyUpdate(
        release: AppUpdateReleaseInfo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): AppUpdateDownloadOutcome {
        return AppUpdateDownloadOutcome.Rejected(
            messageEs = "Repositorio de actualizaciones en modo de prueba sin red.",
            messageEn = "Update repository running in offline test mode."
        )
    }

    override fun canRequestPackageInstalls(): Boolean = true
    override fun launchApkInstaller(verifiedApk: VerifiedDownloadedApk): Result<Unit> = Result.success(Unit)
    override fun openUnknownSourcesSettings(): Result<Unit> = Result.success(Unit)
}

/**
 * Production GitHub Releases Update Engine for `https://github.com/ROSNLR5/TVeoClean`.
 *
 * Security & Reliability Guarantees:
 * - Queries exclusively the official repository `ROSNLR5/TVeoClean` via HTTPS (`api.github.com`).
 * - Evaluates all published releases, parses Semantic Versioning (`SemanticVersion`), and always selects
 *   the highest stable version containing an installable `.apk` asset.
 * - Follows CDN redirects manually (`MAX_REDIRECT_HOPS = 5`) while strictly enforcing `https://` and
 *   GitHub's official release asset hosts (`github.com`, `objects.githubusercontent.com`,
 *   `release-assets.githubusercontent.com`).
 * - Streams `.apk` bytes with progress, enforces size limits (`150 MB`), verifies ZIP/APK header magic,
 *   computes real `SHA-256`, verifies against GitHub's asset `digest` or `.sha256` companion file when
 *   present, and inspects package/signing metadata before prompting Android's `FileProvider` installer.
 */
class GitHubAppUpdateManager(
    private val context: Context? = null,
    private val updateCacheDir: File = context?.cacheDir?.let { File(it, "app_updates") }
        ?: File(System.getProperty("java.io.tmpdir") ?: ".", "tveoclean_app_updates"),
    override val currentVersionName: String = BuildConfig.VERSION_NAME,
    private val repoOwner: String = AppUpdateSecurityConfig.OFFICIAL_REPO_OWNER,
    private val repoName: String = AppUpdateSecurityConfig.OFFICIAL_REPO_NAME,
    private val apiBaseUrl: String = "https://api.github.com",
    private val supportedAbis: List<String> = Build.SUPPORTED_ABIS?.toList().orEmpty(),
    private val archiveInspector: ApkArchiveInspector? = context?.let { AndroidApkArchiveInspector(it) },
    private val httpClient: OkHttpClient = defaultManualRedirectClient(),
    private val allowLoopbackForTest: Boolean = false,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AppUpdateRepository {

    companion object {
        private val SHA256_HEX_REGEX = Regex("""^[0-9a-fA-F]{64}$""")
        private val KNOWN_ABI_TOKENS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

        fun defaultManualRedirectClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
        }
    }

    override suspend fun checkForUpdates(includePreReleases: Boolean): AppUpdateCheckOutcome =
        withContext(ioDispatcher) {
            val installedSemVer = SemanticVersion.parseOrNull(currentVersionName)
                ?: SemanticVersion(1, 0, 0)

            val releasesUrl = "$apiBaseUrl/repos/$repoOwner/$repoName/releases?per_page=25"
            val jsonBytes = fetchBoundedBytesWithControlledRedirects(
                initialUrl = releasesUrl,
                allowedHosts = AppUpdateSecurityConfig.ALLOWED_GITHUB_API_HOSTS,
                maxBytes = AppUpdateSecurityConfig.MAX_RELEASES_JSON_BYTES,
                acceptHeader = "application/vnd.github+json"
            ).getOrElse { err ->
                return@withContext mapNetworkError(err)
            }

            val parsedCandidates = runCatching {
                parseReleaseCandidates(
                    rawJson = jsonBytes.toString(Charsets.UTF_8),
                    includePreReleases = includePreReleases
                )
            }.getOrElse {
                return@withContext AppUpdateCheckOutcome.Failed(
                    messageEs = "La respuesta de GitHub Releases no pudo verificarse. No se descargó nada.",
                    messageEn = "GitHub Releases response could not be verified. Nothing was downloaded."
                )
            }

            if (parsedCandidates.isEmpty()) {
                return@withContext AppUpdateCheckOutcome.UpToDate(
                    currentVersion = currentVersionName,
                    latestPublishedVersion = null,
                    hasPublishedReleases = false
                )
            }

            // Always pick the strictly highest Semantic Version (tie-breaking by publishedAt descending)
            val newestRelease = parsedCandidates.sortedWith(
                compareByDescending<AppUpdateReleaseInfo> { it.semanticVersion }
                    .thenByDescending { it.publishedAtIso }
            ).first()

            if (newestRelease.semanticVersion.isNewerThan(installedSemVer)) {
                AppUpdateCheckOutcome.UpdateAvailable(latestRelease = newestRelease)
            } else {
                AppUpdateCheckOutcome.UpToDate(
                    currentVersion = currentVersionName,
                    latestPublishedVersion = newestRelease.semanticVersion.normalizedString,
                    hasPublishedReleases = true
                )
            }
        }

    override suspend fun downloadAndVerifyUpdate(
        release: AppUpdateReleaseInfo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): AppUpdateDownloadOutcome = withContext(ioDispatcher) {
        val asset = release.apkAsset
        if (!asset.name.lowercase(Locale.ROOT).endsWith(".apk")) {
            return@withContext AppUpdateDownloadOutcome.Rejected(
                messageEs = "El archivo publicado '${asset.name}' no es un paquete .apk válido.",
                messageEn = "Published asset '${asset.name}' is not a valid .apk package."
            )
        }
        if (asset.sizeBytes <= 0L || asset.sizeBytes > AppUpdateSecurityConfig.MAX_APK_BYTES) {
            return@withContext AppUpdateDownloadOutcome.Rejected(
                messageEs = "El tamaño del APK supera el límite de seguridad permitido (150 MB).",
                messageEn = "APK size exceeds the allowed safety limit (150 MB)."
            )
        }

        // Resolve expected SHA-256 from GitHub's native asset digest OR companion .sha256 file if available
        val expectedSha256: String? = asset.declaredSha256 ?: asset.checksumCompanionUrl?.let { companionUrl ->
            fetchBoundedBytesWithControlledRedirects(
                initialUrl = companionUrl,
                allowedHosts = AppUpdateSecurityConfig.ALLOWED_RELEASE_DOWNLOAD_HOSTS,
                maxBytes = AppUpdateSecurityConfig.MAX_CHECKSUM_BYTES,
                acceptHeader = "application/octet-stream"
            ).getOrNull()?.toString(Charsets.UTF_8)?.let { text ->
                extractChecksumForAsset(text, asset.name)
            }
        }

        updateCacheDir.mkdirs()
        // Clean previous partial or stale APKs before downloading
        updateCacheDir.listFiles()?.forEach { old ->
            runCatching { old.delete() }
        }

        val safeFileName = "TVeoClean-${release.semanticVersion.normalizedString}.apk"
            .replace(Regex("""[^0-9A-Za-z._-]"""), "_")
        val partFile = File(updateCacheDir, "$safeFileName.part")
        val targetFile = File(updateCacheDir, safeFileName)

        try {
            val downloadResult = streamDownloadToFileWithControlledRedirects(
                initialUrl = asset.downloadUrl,
                allowedHosts = AppUpdateSecurityConfig.ALLOWED_RELEASE_DOWNLOAD_HOSTS,
                destination = partFile,
                declaredTotalBytes = asset.sizeBytes,
                maxBytes = AppUpdateSecurityConfig.MAX_APK_BYTES,
                onProgress = onProgress
            )
            val computedSha256 = downloadResult.getOrElse { err ->
                partFile.delete()
                val mapped = mapNetworkError(err)
                return@withContext AppUpdateDownloadOutcome.Rejected(
                    messageEs = mapped.messageEs,
                    messageEn = mapped.messageEn
                )
            }

            // 1. Verify ZIP/APK magic header (PK\x03\x04)
            if (!hasValidZipHeader(partFile)) {
                partFile.delete()
                return@withContext AppUpdateDownloadOutcome.Rejected(
                    messageEs = "El archivo descargado no tiene una cabecera APK válida. Descarga descartada.",
                    messageEn = "Downloaded file does not contain a valid APK header. Download discarded."
                )
            }

            // 2. Verify SHA-256 against declared digest / companion .sha256 when published
            if (expectedSha256 != null && !computedSha256.equals(expectedSha256, ignoreCase = true)) {
                partFile.delete()
                return@withContext AppUpdateDownloadOutcome.Rejected(
                    messageEs = "El SHA‑256 del APK descargado no coincide con el publicado en el release. Se bloqueó la instalación.",
                    messageEn = "Downloaded APK SHA‑256 does not match the release checksum. Installation was blocked."
                )
            }

            // 3. Move atomically into final cache path before PackageManager archive inspection
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!partFile.renameTo(targetFile)) {
                partFile.copyTo(targetFile, overwrite = true)
                partFile.delete()
            }

            // 4. Inspect Android APK archive metadata & signing certificate when inspector is available
            val archiveMeta = archiveInspector?.inspectApkFile(targetFile)
            if (archiveInspector != null && archiveMeta == null) {
                targetFile.delete()
                return@withContext AppUpdateDownloadOutcome.Rejected(
                    messageEs = "Android rechazó el archivo porque no es un paquete APK íntegro o compatible.",
                    messageEn = "Android rejected the file because it is not an intact or compatible APK archive."
                )
            }

            val installedPkg = archiveInspector?.installedPackageName
            val installedCert = archiveInspector?.installedSigningCertSha256
            val matchesPkg = archiveMeta?.packageName == null || installedPkg == null ||
                archiveMeta.packageName == installedPkg
            val matchesCert = if (archiveMeta?.signingCertSha256 != null && installedCert != null) {
                archiveMeta.signingCertSha256.equals(installedCert, ignoreCase = true)
            } else {
                null
            }

            AppUpdateDownloadOutcome.Verified(
                VerifiedDownloadedApk(
                    filePath = targetFile.absolutePath,
                    fileName = targetFile.name,
                    sizeBytes = targetFile.length(),
                    computedSha256 = computedSha256,
                    expectedSha256 = expectedSha256,
                    sha256VerifiedAgainstRelease = expectedSha256 != null,
                    archivePackageName = archiveMeta?.packageName,
                    archiveVersionName = archiveMeta?.versionName,
                    signingCertSha256 = archiveMeta?.signingCertSha256,
                    matchesInstalledPackageId = matchesPkg,
                    matchesInstalledSigningCert = matchesCert
                )
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            partFile.delete()
            throw cancelled
        } catch (t: Throwable) {
            partFile.delete()
            AppUpdateDownloadOutcome.Rejected(
                messageEs = "No se pudo completar y verificar la descarga del APK: ${t.message ?: "Error de E/S"}",
                messageEn = "Could not complete and verify APK download: ${t.message ?: "I/O error"}"
            )
        }
    }

    override fun canRequestPackageInstalls(): Boolean {
        val ctx = context ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    override fun openUnknownSourcesSettings(): Result<Unit> {
        val ctx = context ?: return Result.failure(IllegalStateException("Context unavailable"))
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${ctx.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
        }
    }

    override fun launchApkInstaller(verifiedApk: VerifiedDownloadedApk): Result<Unit> {
        val ctx = context ?: return Result.failure(IllegalStateException("Context unavailable"))
        return runCatching {
            val apkFile = File(verifiedApk.filePath)
            if (!apkFile.exists() || !apkFile.isFile) {
                throw IOException("APK file missing from cache. Download again.")
            }
            // Re-verify SHA-256 right before handing URI to Android's system PackageInstaller (TOCTOU guard)
            val currentSha = computeFileSha256(apkFile)
            if (!currentSha.equals(verifiedApk.computedSha256, ignoreCase = true)) {
                apkFile.delete()
                throw SecurityException("APK integrity changed since verification.")
            }

            val contentUri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(installIntent)
        }
    }

    private fun parseReleaseCandidates(
        rawJson: String,
        includePreReleases: Boolean
    ): List<AppUpdateReleaseInfo> {
        val array = JSONArray(rawJson)
        val results = mutableListOf<AppUpdateReleaseInfo>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optBoolean("draft", false)) continue
            if (!includePreReleases && obj.optBoolean("prerelease", false)) continue

            val tagName = obj.optString("tag_name", "").trim()
            val releaseTitle = obj.optString("name", "").trim().ifBlank { tagName }
            val semVer = SemanticVersion.parseOrNull(tagName)
                ?: SemanticVersion.parseOrNull(releaseTitle)
                ?: continue

            val htmlUrl = obj.optString("html_url", AppUpdateSecurityConfig.OFFICIAL_RELEASES_HTML_URL)
            val changelog = obj.optString("body", "").trim()
            val publishedAt = obj.optString("published_at", "")

            val assetsJson = obj.optJSONArray("assets") ?: continue
            val selectedApk = selectBestApkAsset(assetsJson) ?: continue

            results += AppUpdateReleaseInfo(
                tagName = tagName,
                semanticVersion = semVer,
                releaseTitle = releaseTitle,
                changelog = changelog,
                publishedAtIso = publishedAt,
                releaseHtmlUrl = htmlUrl,
                apkAsset = selectedApk
            )
        }
        return results
    }

    private fun selectBestApkAsset(assetsJson: JSONArray): GitHubReleaseAssetInfo? {
        data class RawAsset(
            val name: String,
            val downloadUrl: String,
            val size: Long,
            val contentType: String,
            val digestSha256: String?
        )

        val allAssets = mutableListOf<RawAsset>()
        for (i in 0 until assetsJson.length()) {
            val a = assetsJson.optJSONObject(i) ?: continue
            val name = a.optString("name", "").trim()
            val url = a.optString("browser_download_url", "").trim()
            val size = a.optLong("size", 0L)
            val contentType = a.optString("content_type", "application/vnd.android.package-archive")
            val rawDigest = a.optString("digest", "").trim()
            val parsedDigest = if (rawDigest.startsWith("sha256:", ignoreCase = true)) {
                rawDigest.substringAfter(':').lowercase(Locale.ROOT).takeIf { SHA256_HEX_REGEX.matches(it) }
            } else {
                null
            }
            if (name.isNotEmpty() && url.isNotEmpty()) {
                allAssets += RawAsset(name, url, size, contentType, parsedDigest)
            }
        }

        val apkCandidates = allAssets.filter { asset ->
            asset.name.lowercase(Locale.ROOT).endsWith(".apk") &&
                asset.size in 1L..AppUpdateSecurityConfig.MAX_APK_BYTES &&
                isAllowedDownloadUrl(asset.downloadUrl)
        }
        if (apkCandidates.isEmpty()) return null

        val primaryAbi = supportedAbis.firstOrNull()?.lowercase(Locale.ROOT)
        val bestApk = apkCandidates.sortedWith(
            compareByDescending<RawAsset> { candidate ->
                val lower = candidate.name.lowercase(Locale.ROOT)
                val mentionsAnyAbi = KNOWN_ABI_TOKENS.any { lower.contains(it) }
                when {
                    primaryAbi != null && lower.contains(primaryAbi) -> 3 // Exact device ABI match
                    !mentionsAnyAbi || lower.contains("universal") -> 2   // Universal APK
                    supportedAbis.any { lower.contains(it.lowercase(Locale.ROOT)) } -> 1 // Secondary supported ABI
                    else -> 0
                }
            }.thenByDescending { candidate ->
                // Prefer release builds over debug builds when both exist in a release
                !candidate.name.contains("debug", ignoreCase = true)
            }
        ).first()

        // Locate optional companion .sha256 / SHA256SUMS file in the same release
        val companionAsset = allAssets.firstOrNull { a ->
            val lower = a.name.lowercase(Locale.ROOT)
            (lower == "${bestApk.name.lowercase(Locale.ROOT)}.sha256" ||
                lower == "sha256sums" ||
                lower == "sha256sums.txt" ||
                lower == "checksums.txt" ||
                lower.endsWith(".sha256")) &&
                a.size in 1L..AppUpdateSecurityConfig.MAX_CHECKSUM_BYTES &&
                isAllowedDownloadUrl(a.downloadUrl)
        }

        return GitHubReleaseAssetInfo(
            name = bestApk.name,
            downloadUrl = bestApk.downloadUrl,
            sizeBytes = bestApk.size,
            contentType = bestApk.contentType,
            declaredSha256 = bestApk.digestSha256,
            checksumCompanionUrl = companionAsset?.downloadUrl
        )
    }

    private fun extractChecksumForAsset(checksumText: String, apkFileName: String): String? {
        val lines = checksumText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        // First look for a line explicitly naming this APK
        for (line in lines) {
            val tokens = line.split(Regex("""[\s*]+"""))
            if (tokens.size >= 2) {
                val hashCandidate = tokens[0]
                val fileToken = tokens.last().substringAfterLast('/')
                if (SHA256_HEX_REGEX.matches(hashCandidate) && fileToken.equals(apkFileName, ignoreCase = true)) {
                    return hashCandidate.lowercase(Locale.ROOT)
                }
            }
        }
        // Fallback if the .sha256 file contains a single bare 64-char hex digest
        if (lines.size == 1) {
            val firstToken = lines[0].split(Regex("""\s+""")).firstOrNull().orEmpty()
            if (SHA256_HEX_REGEX.matches(firstToken)) {
                return firstToken.lowercase(Locale.ROOT)
            }
        }
        return null
    }

    private fun isAllowedDownloadUrl(rawUrl: String): Boolean {
        val parsed = rawUrl.toHttpUrlOrNull() ?: return false
        val host = parsed.host.lowercase(Locale.ROOT)
        if (allowLoopbackForTest && (host == "127.0.0.1" || host == "localhost")) {
            return true
        }
        return parsed.scheme == "https" && host in AppUpdateSecurityConfig.ALLOWED_RELEASE_DOWNLOAD_HOSTS
    }

    private fun openVerifiedResponseWithRedirects(
        initialUrl: String,
        allowedHosts: Set<String>,
        acceptHeader: String
    ): Response {
        var currentUrl = initialUrl
        var hops = 0

        while (true) {
            val parsed = currentUrl.toHttpUrlOrNull()
                ?: throw SecurityException("URL inválida bloqueada: $currentUrl")
            val host = parsed.host.lowercase(Locale.ROOT)
            val isLoopbackOk = allowLoopbackForTest && (host == "127.0.0.1" || host == "localhost")

            if (!isLoopbackOk) {
                if (parsed.scheme != "https") {
                    throw SecurityException("Se requiere HTTPS estricto para actualizar desde GitHub.")
                }
                if (host !in allowedHosts) {
                    throw SecurityException("Dominio no autorizado bloqueado durante actualización: $host")
                }
            }

            val req = Request.Builder()
                .url(parsed)
                .header("Accept", acceptHeader)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "TVeoClean-Android/$currentVersionName")
                .get()
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.code in listOf(301, 302, 303, 307, 308)) {
                val location = resp.header("Location")
                resp.close()
                if (location.isNullOrBlank()) {
                    throw SecurityException("Redirección sin destino bloqueada.")
                }
                if (hops >= AppUpdateSecurityConfig.MAX_REDIRECT_HOPS) {
                    throw SecurityException("Demasiadas redirecciones al descargar desde GitHub.")
                }
                val resolvedRedirect = parsed.resolve(location)
                    ?: throw SecurityException("Redirección malformada bloqueada.")
                currentUrl = resolvedRedirect.toString()
                hops++
                continue
            }
            return resp
        }
    }

    private fun fetchBoundedBytesWithControlledRedirects(
        initialUrl: String,
        allowedHosts: Set<String>,
        maxBytes: Long,
        acceptHeader: String
    ): Result<ByteArray> = runCatching {
        openVerifiedResponseWithRedirects(
            initialUrl = initialUrl,
            allowedHosts = allowedHosts,
            acceptHeader = acceptHeader
        ).use { resp ->
            if (resp.code == 403 || resp.code == 429) {
                throw IOException("HTTP_RATE_LIMIT")
            }
            if (resp.code == 404) {
                throw IOException("HTTP_NOT_FOUND")
            }
            if (!resp.isSuccessful) {
                throw IOException("HTTP_${resp.code}")
            }
            val body = resp.body ?: throw IOException("Respuesta vacía")
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0L
            body.byteStream().use { stream ->
                while (true) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > maxBytes) {
                        throw SecurityException("Respuesta excede el límite seguro permitido.")
                    }
                    out.write(buffer, 0, read)
                }
            }
            out.toByteArray()
        }
    }

    private suspend fun streamDownloadToFileWithControlledRedirects(
        initialUrl: String,
        allowedHosts: Set<String>,
        destination: File,
        declaredTotalBytes: Long,
        maxBytes: Long,
        onProgress: (Long, Long) -> Unit
    ): Result<String> = runCatching {
        openVerifiedResponseWithRedirects(
            initialUrl = initialUrl,
            allowedHosts = allowedHosts,
            acceptHeader = "application/octet-stream"
        ).use { resp ->
            if (resp.code == 403 || resp.code == 429) {
                throw IOException("HTTP_RATE_LIMIT")
            }
            if (resp.code == 404) {
                throw IOException("HTTP_NOT_FOUND")
            }
            if (!resp.isSuccessful) {
                throw IOException("HTTP_${resp.code}")
            }

            val body = resp.body ?: throw IOException(" Cuerpo de APK vacío")
            val contentLen = body.contentLength().takeIf { it > 0L } ?: declaredTotalBytes
            if (contentLen > maxBytes) {
                throw SecurityException("APK supera el tamaño máximo permitido (150 MB).")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            val buffer = ByteArray(16 * 1024)

            body.byteStream().use { input ->
                FileOutputStream(destination).use { output ->
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        downloaded += read
                        if (downloaded > maxBytes) {
                            throw SecurityException("APK excedió el límite de 150 MB durante la descarga.")
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                        onProgress(downloaded, contentLen)
                    }
                    output.flush()
                }
            }

            if (downloaded == 0L) {
                throw IOException("Archivo APK vacío recibido.")
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    private fun hasValidZipHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 4L) return false
        return runCatching {
            file.inputStream().use { s ->
                val b0 = s.read()
                val b1 = s.read()
                val b2 = s.read()
                val b3 = s.read()
                b0 == 0x50 && b1 == 0x4B && b2 == 0x03 && b3 == 0x04
            }
        }.getOrDefault(false)
    }

    private fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun mapNetworkError(err: Throwable): AppUpdateCheckOutcome.Failed {
        val msg = err.message.orEmpty()
        return when {
            err is SecurityException -> AppUpdateCheckOutcome.Failed(
                messageEs = err.message ?: "Bloqueado por control de seguridad de actualización.",
                messageEn = err.message ?: "Blocked by update security guard."
            )
            msg == "HTTP_RATE_LIMIT" -> AppUpdateCheckOutcome.Failed(
                messageEs = "GitHub limitó temporalmente las consultas. Inténtalo de nuevo en unos minutos.",
                messageEn = "GitHub temporarily rate-limited requests. Please try again in a few minutes."
            )
            msg == "HTTP_NOT_FOUND" -> AppUpdateCheckOutcome.Failed(
                messageEs = "No se encontró el canal de lanzamientos en github.com/$repoOwner/$repoName.",
                messageEn = "Could not find releases channel on github.com/$repoOwner/$repoName."
            )
            else -> AppUpdateCheckOutcome.Failed(
                messageEs = "No pudimos conectar con GitHub Releases ahora mismo. Comprueba tu conexión a Internet.",
                messageEn = "Could not reach GitHub Releases right now. Check your internet connection."
            )
        }
    }
}

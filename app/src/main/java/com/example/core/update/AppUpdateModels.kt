package com.example.core.update

import com.example.ui.i18n.AppLanguage

object AppUpdateSecurityConfig {
    const val OFFICIAL_REPO_OWNER = "ROSNLR5"
    const val OFFICIAL_REPO_NAME = "TVeoClean"
    const val OFFICIAL_REPO_HTML_URL = "https://github.com/ROSNLR5/TVeoClean"
    const val OFFICIAL_RELEASES_HTML_URL = "https://github.com/ROSNLR5/TVeoClean/releases"

    const val MAX_RELEASES_JSON_BYTES: Long = 1024L * 1024L // 1 MB JSON response limit
    const val MAX_CHECKSUM_BYTES: Long = 64L * 1024L        // 64 KB SHA-256 companion file limit
    const val MAX_APK_BYTES: Long = 150L * 1024L * 1024L    // 150 MB upper bound for APK downloads
    const val MAX_REDIRECT_HOPS: Int = 5

    val ALLOWED_GITHUB_API_HOSTS: Set<String> = setOf(
        "api.github.com"
    )

    val ALLOWED_RELEASE_DOWNLOAD_HOSTS: Set<String> = setOf(
        "github.com",
        "api.github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "raw.githubusercontent.com"
    )
}

data class GitHubReleaseAssetInfo(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val contentType: String,
    val declaredSha256: String? = null,
    val checksumCompanionUrl: String? = null
) {
    fun formattedSizeMb(): String {
        val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}

data class AppUpdateReleaseInfo(
    val tagName: String,
    val semanticVersion: SemanticVersion,
    val releaseTitle: String,
    val changelog: String,
    val publishedAtIso: String,
    val releaseHtmlUrl: String,
    val apkAsset: GitHubReleaseAssetInfo
) {
    fun formattedPublishedDate(): String = publishedAtIso.take(10).ifBlank { "GitHub Release" }
}

data class ApkArchiveMetadata(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val signingCertSha256: String?
)

data class VerifiedDownloadedApk(
    val filePath: String,
    val fileName: String,
    val sizeBytes: Long,
    val computedSha256: String,
    val expectedSha256: String?,
    val sha256VerifiedAgainstRelease: Boolean,
    val archivePackageName: String?,
    val archiveVersionName: String?,
    val signingCertSha256: String?,
    val matchesInstalledPackageId: Boolean,
    val matchesInstalledSigningCert: Boolean?
)

sealed interface AppUpdateCheckOutcome {
    data class UpdateAvailable(
        val latestRelease: AppUpdateReleaseInfo
    ) : AppUpdateCheckOutcome

    data class UpToDate(
        val currentVersion: String,
        val latestPublishedVersion: String?,
        val hasPublishedReleases: Boolean
    ) : AppUpdateCheckOutcome

    data class Failed(
        val messageEs: String,
        val messageEn: String
    ) : AppUpdateCheckOutcome
}

sealed interface AppUpdateDownloadOutcome {
    data class Verified(
        val verifiedApk: VerifiedDownloadedApk
    ) : AppUpdateDownloadOutcome

    data class Rejected(
        val messageEs: String,
        val messageEn: String
    ) : AppUpdateDownloadOutcome
}

sealed interface AppUpdateUiState {
    val currentVersion: String

    data class Idle(
        override val currentVersion: String
    ) : AppUpdateUiState

    data class Checking(
        override val currentVersion: String
    ) : AppUpdateUiState

    data class UpToDate(
        override val currentVersion: String,
        val latestPublishedVersion: String?,
        val hasPublishedReleases: Boolean
    ) : AppUpdateUiState

    data class UpdateAvailable(
        override val currentVersion: String,
        val release: AppUpdateReleaseInfo
    ) : AppUpdateUiState

    data class Downloading(
        override val currentVersion: String,
        val release: AppUpdateReleaseInfo,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : AppUpdateUiState {
        val progressFraction: Float
            get() = if (totalBytes > 0L) {
                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
    }

    data class ReadyToInstall(
        override val currentVersion: String,
        val release: AppUpdateReleaseInfo,
        val verifiedApk: VerifiedDownloadedApk,
        val canInstallPackages: Boolean
    ) : AppUpdateUiState

    data class Error(
        override val currentVersion: String,
        val messageEs: String,
        val messageEn: String,
        val release: AppUpdateReleaseInfo? = null
    ) : AppUpdateUiState {
        fun localizedMessage(lang: AppLanguage): String =
            if (lang == AppLanguage.ES) messageEs else messageEn
    }
}

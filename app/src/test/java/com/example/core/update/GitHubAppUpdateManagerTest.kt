package com.example.core.update

import com.example.BuildConfig
import com.example.core.restore.IntegrityHashUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GitHubAppUpdateManagerTest {

    private fun buildValidMinimalApkBytes(marker: String = "v1.2.0"): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zip.write("TVeoClean-APK-$marker".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    @Test
    fun `BuildConfig VERSION_NAME is 1_0_0 and SemanticVersion compares tags accurately`() {
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)

        val v100 = SemanticVersion.parseOrNull("1.0.0")
        val tagV100 = SemanticVersion.parseOrNull("v1.0.0")
        val v101 = SemanticVersion.parseOrNull("v1.0.1")
        val v122 = SemanticVersion.parseOrNull("v1.2.2")
        val v1210 = SemanticVersion.parseOrNull("release-1.2.10")
        val v130Rc1 = SemanticVersion.parseOrNull("v1.3.0-rc.1")
        val v130Stable = SemanticVersion.parseOrNull("v1.3.0")

        assertNotNull(v100)
        assertEquals(v100, tagV100)
        assertTrue(v101!!.isNewerThan(v100!!))
        assertTrue(v1210!!.isNewerThan(v122!!))
        assertTrue(v130Stable!!.isNewerThan(v130Rc1!!))
        assertFalse(v100.isNewerThan(tagV100!!))
    }

    @Test
    fun `GitHubAppUpdateManager always selects highest stable release from ROSNLR5 TVeoClean and verifies redirected APK SHA-256`() = runBlocking {
        val apkBytesV120 = buildValidMinimalApkBytes("1.2.0")
        val sha256V120 = IntegrityHashUtil.sha256BytesHex(apkBytesV120)

        val releasesJson = """
            [
              {
                "tag_name": "v2.0.0",
                "name": "Draft 2.0.0",
                "draft": true,
                "prerelease": false,
                "published_at": "2026-10-28T10:00:00Z",
                "assets": [
                  {
                    "name": "TVeoClean-v2.0.0.apk",
                    "browser_download_url": "https://github.com/ROSNLR5/TVeoClean/releases/download/v2.0.0/TVeoClean-v2.0.0.apk",
                    "size": ${apkBytesV120.size}
                  }
                ]
              },
              {
                "tag_name": "v1.3.0-beta.1",
                "name": "Beta 1.3.0",
                "draft": false,
                "prerelease": true,
                "published_at": "2026-10-27T10:00:00Z",
                "assets": [
                  {
                    "name": "TVeoClean-v1.3.0-beta.1.apk",
                    "browser_download_url": "https://github.com/ROSNLR5/TVeoClean/releases/download/v1.3.0-beta.1/TVeoClean-v1.3.0-beta.1.apk",
                    "size": ${apkBytesV120.size}
                  }
                ]
              },
              {
                "tag_name": "v1.0.2",
                "name": "TVeoClean 1.0.2 Hotfix",
                "draft": false,
                "prerelease": false,
                "published_at": "2026-10-27T09:00:00Z",
                "assets": [
                  {
                    "name": "TVeoClean-v1.0.2.apk",
                    "browser_download_url": "https://github.com/ROSNLR5/TVeoClean/releases/download/v1.0.2/TVeoClean-v1.0.2.apk",
                    "size": ${apkBytesV120.size}
                  }
                ]
              },
              {
                "tag_name": "v1.2.0",
                "name": "TVeoClean 1.2.0 Oficial",
                "body": "Mejoras de rendimiento y nuevos perfiles verificados.",
                "draft": false,
                "prerelease": false,
                "html_url": "https://github.com/ROSNLR5/TVeoClean/releases/tag/v1.2.0",
                "published_at": "2026-10-26T20:00:00Z",
                "assets": [
                  {
                    "name": "TVeoClean-v1.2.0-debug.apk",
                    "browser_download_url": "https://github.com/ROSNLR5/TVeoClean/releases/download/v1.2.0/TVeoClean-v1.2.0-debug.apk",
                    "size": ${apkBytesV120.size}
                  },
                  {
                    "name": "TVeoClean-v1.2.0-arm64-v8a-release.apk",
                    "browser_download_url": "https://github.com/ROSNLR5/TVeoClean/releases/download/v1.2.0/TVeoClean-v1.2.0-arm64-v8a-release.apk",
                    "size": ${apkBytesV120.size},
                    "digest": "sha256:$sha256V120"
                  }
                ]
              }
            ]
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.toString()
                when (url) {
                    "https://api.github.com/repos/ROSNLR5/TVeoClean/releases?per_page=25" -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(releasesJson.toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                    "https://github.com/ROSNLR5/TVeoClean/releases/download/v1.2.0/TVeoClean-v1.2.0-arm64-v8a-release.apk" -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(302)
                            .message("Found")
                            .header("Location", "https://objects.githubusercontent.com/github-production-release-asset/TVeoClean-v1.2.0.apk")
                            .body("".toResponseBody(null))
                            .build()
                    }
                    "https://objects.githubusercontent.com/github-production-release-asset/TVeoClean-v1.2.0.apk" -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(apkBytesV120.toResponseBody("application/vnd.android.package-archive".toMediaType()))
                            .build()
                    }
                    else -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(404)
                            .message("Not Found")
                            .body("{}".toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                }
            })
            .build()

        val tempCache = Files.createTempDirectory("tveo_update_cache").toFile()
        try {
            val fakeInspector = object : ApkArchiveInspector {
                override val installedPackageName: String = "com.aistudio.tveoclean.kxmprq"
                override val installedSigningCertSha256: String = "aa11bb22cc33"
                override fun inspectApkFile(apkFile: File): ApkArchiveMetadata {
                    return ApkArchiveMetadata(
                        packageName = "com.aistudio.tveoclean.kxmprq",
                        versionName = "1.2.0",
                        versionCode = 10200L,
                        signingCertSha256 = "aa11bb22cc33"
                    )
                }
            }

            val manager = GitHubAppUpdateManager(
                context = null,
                updateCacheDir = tempCache,
                currentVersionName = "1.0.0",
                supportedAbis = listOf("arm64-v8a"),
                archiveInspector = fakeInspector,
                httpClient = mockClient
            )

            // 1. Check updates -> selects v1.2.0 (ignoring draft 2.0.0, beta 1.3.0, and older 1.0.2!)
            val checkOutcome = manager.checkForUpdates()
            assertTrue("Expected UpdateAvailable, got $checkOutcome", checkOutcome is AppUpdateCheckOutcome.UpdateAvailable)
            val latest = (checkOutcome as AppUpdateCheckOutcome.UpdateAvailable).latestRelease
            assertEquals("1.2.0", latest.semanticVersion.normalizedString)
            assertEquals("TVeoClean-v1.2.0-arm64-v8a-release.apk", latest.apkAsset.name)
            assertEquals(sha256V120, latest.apkAsset.declaredSha256)

            // 2. Download & verify -> follows controlled redirect to objects.githubusercontent.com & validates SHA-256
            var sawProgress = false
            val dlOutcome = manager.downloadAndVerifyUpdate(latest) { downloaded, _ ->
                if (downloaded > 0L) sawProgress = true
            }
            assertTrue("Expected Verified, got $dlOutcome", dlOutcome is AppUpdateDownloadOutcome.Verified)
            assertTrue(sawProgress)
            val verifiedApk = (dlOutcome as AppUpdateDownloadOutcome.Verified).verifiedApk
            assertEquals(sha256V120, verifiedApk.computedSha256)
            assertTrue(verifiedApk.sha256VerifiedAgainstRelease)
            assertTrue(verifiedApk.matchesInstalledPackageId)
            assertEquals(true, verifiedApk.matchesInstalledSigningCert)
            assertTrue(File(verifiedApk.filePath).exists())
        } finally {
            tempCache.deleteRecursively()
        }
    }

    @Test
    fun `GitHubAppUpdateManager blocks untrusted redirect hosts, tampered SHA-256 APKs, and handles empty releases cleanly`() = runBlocking {
        val validApk = buildValidMinimalApkBytes("tampered")
        val wrongSha256 = "0".repeat(64)

        val mockClient = OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.toString()
                when (url) {
                    "https://api.github.com/repos/ROSNLR5/TVeoClean/releases?per_page=25" -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("[]".toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                    "https://github.com/ROSNLR5/TVeoClean/releases/download/v1.1.0/evil-redirect.apk" -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(302)
                            .message("Found")
                            .header("Location", "https://evil-cdn.attacker.example/payload.apk")
                            .body("".toResponseBody(null))
                            .build()
                    }
                    "https://github.com/ROSNLR5/TVeoClean/releases/download/v1.1.0/tampered.apk" -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(validApk.toResponseBody("application/vnd.android.package-archive".toMediaType()))
                            .build()
                    }
                    else -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(404)
                            .message("Not Found")
                            .body("{}".toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                }
            })
            .build()

        val tempCache = Files.createTempDirectory("tveo_update_sec_test").toFile()
        try {
            val manager = GitHubAppUpdateManager(
                context = null,
                updateCacheDir = tempCache,
                currentVersionName = "1.0.0",
                archiveInspector = null,
                httpClient = mockClient
            )

            // 1. Empty [] releases -> UpToDate(hasPublishedReleases = false)
            val emptyCheck = manager.checkForUpdates()
            assertTrue(emptyCheck is AppUpdateCheckOutcome.UpToDate)
            assertFalse((emptyCheck as AppUpdateCheckOutcome.UpToDate).hasPublishedReleases)

            // 2. Redirect to non-GitHub domain must be rejected
            val redirectRelease = AppUpdateReleaseInfo(
                tagName = "v1.1.0",
                semanticVersion = SemanticVersion(1, 1, 0),
                releaseTitle = "v1.1.0",
                changelog = "",
                publishedAtIso = "2026-10-26",
                releaseHtmlUrl = AppUpdateSecurityConfig.OFFICIAL_RELEASES_HTML_URL,
                apkAsset = GitHubReleaseAssetInfo(
                    name = "evil-redirect.apk",
                    downloadUrl = "https://github.com/ROSNLR5/TVeoClean/releases/download/v1.1.0/evil-redirect.apk",
                    sizeBytes = validApk.size.toLong(),
                    contentType = "application/vnd.android.package-archive"
                )
            )
            val redirectOutcome = manager.downloadAndVerifyUpdate(redirectRelease)
            assertTrue("Untrusted redirect must be rejected", redirectOutcome is AppUpdateDownloadOutcome.Rejected)

            // 3. Mismatched SHA-256 must be rejected and no APK left behind
            val tamperedRelease = redirectRelease.copy(
                apkAsset = GitHubReleaseAssetInfo(
                    name = "tampered.apk",
                    downloadUrl = "https://github.com/ROSNLR5/TVeoClean/releases/download/v1.1.0/tampered.apk",
                    sizeBytes = validApk.size.toLong(),
                    contentType = "application/vnd.android.package-archive",
                    declaredSha256 = wrongSha256
                )
            )
            val tamperedOutcome = manager.downloadAndVerifyUpdate(tamperedRelease)
            assertTrue("Tampered SHA-256 must be rejected", tamperedOutcome is AppUpdateDownloadOutcome.Rejected)
            assertTrue("Cache dir must contain zero retained APKs", tempCache.listFiles().orEmpty().isEmpty())
        } finally {
            tempCache.deleteRecursively()
        }
    }
}

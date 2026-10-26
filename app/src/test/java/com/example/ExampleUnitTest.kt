package com.example

import com.example.core.adb.ActiveLanSubnetScanner
import com.example.core.adb.AdbErrorKind
import com.example.core.adb.AdbSessionOutcome
import com.example.core.adb.FakeAdbServer
import com.example.core.adb.KeystoreAdbKeyManager
import com.example.core.adb.LanHostValidator
import com.example.core.adb.SocketAdbTransportClient
import com.example.core.adb.TvRemoteKey
import com.example.core.adb.WirelessPairingValidator
import com.example.core.engine.BatchExecutionOutcome
import com.example.core.engine.DifferentialRollbackOutcome
import com.example.core.engine.PackageClassification
import com.example.core.engine.ProtectedPackageRegistry
import com.example.core.engine.ReadOnlyTvAnalyzer
import com.example.core.engine.TransactionalCleanEngine
import com.example.core.packs.GitHubImportResult
import com.example.core.packs.GitHubPackImporter
import com.example.core.packs.PackTrustTier
import com.example.core.packs.StrictGitHubUrlParser
import com.example.core.platform.CapabilitySupportLevel
import com.example.core.platform.HonestPlatformRegistry
import com.example.core.platform.TvPlatformFamily
import com.example.core.privacy.SignalCertainty
import com.example.core.privacy.TvPrivacyInspector
import com.example.core.restore.IntegrityHashUtil
import com.example.core.restore.LocalSnapshotRecord
import com.example.ui.i18n.AppLanguage
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `LanHostValidator and ActiveLanSubnetScanner accept private RFC1918 networks and reject public Internet`() {
        assertTrue(LanHostValidator.validate("192.168.1.50", "5555") is LanHostValidator.ValidationResult.ValidLanTarget)
        assertTrue(LanHostValidator.validate("10.0.0.24", "38912") is LanHostValidator.ValidationResult.ValidLanTarget)
        assertTrue(LanHostValidator.validate("sony-bravia-tv.local", "5555") is LanHostValidator.ValidationResult.ValidLanTarget)
        assertTrue(LanHostValidator.validate("8.8.8.8", "5555") is LanHostValidator.ValidationResult.Rejected)
        assertTrue(LanHostValidator.validate("127.0.0.1", "5555") is LanHostValidator.ValidationResult.Rejected)

        assertTrue(ActiveLanSubnetScanner.isValidPrivatePrefix("192.168.1"))
        assertTrue(ActiveLanSubnetScanner.isValidPrivatePrefix("10.0.0"))
        assertTrue(ActiveLanSubnetScanner.isValidPrivatePrefix("172.16.10"))
        assertFalse(ActiveLanSubnetScanner.isValidPrivatePrefix("8.8.8"))
        assertFalse(ActiveLanSubnetScanner.isValidPrivatePrefix("172.32.0"))
    }

    @Test
    fun `StrictGitHubUrlParser rejects non-HTTPS, lookalike hosts, credentials, traversal and scripts`() {
        val validTree = StrictGitHubUrlParser.parse("https://github.com/Gathin23/cleanyourtv/tree/main/themes/aurora")
        assertTrue(validTree is StrictGitHubUrlParser.ParseOutcome.Valid)
        val parsed = (validTree as StrictGitHubUrlParser.ParseOutcome.Valid).parsed
        assertEquals("Gathin23", parsed.owner)
        assertEquals("cleanyourtv", parsed.repo)
        assertEquals("main", parsed.refOrDefault)
        assertEquals("themes/aurora", parsed.relativePath)

        assertTrue(StrictGitHubUrlParser.parse("http://github.com/user/repo") is StrictGitHubUrlParser.ParseOutcome.Invalid)
        assertTrue(StrictGitHubUrlParser.parse("https://evil-github.com/user/repo") is StrictGitHubUrlParser.ParseOutcome.Invalid)
        assertTrue(StrictGitHubUrlParser.parse("https://token@github.com/user/repo") is StrictGitHubUrlParser.ParseOutcome.Invalid)
        assertTrue(StrictGitHubUrlParser.parse("https://github.com/user/repo/tree/main/../../etc/passwd") is StrictGitHubUrlParser.ParseOutcome.Invalid)
        assertTrue(StrictGitHubUrlParser.parse("https://github.com/user/repo/blob/main/install.sh") is StrictGitHubUrlParser.ParseOutcome.Invalid)
    }

    @Test
    fun `ProtectedPackageRegistry unconditionally guards Wi-Fi, Settings, Play Store and Home launchers`() {
        assertTrue(ProtectedPackageRegistry.isProtected("com.google.android.tvlauncher"))
        assertTrue(ProtectedPackageRegistry.isProtected("com.google.android.apps.tv.launcherx"))
        assertTrue(ProtectedPackageRegistry.isProtected("com.android.tv.settings"))
        assertTrue(ProtectedPackageRegistry.isProtected("com.android.vending"))
        assertTrue(ProtectedPackageRegistry.isProtected("com.android.wifi.resources"))
        assertFalse(ProtectedPackageRegistry.isProtected("com.example.optional.promoapp"))
    }

    @Test
    fun `HonestPlatformRegistry marks Samsung Tizen, LG webOS and Roku as UNAVAILABLE for ADB`() {
        val nonAndroidFamilies = listOf(
            TvPlatformFamily.SAMSUNG_TIZEN,
            TvPlatformFamily.LG_WEBOS,
            TvPlatformFamily.ROKU_OS
        )
        nonAndroidFamilies.forEach { family ->
            val matrix = HonestPlatformRegistry.matrices.first { it.family == family }
            assertFalse(matrix.family.supportsAdb)
            assertEquals(CapabilitySupportLevel.UNAVAILABLE, matrix.readOnlyInspect)
            assertEquals(CapabilitySupportLevel.UNAVAILABLE, matrix.reversibleAppDisable)
        }
    }

    @Test
    fun `IntegrityHashUtil redacts IPv4 and pairing PINs in exported diagnostics`() {
        val raw = "Conectando a 192.168.1.44:5555 con PIN: 482910 en sala"
        val redacted = IntegrityHashUtil.redactSensitiveDiagnostics(raw)
        assertFalse(redacted.contains("192.168.1.44"))
        assertFalse(redacted.contains("482910"))
        assertTrue(redacted.contains("[IP-LOCAL-REDACTADA]"))
    }

    @Test
    fun `KeystoreAdbKeyManager generates valid 2048-bit RSA signature, ADB public key struct and rotates cleanly`() {
        val keyManager = KeystoreAdbKeyManager(context = null)
        val fpBefore = keyManager.publicKeyFingerprintSha256()
        assertEquals(32, fpBefore.split(":").size)

        val token = ByteArray(20) { it.toByte() }
        val signature = keyManager.signAdbToken(token)
        assertEquals(256, signature.size)

        val pubKeyPayload = keyManager.adbPublicKeyPayloadBytes("tveoclean@test").toString(Charsets.UTF_8)
        assertTrue(pubKeyPayload.endsWith(" tveoclean@test\u0000"))

        keyManager.rotateOrRevokeKeys()
        val fpAfter = keyManager.publicKeyFingerprintSha256()
        assertNotEquals(fpBefore, fpAfter)
    }

    @Test
    fun `TransactionalCleanEngine executes reversible batches, verifies live TV state, blocks protected packages and performs differential rollback`() = runBlocking {
        FakeAdbServer(mode = FakeAdbServer.ServerMode.ACCEPT_AFTER_PUBLIC_KEY).use { server ->
            server.start()
            val client = SocketAdbTransportClient(
                keyManager = KeystoreAdbKeyManager(context = null),
                allowLoopbackForTesting = true
            )
            val outcome = client.connect("127.0.0.1", server.port)
            assertTrue(outcome is AdbSessionOutcome.Connected)

            // 1. Read-only inspection
            val analyzer = ReadOnlyTvAnalyzer()
            val report = analyzer.analyzeConnectedTv(client).getOrThrow()
            assertTrue(server.isPackageCurrentlyEnabled("com.sony.dtv.promos"))

            // 2. Attempt to disable a protected critical package via transport -> SecurityException!
            val blockedProtected = client.setPackageEnabledState(
                packageName = "com.android.tv.settings",
                enabled = false,
                activeHomePackage = report.currentHomePackage
            )
            assertTrue(blockedProtected.isFailure)
            assertTrue(blockedProtected.exceptionOrNull() is SecurityException)

            // 3. Build reversible plan (even if caller passes a protected package in set, buildPlanFromSelection strips it!)
            val engine = TransactionalCleanEngine(batchSize = 3)
            val plan = engine.buildPlanFromSelection(
                report = report,
                selectedPackagesToToggle = setOf(
                    "com.sony.dtv.promos",
                    "com.google.android.tungsten.setupwraith",
                    "com.google.android.apps.tv.launcherx" // protected HOME must be stripped!
                )
            )
            assertEquals(2, plan.items.size)
            assertFalse(plan.items.any { it.packageName == "com.google.android.apps.tv.launcherx" })

            // 4. Execute confirmed plan -> saves snapshot BEFORE applying, disables packages, verifies live state
            var persistedSnapshot: LocalSnapshotRecord? = null
            val execResult = engine.executeConfirmedPlan(
                transport = client,
                plan = plan,
                onPersistSnapshotBeforeApply = { snap -> persistedSnapshot = snap }
            )
            assertTrue("Expected Succeeded, got $execResult", execResult is BatchExecutionOutcome.Succeeded)
            assertNotNull(persistedSnapshot)
            assertFalse(server.isPackageCurrentlyEnabled("com.sony.dtv.promos"))
            assertFalse(server.isPackageCurrentlyEnabled("com.google.android.tungsten.setupwraith"))

            // Export reproducible ADB recovery commands without secrets
            val recoveryScript = persistedSnapshot!!.exportReproducibleAdbRecoveryCommands()
            assertTrue(recoveryScript.contains("adb shell pm enable --user 0 com.sony.dtv.promos"))

            // 5. Differential rollback restores exact prior enabled state
            val rollbackResult = engine.rollbackSnapshotOnTv(
                transport = client,
                snapshot = persistedSnapshot!!,
                activeHomePackage = report.currentHomePackage
            )
            assertTrue(rollbackResult is DifferentialRollbackOutcome.RolledBack)
            assertEquals(2, (rollbackResult as DifferentialRollbackOutcome.RolledBack).restoredCount)
            assertTrue(server.isPackageCurrentlyEnabled("com.sony.dtv.promos"))
            assertTrue(server.isPackageCurrentlyEnabled("com.google.android.tungsten.setupwraith"))

            // 6. Test live remote key, safe search text, screenshot PNG, and privacy report
            assertTrue(client.sendRemoteKeyEvent(TvRemoteKey.DPAD_CENTER).isSuccess)
            assertTrue(client.sendSafeSearchText("YouTube Kids").isSuccess)
            assertTrue(client.sendSafeSearchText("bad; rm -rf /").isFailure)

            val pngBytes = client.captureScreenshotPng().getOrThrow()
            assertEquals(FakeAdbServer.MINIMAL_VALID_PNG_BYTES.size, pngBytes.size)

            val privReport = TvPrivacyInspector().inspectConnectedTvPrivacy(client, report).getOrThrow()
            assertTrue(privReport.signals.any { it.certainty == SignalCertainty.VERIFIED_FACT })
            assertTrue(privReport.signals.any { it.certainty == SignalCertainty.NOT_VERIFIABLE_VIA_ADB })
            val exported = privReport.exportRedactedReport(AppLanguage.ES)
            assertFalse(exported.contains("127.0.0.1"))

            client.disconnect()
        }
    }

    @Test
    fun `SocketAdbTransportClient blocks any non-read-only or chained shell command with SecurityException`() = runBlocking {
        FakeAdbServer(mode = FakeAdbServer.ServerMode.ACCEPT_SIGNATURE_IMMEDIATELY).use { server ->
            server.start()
            val client = SocketAdbTransportClient(
                keyManager = KeystoreAdbKeyManager(context = null),
                allowLoopbackForTesting = true
            )
            assertTrue(client.connect("127.0.0.1", server.port) is AdbSessionOutcome.Connected)

            val forbiddenCommands = listOf(
                "pm disable-user --user 0 com.sony.dtv.promos",
                "pm uninstall com.sony.dtv.promos",
                "cmd package install /data/local/tmp/evil.apk",
                "getprop ro.product.model; rm -rf /data/local/tmp",
                "pm list packages -e && reboot"
            )
            for (cmd in forbiddenCommands) {
                val res = client.executeReadOnlyShell(cmd)
                assertTrue("Command '$cmd' must fail read-only guard", res.isFailure)
                assertTrue(res.exceptionOrNull() is SecurityException)
            }
            assertTrue(server.executedShellCommands.isEmpty())
            client.disconnect()
        }
    }

    @Test
    fun `SocketAdbTransportClient handles authorization denial, timeout and TLS-only channel closure cleanly`() = runBlocking {
        FakeAdbServer(mode = FakeAdbServer.ServerMode.DENY_AUTHORIZATION).use { server ->
            server.start()
            val client = SocketAdbTransportClient(KeystoreAdbKeyManager(null), allowLoopbackForTesting = true)
            val outcome = client.connect("127.0.0.1", server.port, connectTimeoutMs = 2_000, authTimeoutMs = 2_000)
            assertEquals(AdbErrorKind.PROTOCOL_ERROR, (outcome as AdbSessionOutcome.Failed).errorCode)
        }

        FakeAdbServer(mode = FakeAdbServer.ServerMode.HANG_FOR_TIMEOUT).use { server ->
            server.start()
            val client = SocketAdbTransportClient(KeystoreAdbKeyManager(null), allowLoopbackForTesting = true)
            val outcome = client.connect("127.0.0.1", server.port, connectTimeoutMs = 1_000, authTimeoutMs = 350)
            assertEquals(AdbErrorKind.AUTHORIZATION_PROMPT_TIMED_OUT, (outcome as AdbSessionOutcome.Failed).errorCode)
        }

        FakeAdbServer(mode = FakeAdbServer.ServerMode.CLOSE_IMMEDIATELY_FOR_TLS_ONLY).use { server ->
            server.start()
            val client = SocketAdbTransportClient(KeystoreAdbKeyManager(null), allowLoopbackForTesting = true)
            val outcome = client.connect("127.0.0.1", server.port, connectTimeoutMs = 2_000, authTimeoutMs = 2_000)
            assertEquals(AdbErrorKind.TLS_PAIRING_REQUIRED, (outcome as AdbSessionOutcome.Failed).errorCode)
        }
    }

    @Test
    fun `WirelessPairingValidator validates 6-digit PIN and LAN port`() {
        assertTrue(WirelessPairingValidator.validateInputs("192.168.1.50", "37123", "482910") is WirelessPairingValidator.PairingInputCheck.ValidFormat)
        assertTrue(WirelessPairingValidator.validateInputs("192.168.1.50", "37123", "12A4") is WirelessPairingValidator.PairingInputCheck.Invalid)
    }
}

package com.example

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.adb.FakeAdbServer
import com.example.core.adb.KeystoreAdbKeyManager
import com.example.core.adb.SocketAdbTransportClient
import com.example.core.packs.GitHubImportResult
import com.example.core.packs.GitHubPackImporter
import com.example.core.packs.PackTrustTier
import com.example.core.persistence.RoomPersistenceRepository
import com.example.core.persistence.TVeoCleanDatabase
import com.example.core.preferences.InMemoryUserPreferencesRepository
import com.example.core.preferences.UserPreferencesState
import com.example.ui.TVeoCleanApp
import com.example.ui.TVeoCleanViewModel
import com.example.ui.i18n.AppLanguage
import java.nio.file.Files
import kotlinx.coroutines.flow.first
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app_name matches TVeoClean brand identity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TVeoClean", appName)
    }

    @Test
    fun `Home omits protected count badge and Settings displays Engineer Rosmer Santaella signature card in Spanish and English`() {
        val repo = InMemoryUserPreferencesRepository(
            UserPreferencesState(onboardingCompleted = true, reduceMotion = true)
        )
        val viewModel = TVeoCleanViewModel(repo)

        composeTestRule.setContent {
            TVeoCleanApp(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        assertEquals(AppLanguage.ES, viewModel.uiState.value.preferences.language)
        composeTestRule.onNodeWithText("Centro de control seguro para tu televisor").assertIsDisplayed()

        // Ensure "17 paquetes críticos protegidos" is completely removed from Home UX
        composeTestRule.onAllNodesWithText("paquetes críticos protegidos", substring = true)
            .assertCountEquals(0)

        // Navigate to Ajustes and verify Engineer Rosmer Santaella signature card + GitHub button
        composeTestRule.onNodeWithTag("nav_item_settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("developer_credits_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ingeniero de Sistemas Rosmer Santaella").assertIsDisplayed()
        composeTestRule.onNodeWithTag("developer_github_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("github.com/ROSNLR5", substring = true).assertIsDisplayed()

        // Scroll to language selector and switch to English
        composeTestRule.onNodeWithTag("settings_lazy_list")
            .performScrollToNode(hasTestTag("language_selector_card"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("lang_option_en").performClick()
        composeTestRule.waitForIdle()

        assertEquals(AppLanguage.EN, viewModel.uiState.value.preferences.language)
        composeTestRule.onNodeWithTag("settings_lazy_list")
            .performScrollToNode(hasTestTag("developer_credits_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Systems Engineer Rosmer Santaella").assertIsDisplayed()

        // Switch back to Spanish
        composeTestRule.onNodeWithTag("settings_lazy_list")
            .performScrollToNode(hasTestTag("language_selector_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lang_option_es").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AppLanguage.ES, viewModel.uiState.value.preferences.language)
    }

    @Test
    fun `friendly 3-step onboarding supports forward and back navigation and Style Studio renders visual catalog thumbnails`() {
        val repo = InMemoryUserPreferencesRepository(
            UserPreferencesState(onboardingCompleted = false, reduceMotion = true)
        )
        val viewModel = TVeoCleanViewModel(repo)

        composeTestRule.setContent {
            TVeoCleanApp(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Step 1 -> Step 2 -> Back to Step 1 -> Step 2 -> Step 3 -> Finish
        composeTestRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conecta tu TV en segundos").assertIsDisplayed()

        composeTestRule.onNodeWithTag("onboarding_primary_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Limpia apps sin riesgo").assertIsDisplayed()

        composeTestRule.onNodeWithTag("onboarding_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Conecta tu TV en segundos").assertIsDisplayed()

        composeTestRule.onNodeWithTag("onboarding_primary_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_primary_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Personaliza y vuelve atrás cuando quieras").assertIsDisplayed()

        composeTestRule.onNodeWithTag("onboarding_primary_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("screen_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()

        // Mi TV defaults to Automatic Wi-Fi Network Scanner mode
        composeTestRule.onNodeWithTag("nav_item_my_tv").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("screen_my_tv").assertIsDisplayed()
        composeTestRule.onNodeWithTag("my_tv_lazy_list")
            .performScrollToNode(hasTestTag("auto_network_scanner_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("start_network_scan_button")
            .performScrollTo()
            .assertIsDisplayed()

        // Style Studio displays both 16:9 frame preview AND visual thumbnails inside catalog cards
        composeTestRule.onNodeWithTag("nav_item_studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("screen_studio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tv_frame_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("studio_lazy_list")
            .performScrollToNode(hasTestTag("pack_card_tveoclean.official.brisa_nocturna"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pack_card_tveoclean.official.brisa_nocturna")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(
            "pack_thumbnail_tveoclean.official.brisa_nocturna",
            useUnmergedTree = true
        ).assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_item_history").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("screen_history").assertIsDisplayed()
    }

    @Test
    fun `GitHubPackImporter pins branch to 40-char Commit SHA, converts legacy config_json and blocks malicious scripts`() = runBlocking {
        val pinnedSha = "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678"
        val legacyConfigJson = """
            {
              "name": "Nebula Minimal",
              "author": "CommunityCreator",
              "background": "#0E1726",
              "surface": "#17253B",
              "accent": "#38BDF8",
              "columns": 5,
              "fillDock": true,
              "unknownShellHook": "sh -c evil"
            }
        """.trimIndent()

        val mockGitHubClient = OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.toString()
                val (code, body) = when {
                    url == "https://api.github.com/repos/CommunityCreator/tv-themes/commits/main" ->
                        200 to """{"sha": "$pinnedSha"}"""
                    url == "https://api.github.com/repos/CommunityCreator/tv-themes/contents/nebula?ref=$pinnedSha" ->
                        200 to """[{"name": "config.json", "path": "nebula/config.json", "type": "file", "size": ${legacyConfigJson.length}}]"""
                    url == "https://raw.githubusercontent.com/CommunityCreator/tv-themes/$pinnedSha/nebula/config.json" ->
                        200 to legacyConfigJson

                    url == "https://api.github.com/repos/EvilUser/bad-pack/commits/main" ->
                        200 to """{"sha": "$pinnedSha"}"""
                    url == "https://api.github.com/repos/EvilUser/bad-pack/contents?ref=$pinnedSha" ->
                        200 to """[{"name": "payload.sh", "path": "payload.sh", "type": "file", "size": 42}]"""

                    else -> 404 to """{"message": "Not Found"}"""
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Not Found")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            })
            .build()

        val tempDir = Files.createTempDirectory("tveo_import_test").toFile()
        try {
            val importer = GitHubPackImporter(
                baseStorageDir = tempDir,
                httpClient = mockGitHubClient
            )

            // 1. Valid legacy Clean Your TV theme pinned to 40-char SHA, unknown key omitted, marked UNVERIFIED_LINK
            val res = importer.importFromUrl("https://github.com/CommunityCreator/tv-themes/tree/main/nebula")
            assertTrue("Expected Imported, got $res", res is GitHubImportResult.Imported)
            val importedPack = (res as GitHubImportResult.Imported).pack
            assertEquals(pinnedSha, importedPack.pinnedCommitSha)
            assertEquals(PackTrustTier.UNVERIFIED_LINK, importedPack.trustTier)
            assertTrue(importedPack.isLegacyConverted)
            assertTrue(importedPack.omittedLegacyKeys.contains("unknownShellHook"))

            // 2. Malicious repo with executable .sh script must be unconditionally rejected
            val badRes = importer.importFromUrl("https://github.com/EvilUser/bad-pack")
            assertTrue("Expected Rejected for .sh file, got $badRes", badRes is GitHubImportResult.Rejected)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `full production CUJ connects to FakeAdbServer, executes confirmed reversible cleanup with Room SQLite snapshot and rolls back`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TVeoCleanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val roomRepo = RoomPersistenceRepository(db.dao())

        try {
            FakeAdbServer(mode = FakeAdbServer.ServerMode.ACCEPT_AFTER_PUBLIC_KEY).use { server ->
                server.start()
                val prefsRepo = InMemoryUserPreferencesRepository(
                    UserPreferencesState(onboardingCompleted = true, reduceMotion = true)
                )
                val keyManager = KeystoreAdbKeyManager(context = null)
                val transport = SocketAdbTransportClient(
                    keyManager = keyManager,
                    allowLoopbackForTesting = true
                )
                val viewModel = TVeoCleanViewModel(
                    preferencesRepository = prefsRepo,
                    keyManager = keyManager,
                    adbTransport = transport,
                    persistenceRepository = roomRepo
                )

                composeTestRule.setContent {
                    TVeoCleanApp(viewModel = viewModel)
                }
                val initDeadline = System.currentTimeMillis() + 3_000L
                while (!viewModel.uiState.value.preferences.onboardingCompleted && System.currentTimeMillis() < initDeadline) {
                    ShadowLooper.idleMainLooper()
                    Thread.sleep(15)
                }
                ShadowLooper.idleMainLooper()
                composeTestRule.waitForIdle()

                // Navigate to Mi TV and enter manual host -> switches to Manual mode & connects
                composeTestRule.onNodeWithTag("nav_item_my_tv").performClick()
                composeTestRule.waitForIdle()

                viewModel.updateManualHost("127.0.0.1")
                viewModel.updateManualPort(server.port.toString())
                composeTestRule.waitForIdle()

                composeTestRule.onNodeWithTag("connect_adb_button")
                    .performScrollTo()
                    .performClick()

                val connectDeadline = System.currentTimeMillis() + 5_000L
                while (viewModel.uiState.value.latestReadOnlyReport == null && System.currentTimeMillis() < connectDeadline) {
                    ShadowLooper.idleMainLooper()
                    Thread.sleep(20)
                }
                ShadowLooper.idleMainLooper()
                composeTestRule.waitForIdle()

                assertTrue(viewModel.uiState.value.isAdbConnected)
                assertNotNull(viewModel.uiState.value.latestReadOnlyReport)
                assertTrue(server.isPackageCurrentlyEnabled("com.sony.dtv.promos"))

                // Stage safe recommended packages -> open confirmation modal -> execute batch reversible plan!
                viewModel.selectAllSafeRecommendations()
                composeTestRule.waitForIdle()
                assertTrue(viewModel.uiState.value.selectedPackagesForPlan.contains("com.sony.dtv.promos"))

                viewModel.openConfirmPlanDialog()
                composeTestRule.waitForIdle()
                composeTestRule.onNodeWithTag("confirm_plan_dialog").assertIsDisplayed()

                composeTestRule.onNodeWithTag("confirm_execute_plan_button").performClick()

                val applyDeadline = System.currentTimeMillis() + 5_000L
                while (server.isPackageCurrentlyEnabled("com.sony.dtv.promos") && System.currentTimeMillis() < applyDeadline) {
                    ShadowLooper.idleMainLooper()
                    Thread.sleep(20)
                }
                ShadowLooper.idleMainLooper()
                composeTestRule.waitForIdle()

                // Verify package disabled on FakeAdbServer AND snapshot persisted in Room SQLite!
                assertFalse(server.isPackageCurrentlyEnabled("com.sony.dtv.promos"))
                val roomSnapshots = runBlocking { roomRepo.snapshotsFlow.first() }
                assertEquals(1, roomSnapshots.size)

                // Verify last remaining recovery snapshot is protected from deletion
                val deletedLast = runBlocking { roomRepo.deleteSnapshotSafely(roomSnapshots.first().id) }
                assertFalse("Last recovery snapshot must never be deleted", deletedLast)

                // Revert from Room snapshot and verify package is re-enabled on FakeAdbServer
                viewModel.revertFromSnapshot(roomSnapshots.first())
                val revertDeadline = System.currentTimeMillis() + 5_000L
                while (!server.isPackageCurrentlyEnabled("com.sony.dtv.promos") && System.currentTimeMillis() < revertDeadline) {
                    ShadowLooper.idleMainLooper()
                    Thread.sleep(20)
                }
                ShadowLooper.idleMainLooper()
                composeTestRule.waitForIdle()

                assertTrue(server.isPackageCurrentlyEnabled("com.sony.dtv.promos"))
            }
        } finally {
            db.close()
        }
    }
}

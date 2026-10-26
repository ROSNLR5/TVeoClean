package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.adb.ActiveLanSubnetScanner
import com.example.core.adb.AndroidNsdTvDiscoveryScanner
import com.example.core.adb.KeystoreAdbKeyManager
import com.example.core.adb.SocketAdbTransportClient
import com.example.core.packs.GitHubPackImporter
import com.example.core.persistence.RoomPersistenceRepository
import com.example.core.persistence.TVeoCleanDatabase
import com.example.core.preferences.DataStoreUserPreferencesRepository
import com.example.core.update.GitHubAppUpdateManager
import com.example.ui.TVeoCleanApp
import com.example.ui.TVeoCleanViewModel
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appCtx = applicationContext
        val preferencesRepository = DataStoreUserPreferencesRepository(appCtx)
        val keyManager = KeystoreAdbKeyManager(appCtx)
        val adbTransport = SocketAdbTransportClient(keyManager)
        val nsdScanner = AndroidNsdTvDiscoveryScanner(appCtx)
        val activeSubnetScanner = ActiveLanSubnetScanner(appCtx)
        val database = TVeoCleanDatabase.getInstance(appCtx)
        val persistenceRepository = RoomPersistenceRepository(database.dao())
        val gitHubImporter = GitHubPackImporter(
            baseStorageDir = File(appCtx.filesDir, "imported_packs")
        )
        val screenshotDir = File(appCtx.filesDir, "private_screenshots")
        val appUpdateManager = GitHubAppUpdateManager(context = appCtx)

        setContent {
            val factory = remember {
                TVeoCleanViewModel.factory(
                    repository = preferencesRepository,
                    keyManager = keyManager,
                    adbTransport = adbTransport,
                    discoveryScanner = nsdScanner,
                    activeNetworkScanner = activeSubnetScanner,
                    persistenceRepository = persistenceRepository,
                    gitHubPackImporter = gitHubImporter,
                    screenshotStorageDir = screenshotDir,
                    appUpdateRepository = appUpdateManager,
                    autoCheckUpdatesOnLaunch = true
                )
            }
            val viewModel: TVeoCleanViewModel = viewModel(factory = factory)
            TVeoCleanApp(viewModel = viewModel)
        }
    }
}

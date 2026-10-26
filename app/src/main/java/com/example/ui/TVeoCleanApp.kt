package com.example.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.feature.connect.MyTvConnectScreen
import com.example.feature.home.HomeScreen
import com.example.feature.home.OnboardingOverlay
import com.example.feature.remote.TvPrivacyReportDialog
import com.example.feature.remote.TvRemoteControlDialog
import com.example.feature.restore.HistoryRestoreScreen
import com.example.feature.settings.AppUpdateDialog
import com.example.feature.settings.SettingsAboutScreen
import com.example.feature.studio.StyleStudioScreen
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.TVeoStrings
import com.example.ui.i18n.stringsForLanguage
import com.example.ui.theme.LocalTVeoExtendedColors
import com.example.ui.theme.TVeoCleanTheme

object TVeoRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_MY_TV = "my_tv"
    const val ROUTE_STUDIO = "style_studio"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SETTINGS = "settings"
}

private data class TopLevelDestination(
    val route: String,
    val labelSelector: (TVeoStrings) -> String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = TVeoRoutes.ROUTE_HOME,
        labelSelector = { it.navHome },
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_item_home"
    ),
    TopLevelDestination(
        route = TVeoRoutes.ROUTE_MY_TV,
        labelSelector = { it.navMyTv },
        selectedIcon = Icons.Filled.Tv,
        unselectedIcon = Icons.Outlined.Tv,
        testTag = "nav_item_my_tv"
    ),
    TopLevelDestination(
        route = TVeoRoutes.ROUTE_STUDIO,
        labelSelector = { it.navStudio },
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_item_studio"
    ),
    TopLevelDestination(
        route = TVeoRoutes.ROUTE_HISTORY,
        labelSelector = { it.navHistory },
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_item_history"
    ),
    TopLevelDestination(
        route = TVeoRoutes.ROUTE_SETTINGS,
        labelSelector = { it.navSettings },
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_item_settings"
    )
)

@Composable
fun TVeoCleanApp(
    viewModel: TVeoCleanViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appStrings = remember(uiState.preferences.language) {
        stringsForLanguage(uiState.preferences.language)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshInstallPermissionState()
        onPauseOrDispose { }
    }

    CompositionLocalProvider(LocalAppStrings provides appStrings) {
        TVeoCleanTheme(
            themeMode = uiState.preferences.themeMode,
            userReduceMotion = uiState.preferences.reduceMotion,
            mobileAccentOverride = uiState.activeMobileOverride
        ) {
            val reduceMotion = LocalTVeoExtendedColors.current.reduceMotion
            var onboardingStep by rememberSaveable { mutableIntStateOf(0) }

            if (!uiState.preferences.onboardingCompleted) {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    OnboardingOverlay(
                        stepIndex = onboardingStep,
                        onSelectStep = { onboardingStep = it.coerceIn(0, 2) },
                        onPreviousStep = { onboardingStep = (onboardingStep - 1).coerceAtLeast(0) },
                        onNextStep = { onboardingStep = (onboardingStep + 1).coerceAtMost(2) },
                        onSkipOrFinish = {
                            onboardingStep = 0
                            viewModel.completeOnboarding()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                return@TVeoCleanTheme
            }

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: TVeoRoutes.ROUTE_HOME
            val snackbarHostState = remember { SnackbarHostState() }
            val activeBanner = uiState.localizedBanner()

            LaunchedEffect(activeBanner) {
                if (activeBanner != null) {
                    snackbarHostState.showSnackbar(activeBanner)
                    viewModel.dismissBanner()
                }
            }

            fun navigateTopLevel(route: String) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavigationRail) {
                            Column {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                )
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp
                                ) {
                                    topLevelDestinations.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        val label = dest.labelSelector(appStrings)
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(dest.route) },
                                            alwaysShowLabel = true,
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = label
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = label,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.testTag(dest.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                topLevelDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    val label = dest.labelSelector(appStrings)
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                            VerticalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        }

                        NavHost(
                            navController = navController,
                            startDestination = TVeoRoutes.ROUTE_HOME,
                            enterTransition = {
                                if (reduceMotion) EnterTransition.None else fadeIn(tween(180))
                            },
                            exitTransition = {
                                if (reduceMotion) ExitTransition.None else fadeOut(tween(140))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(TVeoRoutes.ROUTE_HOME) {
                                HomeScreen(
                                    uiState = uiState,
                                    onNavigateToConnect = { navigateTopLevel(TVeoRoutes.ROUTE_MY_TV) },
                                    onNavigateToStudio = { navigateTopLevel(TVeoRoutes.ROUTE_STUDIO) },
                                    onNavigateToHistory = { navigateTopLevel(TVeoRoutes.ROUTE_HISTORY) },
                                    onOpenRemoteControl = viewModel::openRemoteDialog,
                                    onOpenUpdateDialog = viewModel::openAppUpdateDialog,
                                    onDismissUpdateBanner = viewModel::dismissHomeUpdateBanner
                                )
                            }
                            composable(TVeoRoutes.ROUTE_MY_TV) {
                                MyTvConnectScreen(
                                    uiState = uiState,
                                    onSelectConnectionMode = viewModel::setConnectionDiscoveryMode,
                                    onSubnetPrefixChange = viewModel::updateCustomSubnetPrefix,
                                    onStartNetworkScan = viewModel::startAutomaticNetworkScan,
                                    onStopNetworkScan = viewModel::stopAutomaticNetworkScan,
                                    onConnectScannedDevice = viewModel::selectAndConnectScannedDevice,
                                    onConnectSavedDevice = viewModel::connectSavedTvDevice,
                                    onHostChange = viewModel::updateManualHost,
                                    onPortChange = viewModel::updateManualPort,
                                    onSelectDiscoveredTv = viewModel::selectDiscoveredTv,
                                    onConnectAndInspect = viewModel::connectAndInspectReadOnly,
                                    onCancelConnection = viewModel::cancelActiveConnectionAttempt,
                                    onDisconnectAdb = viewModel::disconnectAdbSession,
                                    onReRunReadOnlyInspection = viewModel::runReadOnlyInspection,
                                    onOpenRemoteTools = viewModel::openRemoteDialog,
                                    onOpenPrivacyReport = viewModel::openAndInspectPrivacyReport,
                                    onSelectReportFilter = viewModel::selectReportFilter,
                                    onTogglePackageForPlan = viewModel::togglePackageSelectionForPlan,
                                    onSelectRecommendedPackages = viewModel::selectAllSafeRecommendations,
                                    onClearPlanSelection = viewModel::clearPlanSelection,
                                    onOpenConfirmPlanDialog = viewModel::openConfirmPlanDialog,
                                    onDismissConfirmPlanDialog = viewModel::dismissConfirmPlanDialog,
                                    onExecuteConfirmedPlan = viewModel::executeConfirmedReversiblePlan,
                                    onRevokeAdbKey = viewModel::revokeAndRotateAdbKey,
                                    onOpenPairingGuide = viewModel::openPairingGuide,
                                    onClosePairingGuide = viewModel::closePairingGuide,
                                    onUpdatePairingPort = viewModel::updatePairingPort,
                                    onUpdatePairingPin = viewModel::updatePairingPin,
                                    onCheckPairingInputs = viewModel::checkWirelessPairingInputs
                                )
                            }
                            composable(TVeoRoutes.ROUTE_STUDIO) {
                                StyleStudioScreen(
                                    uiState = uiState,
                                    onSearchChange = viewModel::updateStudioSearch,
                                    onSelectTab = viewModel::selectStudioTab,
                                    onSelectPack = viewModel::selectPack,
                                    onToggleUnverifiedConsent = viewModel::setUnverifiedPackConsent,
                                    onToggleCompareProposed = viewModel::setCompareProposed,
                                    onApplyPack = { pack ->
                                        viewModel.applyMobileUiPackWithSnapshot(
                                            pack = pack,
                                            onNavigateToMyTv = { navigateTopLevel(TVeoRoutes.ROUTE_MY_TV) }
                                        )
                                    },
                                    onOpenGitHubImport = viewModel::openGitHubImportSheet,
                                    onCloseGitHubImport = viewModel::closeGitHubImportSheet,
                                    onUpdateGitHubUrl = viewModel::updateGitHubImportUrl,
                                    onVerifyGitHubUrl = viewModel::verifyGitHubUrlStructure,
                                    onImportFromGitHubLive = viewModel::importPackFromGitHubLive
                                )
                            }
                            composable(TVeoRoutes.ROUTE_HISTORY) {
                                HistoryRestoreScreen(
                                    uiState = uiState,
                                    onRevertSnapshot = viewModel::revertFromSnapshot,
                                    onDeleteSnapshot = viewModel::deleteSnapshotRecord,
                                    onCopiedNotice = viewModel::notifyCopiedMessage
                                )
                            }
                            composable(TVeoRoutes.ROUTE_SETTINGS) {
                                SettingsAboutScreen(
                                    uiState = uiState,
                                    onSelectLanguage = viewModel::setLanguage,
                                    onSelectThemeMode = viewModel::setThemeMode,
                                    onToggleReduceMotion = viewModel::setReduceMotion,
                                    onToggleAdvancedAdb = viewModel::setShowAdvancedAdbDetails,
                                    onReopenOnboarding = viewModel::reopenOnboarding,
                                    onCheckForUpdates = { viewModel.checkForAppUpdates(silentOnNoNewRelease = false) },
                                    onOpenUpdateDialog = viewModel::openAppUpdateDialog,
                                    onCopiedNotice = viewModel::notifyCopiedMessage
                                )
                            }
                        }
                    }
                }

                // Official GitHub Releases APK Update Modal
                if (uiState.isUpdateDialogOpen) {
                    AppUpdateDialog(
                        updateState = uiState.appUpdateState,
                        language = uiState.preferences.language,
                        apkConsentChecked = uiState.apkInstallConsentChecked,
                        showTechnicalHashes = uiState.preferences.showAdvancedAdbDetails,
                        onToggleConsent = viewModel::setApkInstallConsent,
                        onStartDownload = viewModel::downloadLatestAppUpdate,
                        onCancelDownload = viewModel::cancelAppUpdateDownload,
                        onGrantInstallPermission = viewModel::requestUnknownSourcesPermission,
                        onInstallVerifiedApk = viewModel::installVerifiedAppUpdate,
                        onDismiss = viewModel::closeAppUpdateDialog
                    )
                }

                // Live ADB Remote Control & Screenshot Modal
                if (uiState.isRemoteDialogOpen) {
                    TvRemoteControlDialog(
                        isConnected = uiState.isAdbConnected,
                        latestScreenshotPath = uiState.latestScreenshotPath,
                        isCapturingScreenshot = uiState.isCapturingScreenshot,
                        onSendKey = viewModel::sendTvRemoteKey,
                        onSendSearchText = viewModel::sendTvSearchText,
                        onCaptureScreenshot = viewModel::capturePrivateTvScreenshot,
                        onGoToConnect = { navigateTopLevel(TVeoRoutes.ROUTE_MY_TV) },
                        onDismiss = viewModel::closeRemoteDialog
                    )
                }

                // Evidence-Based Privacy Report Modal
                if (uiState.activePrivacyReport != null) {
                    TvPrivacyReportDialog(
                        report = uiState.activePrivacyReport,
                        language = uiState.preferences.language,
                        onRefresh = viewModel::openAndInspectPrivacyReport,
                        onCopiedNotice = viewModel::notifyPrivacyCopied,
                        onDismiss = viewModel::closePrivacyReportDialog
                    )
                }
            }
        }
    }
}

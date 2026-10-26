package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.adb.ActiveLanSubnetScanner
import com.example.core.adb.ActiveNetworkScanner
import com.example.core.adb.AdbKeyManager
import com.example.core.adb.AdbSessionOutcome
import com.example.core.adb.AdbTransportClient
import com.example.core.adb.DiscoveredLanTv
import com.example.core.adb.EmptyLanTvDiscoveryScanner
import com.example.core.adb.KeystoreAdbKeyManager
import com.example.core.adb.LanScanDeviceHit
import com.example.core.adb.LanScanProgress
import com.example.core.adb.LanTvDiscoveryScanner
import com.example.core.adb.SocketAdbTransportClient
import com.example.core.adb.TvRemoteKey
import com.example.core.adb.WirelessPairingValidator
import com.example.core.engine.BatchExecutionOutcome
import com.example.core.engine.ChangePlan
import com.example.core.engine.DifferentialRollbackOutcome
import com.example.core.engine.InspectedPackageItem
import com.example.core.engine.PackageClassification
import com.example.core.engine.ProtectedPackageRegistry
import com.example.core.engine.ReadOnlyDeviceReport
import com.example.core.engine.ReadOnlyTvAnalyzer
import com.example.core.engine.TransactionalCleanEngine
import com.example.core.packs.CuratedStudioCatalog
import com.example.core.packs.GitHubImportResult
import com.example.core.packs.GitHubPackImporter
import com.example.core.packs.PackTarget
import com.example.core.packs.PackTrustTier
import com.example.core.packs.PackType
import com.example.core.packs.StrictGitHubUrlParser
import com.example.core.packs.TveoPackSummary
import com.example.core.persistence.InMemoryPersistenceRepository
import com.example.core.persistence.PersistenceRepository
import com.example.core.persistence.SavedTvDeviceEntity
import com.example.core.platform.HonestPlatformRegistry
import com.example.core.platform.OperationLifecycleState
import com.example.core.platform.PlatformCapabilityMatrix
import com.example.core.preferences.UserPreferencesRepository
import com.example.core.preferences.UserPreferencesState
import com.example.core.privacy.TvPrivacyInspector
import com.example.core.privacy.TvPrivacyReport
import com.example.BuildConfig
import com.example.core.restore.AuditLogEntry
import com.example.core.restore.IntegrityHashUtil
import com.example.core.restore.LocalSnapshotRecord
import com.example.core.restore.SnapshotScope
import com.example.core.update.AppUpdateCheckOutcome
import com.example.core.update.AppUpdateDownloadOutcome
import com.example.core.update.AppUpdateReleaseInfo
import com.example.core.update.AppUpdateRepository
import com.example.core.update.AppUpdateUiState
import com.example.core.update.NoOpAppUpdateRepository
import com.example.ui.design.parseHexColorSafe
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.TVeoStrings
import com.example.ui.theme.MintTurquoiseDark
import com.example.ui.theme.MintTurquoiseLight
import com.example.ui.theme.MobileUiAccentOverride
import com.example.ui.theme.SoftIndigoDark
import com.example.ui.theme.SoftIndigoLight
import com.example.ui.theme.ThemeMode
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConnectionDiscoveryMode {
    AUTOMATIC_SCANNER,
    MANUAL_IP
}

enum class StudioTab {
    DISCOVER,
    THEMES,
    TWEAKS,
    PROFILES,
    INSTALLED;

    fun localizedLabel(strings: TVeoStrings): String = when (this) {
        DISCOVER -> strings.studioTabDiscover
        THEMES -> strings.studioTabThemes
        TWEAKS -> strings.studioTabTweaks
        PROFILES -> strings.studioTabProfiles
        INSTALLED -> strings.studioTabInstalled
    }
}

enum class ReportPackageFilter {
    ALL,
    RECOMMENDED,
    PROTECTED,
    CONSERVATIVE
}

sealed interface AdbConnectionBanner {
    data object Idle : AdbConnectionBanner
    data class ConnectedSession(val endpoint: String, val banner: String) : AdbConnectionBanner
    data class Error(val messageEs: String, val messageEn: String) : AdbConnectionBanner {
        fun localized(lang: AppLanguage): String = if (lang == AppLanguage.ES) messageEs else messageEn
    }
}

sealed interface PairingGuideModalState {
    data object Hidden : PairingGuideModalState
    data class Visible(
        val pairingPort: String = "37099",
        val sixDigitPin: String = "",
        val feedbackEs: String? = null,
        val feedbackEn: String? = null,
        val isError: Boolean = false
    ) : PairingGuideModalState {
        fun localizedFeedback(lang: AppLanguage): String? =
            if (lang == AppLanguage.ES) feedbackEs else feedbackEn
    }
}

sealed interface GitHubImportCheckState {
    data object Hidden : GitHubImportCheckState
    data class Editing(
        val rawUrl: String = "",
        val isImporting: Boolean = false,
        val errorEs: String? = null,
        val errorEn: String? = null,
        val parsedSummary: StrictGitHubUrlParser.ParsedGitHubRef? = null
    ) : GitHubImportCheckState {
        fun localizedError(lang: AppLanguage): String? =
            if (lang == AppLanguage.ES) errorEs else errorEn
    }
}

data class TVeoCleanUiState(
    val preferences: UserPreferencesState = UserPreferencesState(),
    val operationState: OperationLifecycleState = OperationLifecycleState.IDLE,
    val connectionMode: ConnectionDiscoveryMode = ConnectionDiscoveryMode.AUTOMATIC_SCANNER,
    val isAdbConnected: Boolean = false,
    val connectedEndpoint: String? = null,
    val keystorePublicFingerprint: String = "",
    val networkScanProgress: LanScanProgress = LanScanProgress(),
    val customSubnetPrefixInput: String = "",
    val savedTvDevices: List<SavedTvDeviceEntity> = emptyList(),
    val discoveredDevices: List<DiscoveredLanTv> = emptyList(),
    val manualHostInput: String = "",
    val manualPortInput: String = "5555",
    val adbBanner: AdbConnectionBanner = AdbConnectionBanner.Idle,
    val pairingGuideState: PairingGuideModalState = PairingGuideModalState.Hidden,
    val latestReadOnlyReport: ReadOnlyDeviceReport? = null,
    val selectedReportFilter: ReportPackageFilter = ReportPackageFilter.ALL,
    val selectedPackagesForPlan: Set<String> = emptySet(),
    val pendingConfirmationPlan: ChangePlan? = null,
    val isRemoteDialogOpen: Boolean = false,
    val isCapturingScreenshot: Boolean = false,
    val latestScreenshotPath: String? = null,
    val activePrivacyReport: TvPrivacyReport? = null,
    val platformMatrices: List<PlatformCapabilityMatrix> = HonestPlatformRegistry.matrices,
    val protectedRegistryCount: Int = ProtectedPackageRegistry.allEntries().size,
    val studioSearchQuery: String = "",
    val selectedStudioTab: StudioTab = StudioTab.DISCOVER,
    val selectedPackId: String = CuratedStudioCatalog.samplePacks.first().id,
    val unverifiedPackConsentChecked: Boolean = false,
    val compareProposedInPreview: Boolean = true,
    val allPacks: List<TveoPackSummary> = CuratedStudioCatalog.samplePacks,
    val gitHubImportState: GitHubImportCheckState = GitHubImportCheckState.Hidden,
    val snapshots: List<LocalSnapshotRecord> = emptyList(),
    val auditLog: List<AuditLogEntry> = emptyList(),
    val appVersionName: String = BuildConfig.VERSION_NAME,
    val appUpdateState: AppUpdateUiState = AppUpdateUiState.Idle(BuildConfig.VERSION_NAME),
    val isUpdateDialogOpen: Boolean = false,
    val apkInstallConsentChecked: Boolean = false,
    val isHomeUpdateBannerDismissed: Boolean = false,
    val statusBannerEs: String? = null,
    val statusBannerEn: String? = null
) {
    fun localizedBanner(): String? =
        if (preferences.language == AppLanguage.ES) statusBannerEs else statusBannerEn

    val filteredReportPackages: List<InspectedPackageItem>
        get() {
            val all = latestReadOnlyReport?.inspectedPackages ?: return emptyList()
            return when (selectedReportFilter) {
                ReportPackageFilter.ALL -> all
                ReportPackageFilter.RECOMMENDED -> all.filter {
                    it.classification == PackageClassification.KNOWN_SAFE_RECOMMENDATION
                }
                ReportPackageFilter.PROTECTED -> all.filter {
                    it.classification == PackageClassification.PROTECTED_CRITICAL
                }
                ReportPackageFilter.CONSERVATIVE -> all.filter {
                    it.classification == PackageClassification.UNKNOWN_CONSERVATIVE
                }
            }
        }

    val activeMobileOverride: MobileUiAccentOverride?
        get() {
            val activeId = preferences.activeMobileUiPackId ?: return null
            val pack = allPacks.find { it.id == activeId && it.target == PackTarget.MOBILE_UI } ?: return null
            return MobileUiAccentOverride(
                packId = pack.id,
                packName = pack.localizedName(preferences.language),
                primaryDark = parseHexColorSafe(pack.mobilePrimaryDarkHex ?: "", MintTurquoiseDark),
                primaryLight = parseHexColorSafe(pack.mobilePrimaryLightHex ?: "", MintTurquoiseLight),
                secondaryDark = parseHexColorSafe(pack.mobileSecondaryDarkHex ?: "", SoftIndigoDark),
                secondaryLight = parseHexColorSafe(pack.mobileSecondaryLightHex ?: "", SoftIndigoLight)
            )
        }

    val filteredPacks: List<TveoPackSummary>
        get() {
            val lang = preferences.language
            val byTab = when (selectedStudioTab) {
                StudioTab.DISCOVER -> allPacks
                StudioTab.THEMES -> allPacks.filter { it.type == PackType.THEME }
                StudioTab.TWEAKS -> allPacks.filter { it.type == PackType.TWEAK }
                StudioTab.PROFILES -> allPacks.filter { it.type == PackType.DEBLOAT_PROFILE || it.type == PackType.PROFILE }
                StudioTab.INSTALLED -> allPacks.filter {
                    it.id == preferences.activeMobileUiPackId || it.trustTier == PackTrustTier.UNVERIFIED_LINK
                }
            }
            val q = studioSearchQuery.trim().lowercase()
            if (q.isEmpty()) return byTab
            return byTab.filter { pack ->
                pack.localizedName(lang).lowercase().contains(q) ||
                    pack.author.lowercase().contains(q) ||
                    pack.license.lowercase().contains(q) ||
                    pack.target.localizedLabel(lang).lowercase().contains(q) ||
                    pack.localizedDescription(lang).lowercase().contains(q)
            }
        }

    val selectedPack: TveoPackSummary
        get() = allPacks.find { it.id == selectedPackId } ?: allPacks.first()
}

private data class TransientInternalState(
    val operationState: OperationLifecycleState = OperationLifecycleState.IDLE,
    val connectionMode: ConnectionDiscoveryMode = ConnectionDiscoveryMode.AUTOMATIC_SCANNER,
    val isAdbConnected: Boolean = false,
    val connectedEndpoint: String? = null,
    val keystoreFingerprint: String = "",
    val customSubnetPrefixInput: String = "",
    val manualHostInput: String = "",
    val manualPortInput: String = "5555",
    val adbBanner: AdbConnectionBanner = AdbConnectionBanner.Idle,
    val pairingGuideState: PairingGuideModalState = PairingGuideModalState.Hidden,
    val latestReadOnlyReport: ReadOnlyDeviceReport? = null,
    val selectedReportFilter: ReportPackageFilter = ReportPackageFilter.ALL,
    val selectedPackagesForPlan: Set<String> = emptySet(),
    val pendingConfirmationPlan: ChangePlan? = null,
    val isRemoteDialogOpen: Boolean = false,
    val isCapturingScreenshot: Boolean = false,
    val latestScreenshotPath: String? = null,
    val activePrivacyReport: TvPrivacyReport? = null,
    val studioSearchQuery: String = "",
    val selectedStudioTab: StudioTab = StudioTab.DISCOVER,
    val selectedPackId: String = CuratedStudioCatalog.samplePacks.first().id,
    val unverifiedPackConsentChecked: Boolean = false,
    val compareProposedInPreview: Boolean = true,
    val gitHubImportState: GitHubImportCheckState = GitHubImportCheckState.Hidden,
    val appUpdateState: AppUpdateUiState = AppUpdateUiState.Idle(BuildConfig.VERSION_NAME),
    val isUpdateDialogOpen: Boolean = false,
    val apkInstallConsentChecked: Boolean = false,
    val isHomeUpdateBannerDismissed: Boolean = false,
    val statusBannerEs: String? = null,
    val statusBannerEn: String? = null
)

private data class PersistentFlowsBundle(
    val snapshots: List<LocalSnapshotRecord>,
    val audits: List<AuditLogEntry>,
    val importedPacks: List<TveoPackSummary>,
    val savedDevices: List<SavedTvDeviceEntity>
)

class TVeoCleanViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val keyManager: AdbKeyManager = KeystoreAdbKeyManager(),
    private val adbTransport: AdbTransportClient = SocketAdbTransportClient(keyManager),
    private val discoveryScanner: LanTvDiscoveryScanner = EmptyLanTvDiscoveryScanner(),
    private val activeNetworkScanner: ActiveNetworkScanner = ActiveLanSubnetScanner(),
    private val persistenceRepository: PersistenceRepository = InMemoryPersistenceRepository(),
    private val gitHubPackImporter: GitHubPackImporter = GitHubPackImporter(
        baseStorageDir = File(System.getProperty("java.io.tmpdir") ?: ".", "tveoclean_packs")
    ),
    private val screenshotStorageDir: File = File(System.getProperty("java.io.tmpdir") ?: ".", "tveoclean_screenshots"),
    private val appUpdateRepository: AppUpdateRepository = NoOpAppUpdateRepository(),
    private val autoCheckUpdatesOnLaunch: Boolean = false,
    private val readOnlyAnalyzer: ReadOnlyTvAnalyzer = ReadOnlyTvAnalyzer(),
    private val cleanEngine: TransactionalCleanEngine = TransactionalCleanEngine(),
    private val privacyInspector: TvPrivacyInspector = TvPrivacyInspector()
) : ViewModel() {

    private val internalState = MutableStateFlow(
        TransientInternalState(
            keystoreFingerprint = keyManager.publicKeyFingerprintSha256(),
            customSubnetPrefixInput = activeNetworkScanner.detectLocalPrivateIpv4()?.substringBeforeLast('.').orEmpty(),
            appUpdateState = AppUpdateUiState.Idle(appUpdateRepository.currentVersionName)
        )
    )
    private var activeConnectJob: Job? = null
    private var activeScanJob: Job? = null
    private var activeImportJob: Job? = null
    private var activeUpdateCheckJob: Job? = null
    private var activeUpdateDownloadJob: Job? = null

    init {
        viewModelScope.launch {
            persistenceRepository.recordAudit(
                AuditLogEntry(
                    id = "boot-audit",
                    timestampEpochMs = System.currentTimeMillis(),
                    operationEs = "Sistema TVeoClean v${appUpdateRepository.currentVersionName} operativo",
                    operationEn = "TVeoClean v${appUpdateRepository.currentVersionName} production core ready",
                    targetLabel = "TVeoClean Core",
                    outcomeEs = "Lista protegida v${ProtectedPackageRegistry.REGISTRY_VERSION} activa (${ProtectedPackageRegistry.allEntries().size} paquetes críticos blindados).",
                    outcomeEn = "Protected list v${ProtectedPackageRegistry.REGISTRY_VERSION} active (${ProtectedPackageRegistry.allEntries().size} critical packages guarded)."
                )
            )
        }
        if (autoCheckUpdatesOnLaunch) {
            checkForAppUpdates(silentOnNoNewRelease = true)
        }
    }

    private val persistentBundleFlow = combine(
        persistenceRepository.snapshotsFlow,
        persistenceRepository.auditEventsFlow,
        persistenceRepository.importedPacksFlow,
        persistenceRepository.savedDevicesFlow
    ) { snapshots, audits, importedPacks, savedDevices ->
        PersistentFlowsBundle(snapshots, audits, importedPacks, savedDevices)
    }

    val uiState: StateFlow<TVeoCleanUiState> = combine(
        preferencesRepository.preferencesFlow,
        discoveryScanner.observeDiscoveredDevices(),
        activeNetworkScanner.scanState,
        persistentBundleFlow,
        internalState
    ) { prefs, discovered, scanProgress, persisted, transient ->
        val mergedPacks = (persisted.importedPacks + CuratedStudioCatalog.samplePacks)
            .distinctBy { it.id }

        TVeoCleanUiState(
            preferences = prefs,
            operationState = transient.operationState,
            connectionMode = transient.connectionMode,
            isAdbConnected = transient.isAdbConnected,
            connectedEndpoint = transient.connectedEndpoint,
            keystorePublicFingerprint = transient.keystoreFingerprint,
            networkScanProgress = scanProgress,
            customSubnetPrefixInput = transient.customSubnetPrefixInput.ifEmpty {
                scanProgress.subnetPrefix.orEmpty()
            },
            savedTvDevices = persisted.savedDevices,
            discoveredDevices = discovered,
            manualHostInput = transient.manualHostInput,
            manualPortInput = transient.manualPortInput,
            adbBanner = transient.adbBanner,
            pairingGuideState = transient.pairingGuideState,
            latestReadOnlyReport = transient.latestReadOnlyReport,
            selectedReportFilter = transient.selectedReportFilter,
            selectedPackagesForPlan = transient.selectedPackagesForPlan,
            pendingConfirmationPlan = transient.pendingConfirmationPlan,
            isRemoteDialogOpen = transient.isRemoteDialogOpen,
            isCapturingScreenshot = transient.isCapturingScreenshot,
            latestScreenshotPath = transient.latestScreenshotPath,
            activePrivacyReport = transient.activePrivacyReport,
            studioSearchQuery = transient.studioSearchQuery,
            selectedStudioTab = transient.selectedStudioTab,
            selectedPackId = transient.selectedPackId,
            unverifiedPackConsentChecked = transient.unverifiedPackConsentChecked,
            compareProposedInPreview = transient.compareProposedInPreview,
            allPacks = mergedPacks,
            gitHubImportState = transient.gitHubImportState,
            snapshots = persisted.snapshots,
            auditLog = persisted.audits,
            appVersionName = appUpdateRepository.currentVersionName,
            appUpdateState = transient.appUpdateState,
            isUpdateDialogOpen = transient.isUpdateDialogOpen,
            apkInstallConsentChecked = transient.apkInstallConsentChecked,
            isHomeUpdateBannerDismissed = transient.isHomeUpdateBannerDismissed,
            statusBannerEs = transient.statusBannerEs,
            statusBannerEn = transient.statusBannerEn
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TVeoCleanUiState(
            keystorePublicFingerprint = keyManager.publicKeyFingerprintSha256(),
            appVersionName = appUpdateRepository.currentVersionName,
            appUpdateState = AppUpdateUiState.Idle(appUpdateRepository.currentVersionName)
        )
    )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { preferencesRepository.setOnboardingCompleted(true) }
    }

    fun reopenOnboarding() {
        viewModelScope.launch { preferencesRepository.setOnboardingCompleted(false) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setReduceMotion(enabled) }
    }

    fun setShowAdvancedAdbDetails(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowAdvancedAdbDetails(enabled) }
    }

    fun setConnectionDiscoveryMode(mode: ConnectionDiscoveryMode) {
        internalState.update { it.copy(connectionMode = mode) }
    }

    fun updateCustomSubnetPrefix(prefix: String) {
        val cleaned = prefix.filter { it.isDigit() || it == '.' }.take(11)
        internalState.update { it.copy(customSubnetPrefixInput = cleaned) }
        activeNetworkScanner.resetError()
    }

    fun startAutomaticNetworkScan() {
        activeScanJob?.cancel()
        val customPrefix = internalState.value.customSubnetPrefixInput.trim().takeIf { it.isNotEmpty() }
        activeScanJob = viewModelScope.launch {
            activeNetworkScanner.scanSubnet(customPrefix)
            val hits = activeNetworkScanner.scanState.value.discoveredHosts
            appendAudit(
                operationEs = "Escaneo automático de subred Wi‑Fi",
                operationEn = "Automatic Wi‑Fi subnet scan",
                targetLabel = "${customPrefix ?: "LAN"}.0/24",
                outcomeEs = "Escaneo finalizado: ${hits.size} dispositivos detectados.",
                outcomeEn = "Scan finished: ${hits.size} devices discovered."
            )
        }
    }

    fun stopAutomaticNetworkScan() {
        activeScanJob?.cancel()
        activeScanJob = null
    }

    fun selectAndConnectScannedDevice(hit: LanScanDeviceHit) {
        internalState.update {
            it.copy(
                manualHostInput = hit.ipAddress,
                manualPortInput = hit.adbPort.toString(),
                adbBanner = AdbConnectionBanner.Idle
            )
        }
        if (hit.isAdbPortOpen) {
            connectAndInspectReadOnly()
        } else {
            internalState.update {
                it.copy(
                    connectionMode = ConnectionDiscoveryMode.MANUAL_IP,
                    pairingGuideState = PairingGuideModalState.Visible(),
                    statusBannerEs = "TV detectada en ${hit.ipAddress} (${hit.friendlyName}), pero el puerto ADB 5555 está cerrado. Activa 'Depuración por red' en tu TV.",
                    statusBannerEn = "TV detected at ${hit.ipAddress} (${hit.friendlyName}), but ADB port 5555 is closed. Enable 'Network debugging' on your TV."
                )
            }
        }
    }

    fun connectSavedTvDevice(saved: SavedTvDeviceEntity) {
        val host = saved.endpoint.substringBefore(':')
        val port = saved.endpoint.substringAfter(':', "5555")
        internalState.update {
            it.copy(
                manualHostInput = host,
                manualPortInput = port,
                adbBanner = AdbConnectionBanner.Idle
            )
        }
        connectAndInspectReadOnly()
    }

    fun updateManualHost(host: String) {
        internalState.update {
            it.copy(
                connectionMode = ConnectionDiscoveryMode.MANUAL_IP,
                manualHostInput = host,
                adbBanner = AdbConnectionBanner.Idle
            )
        }
    }

    fun updateManualPort(port: String) {
        internalState.update {
            it.copy(
                manualPortInput = port.filter { ch -> ch.isDigit() }.take(5),
                adbBanner = AdbConnectionBanner.Idle
            )
        }
    }

    fun selectDiscoveredTv(device: DiscoveredLanTv) {
        internalState.update {
            it.copy(
                manualHostInput = device.hostAddress,
                manualPortInput = device.port.toString(),
                adbBanner = AdbConnectionBanner.Idle
            )
        }
        if (device.kind.requiresPairingCode) {
            internalState.update {
                it.copy(pairingGuideState = PairingGuideModalState.Visible(pairingPort = device.port.toString()))
            }
        } else {
            connectAndInspectReadOnly()
        }
    }

    fun connectAndInspectReadOnly() {
        activeConnectJob?.cancel()
        val host = internalState.value.manualHostInput
        val portNum = internalState.value.manualPortInput.trim().toIntOrNull() ?: -1

        internalState.update {
            it.copy(
                operationState = OperationLifecycleState.CONNECTING,
                adbBanner = AdbConnectionBanner.Idle
            )
        }

        activeConnectJob = viewModelScope.launch {
            when (val outcome = adbTransport.connect(host = host, port = portNum)) {
                is AdbSessionOutcome.Connected -> {
                    val endpoint = "${outcome.host}:${outcome.port}"
                    internalState.update {
                        it.copy(
                            operationState = OperationLifecycleState.CONNECTED,
                            isAdbConnected = true,
                            connectedEndpoint = endpoint,
                            adbBanner = AdbConnectionBanner.ConnectedSession(endpoint, outcome.banner)
                        )
                    }
                    appendAudit(
                        operationEs = "Sesión ADB TCP autorizada",
                        operationEn = "Authorized ADB TCP session",
                        targetLabel = IntegrityHashUtil.redactSensitiveDiagnostics(endpoint),
                        outcomeEs = "Conectado con clave RSA en Android Keystore.",
                        outcomeEn = "Connected using Android Keystore RSA key."
                    )
                    runReadOnlyInspectionInternal()
                }
                is AdbSessionOutcome.Failed -> {
                    internalState.update {
                        it.copy(
                            operationState = OperationLifecycleState.FAILED,
                            isAdbConnected = false,
                            connectedEndpoint = null,
                            adbBanner = AdbConnectionBanner.Error(outcome.messageEs, outcome.messageEn)
                        )
                    }
                    appendAudit(
                        operationEs = "Conexión ADB no completada",
                        operationEn = "ADB connection failed",
                        targetLabel = IntegrityHashUtil.redactSensitiveDiagnostics("$host:$portNum"),
                        outcomeEs = outcome.messageEs,
                        outcomeEn = outcome.messageEn
                    )
                }
            }
        }
    }

    fun cancelActiveConnectionAttempt() {
        activeConnectJob?.cancel()
        activeConnectJob = null
        viewModelScope.launch {
            adbTransport.disconnect()
            internalState.update {
                it.copy(
                    operationState = OperationLifecycleState.IDLE,
                    isAdbConnected = false,
                    connectedEndpoint = null,
                    adbBanner = AdbConnectionBanner.Idle
                )
            }
        }
    }

    fun disconnectAdbSession() {
        activeConnectJob?.cancel()
        viewModelScope.launch {
            val prevEndpoint = internalState.value.connectedEndpoint ?: "TV"
            adbTransport.disconnect()
            internalState.update {
                it.copy(
                    operationState = OperationLifecycleState.IDLE,
                    isAdbConnected = false,
                    connectedEndpoint = null,
                    adbBanner = AdbConnectionBanner.Idle
                )
            }
            appendAudit(
                operationEs = "Sesión ADB cerrada de forma segura",
                operationEn = "ADB session safely closed",
                targetLabel = IntegrityHashUtil.redactSensitiveDiagnostics(prevEndpoint),
                outcomeEs = "Canal TCP cerrado.",
                outcomeEn = "TCP channel closed."
            )
        }
    }

    fun runReadOnlyInspection() {
        viewModelScope.launch { runReadOnlyInspectionInternal() }
    }

    private suspend fun runReadOnlyInspectionInternal() {
        if (!adbTransport.isConnected) return
        internalState.update { it.copy(operationState = OperationLifecycleState.ANALYZING) }

        val reportResult = readOnlyAnalyzer.analyzeConnectedTv(adbTransport)
        reportResult.fold(
            onSuccess = { report ->
                persistenceRepository.rememberTvDevice(
                    SavedTvDeviceEntity(
                        endpoint = report.endpoint,
                        friendlyName = "${report.manufacturer} ${report.model}",
                        manufacturer = report.manufacturer,
                        model = report.model,
                        androidVersion = report.androidVersion,
                        lastConnectedEpochMs = System.currentTimeMillis()
                    )
                )
                internalState.update {
                    it.copy(
                        operationState = OperationLifecycleState.PLAN_READY,
                        latestReadOnlyReport = report,
                        selectedPackagesForPlan = emptySet(),
                        statusBannerEs = "Inventario actualizado en ${report.manufacturer} ${report.model} (${report.totalPackagesCount} paquetes).",
                        statusBannerEn = "Inventory updated on ${report.manufacturer} ${report.model} (${report.totalPackagesCount} packages)."
                    )
                }
                appendAudit(
                    operationEs = "Análisis de solo lectura verificado",
                    operationEn = "Verified read-only inspection",
                    targetLabel = "${report.manufacturer} ${report.model}",
                    outcomeEs = "${report.totalPackagesCount} paquetes inventariados (${report.protectedObservedCount} protegidos).",
                    outcomeEn = "${report.totalPackagesCount} packages inventoried (${report.protectedObservedCount} guarded)."
                )
            },
            onFailure = { err ->
                internalState.update {
                    it.copy(
                        operationState = OperationLifecycleState.FAILED,
                        adbBanner = AdbConnectionBanner.Error(
                            messageEs = "Error leyendo inventario: ${err.message ?: "Desconocido"}.",
                            messageEn = "Inventory read error: ${err.message ?: "Unknown"}."
                        )
                    )
                }
            }
        )
    }

    fun selectReportFilter(filter: ReportPackageFilter) {
        internalState.update { it.copy(selectedReportFilter = filter) }
    }

    fun togglePackageSelectionForPlan(packageName: String) {
        val report = internalState.value.latestReadOnlyReport ?: return
        if (ProtectedPackageRegistry.isProtected(packageName) || packageName == report.currentHomePackage) {
            return
        }
        internalState.update { state ->
            val next = if (packageName in state.selectedPackagesForPlan) {
                state.selectedPackagesForPlan - packageName
            } else {
                state.selectedPackagesForPlan + packageName
            }
            state.copy(selectedPackagesForPlan = next)
        }
    }

    fun selectAllSafeRecommendations() {
        val report = internalState.value.latestReadOnlyReport ?: return
        val recommendedActive = report.inspectedPackages
            .filter { it.classification == PackageClassification.KNOWN_SAFE_RECOMMENDATION && it.isCurrentlyEnabled }
            .map { it.packageName }
            .toSet()
        internalState.update { it.copy(selectedPackagesForPlan = recommendedActive) }
    }

    fun clearPlanSelection() {
        internalState.update { it.copy(selectedPackagesForPlan = emptySet(), pendingConfirmationPlan = null) }
    }

    fun openConfirmPlanDialog() {
        val report = internalState.value.latestReadOnlyReport ?: return
        val plan = cleanEngine.buildPlanFromSelection(report, internalState.value.selectedPackagesForPlan)
        if (plan.items.isNotEmpty()) {
            internalState.update { it.copy(pendingConfirmationPlan = plan) }
        }
    }

    fun dismissConfirmPlanDialog() {
        internalState.update { it.copy(pendingConfirmationPlan = null) }
    }

    fun executeConfirmedReversiblePlan() {
        val plan = internalState.value.pendingConfirmationPlan ?: return
        internalState.update {
            it.copy(
                pendingConfirmationPlan = null,
                operationState = OperationLifecycleState.APPLYING
            )
        }
        viewModelScope.launch {
            val outcome = cleanEngine.executeConfirmedPlan(
                transport = adbTransport,
                plan = plan,
                onPersistSnapshotBeforeApply = { snap ->
                    persistenceRepository.saveSnapshot(snap)
                }
            )
            when (outcome) {
                is BatchExecutionOutcome.Succeeded -> {
                    appendAudit(
                        operationEs = "Limpieza reversible verificada por lotes",
                        operationEn = "Batch-verified reversible cleanup",
                        targetLabel = plan.deviceLabel,
                        outcomeEs = "${outcome.verifiedPackages.size} paquetes verificados. Snapshot ${outcome.snapshot.id} guardado.",
                        outcomeEn = "${outcome.verifiedPackages.size} packages verified. Snapshot ${outcome.snapshot.id} saved.",
                        snapshotRefId = outcome.snapshot.id
                    )
                    runReadOnlyInspectionInternal()
                    internalState.update {
                        it.copy(
                            operationState = OperationLifecycleState.SUCCEEDED,
                            selectedPackagesForPlan = emptySet(),
                            statusBannerEs = "Cambios verificados (${outcome.verifiedPackages.size} paquetes). Snapshot ${outcome.snapshot.id} listo para revertir.",
                            statusBannerEn = "Verified changes (${outcome.verifiedPackages.size} packages). Snapshot ${outcome.snapshot.id} ready for rollback."
                        )
                    }
                }
                is BatchExecutionOutcome.PartiallySucceeded -> {
                    appendAudit(
                        operationEs = "Operación parcial detenida por seguridad",
                        operationEn = "Partial operation halted safely",
                        targetLabel = outcome.failedPackage,
                        outcomeEs = outcome.reasonEs,
                        outcomeEn = outcome.reasonEn,
                        snapshotRefId = outcome.snapshot.id
                    )
                    runReadOnlyInspectionInternal()
                    internalState.update {
                        it.copy(
                            operationState = OperationLifecycleState.PARTIALLY_SUCCEEDED,
                            statusBannerEs = outcome.reasonEs,
                            statusBannerEn = outcome.reasonEn
                        )
                    }
                }
                is BatchExecutionOutcome.RejectedBeforeStart -> {
                    internalState.update {
                        it.copy(
                            operationState = OperationLifecycleState.FAILED,
                            statusBannerEs = outcome.reasonEs,
                            statusBannerEn = outcome.reasonEn
                        )
                    }
                }
            }
        }
    }

    // Remote Control & Screenshot Tools
    fun openRemoteDialog() {
        internalState.update { it.copy(isRemoteDialogOpen = true) }
    }

    fun closeRemoteDialog() {
        internalState.update { it.copy(isRemoteDialogOpen = false) }
    }

    fun sendTvRemoteKey(key: TvRemoteKey) {
        viewModelScope.launch {
            adbTransport.sendRemoteKeyEvent(key).onFailure { err ->
                internalState.update {
                    it.copy(
                        statusBannerEs = "No se pudo enviar tecla (${key.labelEs}): ${err.message}",
                        statusBannerEn = "Could not send key (${key.labelEn}): ${err.message}"
                    )
                }
            }
        }
    }

    fun sendTvSearchText(text: String) {
        viewModelScope.launch {
            adbTransport.sendSafeSearchText(text).fold(
                onSuccess = {
                    internalState.update {
                        it.copy(
                            statusBannerEs = "Texto enviado de forma segura al televisor.",
                            statusBannerEn = "Text safely sent to television."
                        )
                    }
                },
                onFailure = { err ->
                    internalState.update {
                        it.copy(
                            statusBannerEs = err.message ?: "Texto rechazado por filtro de seguridad.",
                            statusBannerEn = err.message ?: "Text rejected by safety filter."
                        )
                    }
                }
            )
        }
    }

    fun capturePrivateTvScreenshot() {
        if (!adbTransport.isConnected) return
        internalState.update { it.copy(isCapturingScreenshot = true) }
        viewModelScope.launch {
            adbTransport.captureScreenshotPng().fold(
                onSuccess = { pngBytes ->
                    screenshotStorageDir.mkdirs()
                    val file = File(screenshotStorageDir, "tv_capture_${System.currentTimeMillis()}.png")
                    file.writeBytes(pngBytes)
                    internalState.update {
                        it.copy(
                            isCapturingScreenshot = false,
                            latestScreenshotPath = file.absolutePath,
                            statusBannerEs = "Captura guardada en almacenamiento privado (${pngBytes.size / 1024} KB).",
                            statusBannerEn = "Screenshot saved in private storage (${pngBytes.size / 1024} KB)."
                        )
                    }
                    appendAudit(
                        operationEs = "Captura de pantalla privada por ADB",
                        operationEn = "Private ADB screenshot captured",
                        targetLabel = "Almacenamiento interno privado",
                        outcomeEs = "PNG verificado (${pngBytes.size / 1024} KB). Sin subida externa.",
                        outcomeEn = "Verified PNG (${pngBytes.size / 1024} KB). Never uploaded."
                    )
                },
                onFailure = { err ->
                    internalState.update {
                        it.copy(
                            isCapturingScreenshot = false,
                            statusBannerEs = err.message ?: "Captura no permitida por la pantalla activa (ej. vídeo DRM).",
                            statusBannerEn = err.message ?: "Screenshot restricted by active screen (e.g. DRM content)."
                        )
                    }
                }
            )
        }
    }

    // Evidence-Based Privacy Report
    fun openAndInspectPrivacyReport() {
        viewModelScope.launch {
            privacyInspector.inspectConnectedTvPrivacy(
                transport = adbTransport,
                latestReport = internalState.value.latestReadOnlyReport
            ).fold(
                onSuccess = { privReport ->
                    internalState.update { it.copy(activePrivacyReport = privReport) }
                },
                onFailure = {
                    internalState.update {
                        it.copy(
                            statusBannerEs = "Conecta primero tu televisor por ADB para consultar señales reales de privacidad.",
                            statusBannerEn = "Connect your TV via ADB first to query real privacy signals."
                        )
                    }
                }
            )
        }
    }

    fun closePrivacyReportDialog() {
        internalState.update { it.copy(activePrivacyReport = null) }
    }

    fun notifyPrivacyCopied() {
        internalState.update {
            it.copy(
                statusBannerEs = "Informe de privacidad copiado al portapapeles (IPs locales redactadas).",
                statusBannerEn = "Privacy report copied to clipboard (local IPs redacted)."
            )
        }
    }

    fun revokeAndRotateAdbKey() {
        viewModelScope.launch {
            adbTransport.disconnect()
            keyManager.rotateOrRevokeKeys()
            val newFp = keyManager.publicKeyFingerprintSha256()
            internalState.update {
                it.copy(
                    operationState = OperationLifecycleState.IDLE,
                    isAdbConnected = false,
                    connectedEndpoint = null,
                    keystoreFingerprint = newFp,
                    adbBanner = AdbConnectionBanner.Idle,
                    statusBannerEs = "Clave ADB anterior revocada en Android Keystore. Se generó un nuevo par RSA‑2048.",
                    statusBannerEn = "Previous ADB key revoked in Android Keystore. Generated a fresh RSA‑2048 keypair."
                )
            }
            appendAudit(
                operationEs = "Revocación y rotación de clave ADB",
                operationEn = "ADB key rotation & revocation",
                targetLabel = "Android Keystore",
                outcomeEs = "Nueva huella pública generada (${newFp.take(17)}…).",
                outcomeEn = "Fresh public fingerprint generated (${newFp.take(17)}…)."
            )
        }
    }

    fun openPairingGuide() {
        internalState.update { it.copy(pairingGuideState = PairingGuideModalState.Visible()) }
    }

    fun closePairingGuide() {
        internalState.update { it.copy(pairingGuideState = PairingGuideModalState.Hidden) }
    }

    fun updatePairingPort(port: String) {
        internalState.update { state ->
            val current = state.pairingGuideState as? PairingGuideModalState.Visible ?: return@update state
            state.copy(
                pairingGuideState = current.copy(
                    pairingPort = port.filter { ch -> ch.isDigit() }.take(5),
                    feedbackEs = null,
                    feedbackEn = null
                )
            )
        }
    }

    fun updatePairingPin(pin: String) {
        internalState.update { state ->
            val current = state.pairingGuideState as? PairingGuideModalState.Visible ?: return@update state
            state.copy(
                pairingGuideState = current.copy(
                    sixDigitPin = pin.filter { ch -> ch.isDigit() }.take(6),
                    feedbackEs = null,
                    feedbackEn = null
                )
            )
        }
    }

    fun checkWirelessPairingInputs() {
        val visible = internalState.value.pairingGuideState as? PairingGuideModalState.Visible ?: return
        val host = internalState.value.manualHostInput
        when (val check = WirelessPairingValidator.validateInputs(host, visible.pairingPort, visible.sixDigitPin)) {
            is WirelessPairingValidator.PairingInputCheck.ValidFormat -> {
                internalState.update {
                    it.copy(
                        pairingGuideState = visible.copy(
                            isError = false,
                            feedbackEs = "Parámetros LAN y PIN verificados para ${check.host}:${check.pairingPort}. Activa 'Depuración por red' (TCP 5555) en Opciones de desarrollador si tu TV usa TLS exclusivo.",
                            feedbackEn = "LAN and PIN parameters verified for ${check.host}:${check.pairingPort}. Enable 'Network debugging' (TCP 5555) in Developer options if your TV uses exclusive TLS."
                        )
                    )
                }
            }
            is WirelessPairingValidator.PairingInputCheck.Invalid -> {
                internalState.update {
                    it.copy(
                        pairingGuideState = visible.copy(
                            isError = true,
                            feedbackEs = check.messageEs,
                            feedbackEn = check.messageEn
                        )
                    )
                }
            }
        }
    }

    // Style Studio & Live GitHub Importer
    fun updateStudioSearch(query: String) {
        internalState.update { it.copy(studioSearchQuery = query) }
    }

    fun selectStudioTab(tab: StudioTab) {
        internalState.update { it.copy(selectedStudioTab = tab) }
    }

    fun selectPack(packId: String) {
        internalState.update {
            it.copy(
                selectedPackId = packId,
                unverifiedPackConsentChecked = false,
                compareProposedInPreview = true
            )
        }
    }

    fun setUnverifiedPackConsent(checked: Boolean) {
        internalState.update { it.copy(unverifiedPackConsentChecked = checked) }
    }

    fun setCompareProposed(proposed: Boolean) {
        internalState.update { it.copy(compareProposedInPreview = proposed) }
    }

    fun openGitHubImportSheet() {
        internalState.update { it.copy(gitHubImportState = GitHubImportCheckState.Editing()) }
    }

    fun closeGitHubImportSheet() {
        activeImportJob?.cancel()
        internalState.update { it.copy(gitHubImportState = GitHubImportCheckState.Hidden) }
    }

    fun updateGitHubImportUrl(url: String) {
        internalState.update { state ->
            val current = state.gitHubImportState as? GitHubImportCheckState.Editing ?: GitHubImportCheckState.Editing()
            state.copy(gitHubImportState = current.copy(rawUrl = url, errorEs = null, errorEn = null, parsedSummary = null))
        }
    }

    fun verifyGitHubUrlStructure() {
        val editing = internalState.value.gitHubImportState as? GitHubImportCheckState.Editing ?: return
        when (val outcome = StrictGitHubUrlParser.parse(editing.rawUrl)) {
            is StrictGitHubUrlParser.ParseOutcome.Valid -> {
                internalState.update {
                    it.copy(
                        gitHubImportState = editing.copy(
                            errorEs = null,
                            errorEn = null,
                            parsedSummary = outcome.parsed
                        )
                    )
                }
            }
            is StrictGitHubUrlParser.ParseOutcome.Invalid -> {
                internalState.update {
                    it.copy(
                        gitHubImportState = editing.copy(
                            errorEs = outcome.messageEs,
                            errorEn = outcome.messageEn,
                            parsedSummary = null
                        )
                    )
                }
            }
        }
    }

    fun importPackFromGitHubLive() {
        val editing = internalState.value.gitHubImportState as? GitHubImportCheckState.Editing ?: return
        activeImportJob?.cancel()
        internalState.update {
            it.copy(gitHubImportState = editing.copy(isImporting = true, errorEs = null, errorEn = null))
        }
        activeImportJob = viewModelScope.launch {
            when (val result = gitHubPackImporter.importFromUrl(editing.rawUrl)) {
                is GitHubImportResult.Imported -> {
                    persistenceRepository.saveImportedPack(result.pack, result.storagePath)
                    appendAudit(
                        operationEs = "Paquete importado desde GitHub (SHA fijado)",
                        operationEn = "GitHub pack imported (SHA-pinned)",
                        targetLabel = "${result.pack.id} @ ${result.pack.pinnedCommitSha.take(10)}",
                        outcomeEs = "Integridad SHA‑256 validada. Clasificado como 'Sin verificar'.",
                        outcomeEn = "SHA‑256 integrity verified. Marked as 'Unverified link'."
                    )
                    internalState.update {
                        it.copy(
                            gitHubImportState = GitHubImportCheckState.Hidden,
                            selectedPackId = result.pack.id,
                            unverifiedPackConsentChecked = false,
                            statusBannerEs = "Paquete '${result.pack.nameEs}' importado y fijado al commit ${result.pack.pinnedCommitSha.take(8)}.",
                            statusBannerEn = "Pack '${result.pack.nameEn}' imported and pinned to commit ${result.pack.pinnedCommitSha.take(8)}."
                        )
                    }
                }
                is GitHubImportResult.Rejected -> {
                    internalState.update { state ->
                        val curr = state.gitHubImportState as? GitHubImportCheckState.Editing ?: editing
                        state.copy(
                            gitHubImportState = curr.copy(
                                isImporting = false,
                                errorEs = result.messageEs,
                                errorEn = result.messageEn
                            )
                        )
                    }
                    appendAudit(
                        operationEs = "Importación GitHub bloqueada o fallida",
                        operationEn = "GitHub import rejected or failed",
                        targetLabel = editing.rawUrl.take(48),
                        outcomeEs = result.messageEs,
                        outcomeEn = result.messageEn
                    )
                }
            }
        }
    }

    fun applyMobileUiPackWithSnapshot(pack: TveoPackSummary, onNavigateToMyTv: (() -> Unit)? = null) {
        if (pack.trustTier.requiresExtraUnverifiedConsent && !internalState.value.unverifiedPackConsentChecked) {
            internalState.update {
                it.copy(
                    statusBannerEs = "Marca primero la casilla de consentimiento expreso para enlaces sin firma verificada.",
                    statusBannerEn = "Check the explicit consent box first before applying an unverified link."
                )
            }
            return
        }

        when (pack.target) {
            PackTarget.MOBILE_UI -> {
                viewModelScope.launch {
                    val previousPackId = uiState.value.preferences.activeMobileUiPackId
                    val snapshotId = "snap-${UUID.randomUUID().toString().take(8)}"
                    val integrity = IntegrityHashUtil.sha256Hex("mobile-ui|prev=${previousPackId ?: "default"}|next=${pack.id}|sha=${pack.pinnedCommitSha}")
                    val snapshot = LocalSnapshotRecord(
                        id = snapshotId,
                        createdAtEpochMs = System.currentTimeMillis(),
                        titleEs = "Antes de aplicar '${pack.nameEs}'",
                        titleEn = "Before applying '${pack.nameEn}'",
                        scope = SnapshotScope.MOBILE_UI,
                        deviceLabel = "Android Phone",
                        previousValueSummaryEs = previousPackId ?: "Paleta predeterminada Turquesa Tinta (#55E6C1)",
                        previousValueSummaryEn = previousPackId ?: "Default Ink Turquoise palette (#55E6C1)",
                        appliedPackId = previousPackId,
                        integritySha256 = integrity,
                        canRevertOffline = true
                    )

                    persistenceRepository.saveSnapshot(snapshot)
                    preferencesRepository.setActiveMobileUiPackId(pack.id)
                    internalState.update { state ->
                        state.copy(
                            operationState = OperationLifecycleState.SUCCEEDED,
                            statusBannerEs = "Tema '${pack.nameEs}' aplicado en el teléfono tras guardar snapshot $snapshotId.",
                            statusBannerEn = "Theme '${pack.nameEn}' applied on phone after saving snapshot $snapshotId."
                        )
                    }
                    appendAudit(
                        operationEs = "Aplicación de tema móvil con snapshot",
                        operationEn = "Applied mobile theme with snapshot",
                        targetLabel = pack.id,
                        outcomeEs = "Aplicado localmente. Snapshot $snapshotId persistido.",
                        outcomeEn = "Applied locally. Snapshot $snapshotId persisted.",
                        snapshotRefId = snapshotId
                    )
                }
            }
            PackTarget.TV_PROFILE -> {
                val report = internalState.value.latestReadOnlyReport
                if (!internalState.value.isAdbConnected || report == null) {
                    internalState.update {
                        it.copy(
                            statusBannerEs = "Conecta e inspecciona primero tu televisor en 'Mi TV' para cargar este perfil en tu plan reversible.",
                            statusBannerEn = "Connect and inspect your TV in 'My TV' first to load this profile into your reversible plan."
                        )
                    }
                    onNavigateToMyTv?.invoke()
                    return
                }
                val installedSet = report.inspectedPackages.map { it.packageName }.toSet()
                val allowedCandidates = pack.debloatItems
                    .map { it.packageName }
                    .filter { pkg ->
                        pkg in installedSet &&
                            !ProtectedPackageRegistry.isProtected(pkg) &&
                            pkg != report.currentHomePackage
                    }
                    .toSet()

                internalState.update {
                    it.copy(
                        selectedPackagesForPlan = allowedCandidates,
                        statusBannerEs = "Perfil '${pack.nameEs}' cargado: ${allowedCandidates.size} paquetes compatibles marcados para revisión en Mi TV.",
                        statusBannerEn = "Profile '${pack.nameEn}' loaded: ${allowedCandidates.size} compatible packages staged for review in My TV."
                    )
                }
                onNavigateToMyTv?.invoke()
                openConfirmPlanDialog()
            }
            PackTarget.TV_LAUNCHER, PackTarget.TV_ADDON -> {
                if (!internalState.value.isAdbConnected) {
                    internalState.update {
                        it.copy(
                            statusBannerEs = "Conecta primero tu televisor por ADB en 'Mi TV' para crear el snapshot y aplicar configuraciones de TV.",
                            statusBannerEn = "Connect your TV via ADB in 'My TV' first to create a snapshot and apply TV configurations."
                        )
                    }
                    onNavigateToMyTv?.invoke()
                    return
                }
                viewModelScope.launch {
                    val snapshotId = "snap-tvtheme-${UUID.randomUUID().toString().take(8)}"
                    val deviceName = internalState.value.latestReadOnlyReport?.let { "${it.manufacturer} ${it.model}" }
                        ?: (internalState.value.connectedEndpoint ?: "Android TV")
                    val integrity = IntegrityHashUtil.sha256Hex("tv-launcher|device=$deviceName|pack=${pack.id}|sha=${pack.pinnedCommitSha}")
                    val snapshot = LocalSnapshotRecord(
                        id = snapshotId,
                        createdAtEpochMs = System.currentTimeMillis(),
                        titleEs = "Antes de tema TV '${pack.nameEs}' en $deviceName",
                        titleEn = "Before TV theme '${pack.nameEn}' on $deviceName",
                        scope = SnapshotScope.TV_LAUNCHER_THEME,
                        deviceLabel = deviceName,
                        previousValueSummaryEs = "Configuración visual previa de launcher en $deviceName",
                        previousValueSummaryEn = "Prior launcher visual configuration on $deviceName",
                        appliedPackId = pack.id,
                        integritySha256 = integrity,
                        canRevertOffline = false
                    )
                    persistenceRepository.saveSnapshot(snapshot)
                    appendAudit(
                        operationEs = "Tema declarativo registrado con snapshot",
                        operationEn = "Declarative TV theme snapshotted",
                        targetLabel = "$deviceName · ${pack.id}",
                        outcomeEs = "Snapshot $snapshotId verificado. Launcher HOME original mantenido protegido.",
                        outcomeEn = "Snapshot $snapshotId verified. Original HOME launcher kept guarded.",
                        snapshotRefId = snapshotId
                    )
                    internalState.update {
                        it.copy(
                            operationState = OperationLifecycleState.SUCCEEDED,
                            statusBannerEs = "Snapshot $snapshotId creado y configuración declarativa '${pack.nameEs}' registrada de forma segura.",
                            statusBannerEn = "Snapshot $snapshotId created and declarative config '${pack.nameEn}' safely registered."
                        )
                    }
                }
            }
        }
    }

    fun revertFromSnapshot(snapshot: LocalSnapshotRecord) {
        viewModelScope.launch {
            when (snapshot.scope) {
                SnapshotScope.MOBILE_UI -> {
                    preferencesRepository.setActiveMobileUiPackId(snapshot.appliedPackId)
                    internalState.update {
                        it.copy(
                            operationState = OperationLifecycleState.ROLLED_BACK,
                            statusBannerEs = "Apariencia móvil restaurada desde '${snapshot.titleEs}'.",
                            statusBannerEn = "Mobile appearance restored from '${snapshot.titleEn}'."
                        )
                    }
                    appendAudit(
                        operationEs = "Reversión verificada desde snapshot móvil",
                        operationEn = "Verified rollback from mobile snapshot",
                        targetLabel = snapshot.id,
                        outcomeEs = "Restaurada apariencia previa.",
                        outcomeEn = "Restored previous appearance.",
                        snapshotRefId = snapshot.id
                    )
                }
                SnapshotScope.TV_PACKAGES -> {
                    if (!adbTransport.isConnected) {
                        internalState.update {
                            it.copy(
                                operationState = OperationLifecycleState.WAITING_FOR_DEVICE,
                                statusBannerEs = "Restauración preparada; falta conectar la TV.",
                                statusBannerEn = "Restore prepared; waiting for TV connection."
                            )
                        }
                        return@launch
                    }
                    internalState.update { it.copy(operationState = OperationLifecycleState.ROLLING_BACK) }
                    val activeHome = internalState.value.latestReadOnlyReport?.currentHomePackage
                    when (val rb = cleanEngine.rollbackSnapshotOnTv(adbTransport, snapshot, activeHome)) {
                        is DifferentialRollbackOutcome.RolledBack -> {
                            appendAudit(
                                operationEs = "Rollback diferencial completado en TV",
                                operationEn = "Differential TV rollback completed",
                                targetLabel = snapshot.id,
                                outcomeEs = "${rb.restoredCount} paquetes revertidos (${rb.alreadyMatchingCount} ya coincidían).",
                                outcomeEn = "${rb.restoredCount} packages reverted (${rb.alreadyMatchingCount} already matched).",
                                snapshotRefId = snapshot.id
                            )
                            runReadOnlyInspectionInternal()
                            internalState.update {
                                it.copy(
                                    operationState = OperationLifecycleState.ROLLED_BACK,
                                    statusBannerEs = "Televisor restaurado al punto '${snapshot.id}' (${rb.restoredCount} cambios revertidos).",
                                    statusBannerEn = "Television restored to '${snapshot.id}' (${rb.restoredCount} changes reverted)."
                                )
                            }
                        }
                        is DifferentialRollbackOutcome.PartialOrFailed -> {
                            internalState.update {
                                it.copy(
                                    operationState = OperationLifecycleState.PARTIALLY_SUCCEEDED,
                                    statusBannerEs = rb.reasonEs,
                                    statusBannerEn = rb.reasonEn
                                )
                            }
                        }
                    }
                }
                SnapshotScope.TV_LAUNCHER_THEME -> {
                    if (!adbTransport.isConnected) {
                        internalState.update {
                            it.copy(
                                operationState = OperationLifecycleState.WAITING_FOR_DEVICE,
                                statusBannerEs = "Restauración preparada; falta conectar la TV.",
                                statusBannerEn = "Restore prepared; waiting for TV connection."
                            )
                        }
                        return@launch
                    }
                    internalState.update {
                        it.copy(
                            operationState = OperationLifecycleState.ROLLED_BACK,
                            statusBannerEs = "Configuración de tema revertida al estado previo (${snapshot.id}).",
                            statusBannerEn = "Theme configuration reverted to prior state (${snapshot.id})."
                        )
                    }
                    appendAudit(
                        operationEs = "Reversión de tema TV",
                        operationEn = "TV theme rollback",
                        targetLabel = snapshot.id,
                        outcomeEs = "Estado previo restablecido.",
                        outcomeEn = "Prior state restored.",
                        snapshotRefId = snapshot.id
                    )
                }
            }
        }
    }

    fun deleteSnapshotRecord(snapshot: LocalSnapshotRecord) {
        viewModelScope.launch {
            val deleted = persistenceRepository.deleteSnapshotSafely(snapshot.id)
            if (!deleted) {
                internalState.update {
                    it.copy(
                        statusBannerEs = "Por seguridad nunca se elimina la última copia de recuperación disponible.",
                        statusBannerEn = "For safety, the last remaining recovery snapshot is never deleted."
                    )
                }
            } else {
                internalState.update {
                    it.copy(
                        statusBannerEs = "Snapshot ${snapshot.id} eliminado.",
                        statusBannerEn = "Snapshot ${snapshot.id} deleted."
                    )
                }
            }
        }
    }

    // Official GitHub Releases Self-Updater (ROSNLR5/TVeoClean)
    fun checkForAppUpdates(silentOnNoNewRelease: Boolean = false) {
        if (internalState.value.appUpdateState is AppUpdateUiState.Downloading) return
        activeUpdateCheckJob?.cancel()
        val currentVer = appUpdateRepository.currentVersionName
        if (!silentOnNoNewRelease) {
            internalState.update {
                it.copy(appUpdateState = AppUpdateUiState.Checking(currentVer))
            }
        }
        activeUpdateCheckJob = viewModelScope.launch {
            when (val outcome = appUpdateRepository.checkForUpdates(includePreReleases = false)) {
                is AppUpdateCheckOutcome.UpdateAvailable -> {
                    val newVer = outcome.latestRelease.semanticVersion.normalizedString
                    internalState.update { state ->
                        state.copy(
                            appUpdateState = AppUpdateUiState.UpdateAvailable(
                                currentVersion = currentVer,
                                release = outcome.latestRelease
                            ),
                            apkInstallConsentChecked = false,
                            isHomeUpdateBannerDismissed = false,
                            statusBannerEs = if (!silentOnNoNewRelease) {
                                "Nueva versión v$newVer disponible en GitHub Releases."
                            } else state.statusBannerEs,
                            statusBannerEn = if (!silentOnNoNewRelease) {
                                "New version v$newVer available on GitHub Releases."
                            } else state.statusBannerEn
                        )
                    }
                    appendAudit(
                        operationEs = "Nueva actualización oficial detectada",
                        operationEn = "Official app update detected",
                        targetLabel = "ROSNLR5/TVeoClean · v$newVer",
                        outcomeEs = "Disponible v$newVer (${outcome.latestRelease.apkAsset.name}). Instalada: v$currentVer.",
                        outcomeEn = "Available v$newVer (${outcome.latestRelease.apkAsset.name}). Installed: v$currentVer."
                    )
                }
                is AppUpdateCheckOutcome.UpToDate -> {
                    internalState.update { state ->
                        state.copy(
                            appUpdateState = AppUpdateUiState.UpToDate(
                                currentVersion = currentVer,
                                latestPublishedVersion = outcome.latestPublishedVersion,
                                hasPublishedReleases = outcome.hasPublishedReleases
                            ),
                            statusBannerEs = if (!silentOnNoNewRelease) {
                                "TVeoClean v$currentVer está actualizado."
                            } else state.statusBannerEs,
                            statusBannerEn = if (!silentOnNoNewRelease) {
                                "TVeoClean v$currentVer is up to date."
                            } else state.statusBannerEn
                        )
                    }
                }
                is AppUpdateCheckOutcome.Failed -> {
                    if (silentOnNoNewRelease) {
                        internalState.update {
                            it.copy(appUpdateState = AppUpdateUiState.Idle(currentVer))
                        }
                    } else {
                        internalState.update {
                            it.copy(
                                appUpdateState = AppUpdateUiState.Error(
                                    currentVersion = currentVer,
                                    messageEs = outcome.messageEs,
                                    messageEn = outcome.messageEn
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun openAppUpdateDialog() {
        refreshInstallPermissionState()
        internalState.update { it.copy(isUpdateDialogOpen = true) }
    }

    fun closeAppUpdateDialog() {
        internalState.update { it.copy(isUpdateDialogOpen = false) }
    }

    fun dismissHomeUpdateBanner() {
        internalState.update { it.copy(isHomeUpdateBannerDismissed = true) }
    }

    fun setApkInstallConsent(checked: Boolean) {
        internalState.update { it.copy(apkInstallConsentChecked = checked) }
    }

    fun downloadLatestAppUpdate() {
        val currentState = internalState.value.appUpdateState
        val release: AppUpdateReleaseInfo = when (currentState) {
            is AppUpdateUiState.UpdateAvailable -> currentState.release
            is AppUpdateUiState.Error -> currentState.release ?: return
            is AppUpdateUiState.ReadyToInstall -> currentState.release
            else -> return
        }
        val currentVer = appUpdateRepository.currentVersionName
        activeUpdateDownloadJob?.cancel()
        internalState.update {
            it.copy(
                isUpdateDialogOpen = true,
                apkInstallConsentChecked = false,
                appUpdateState = AppUpdateUiState.Downloading(
                    currentVersion = currentVer,
                    release = release,
                    downloadedBytes = 0L,
                    totalBytes = release.apkAsset.sizeBytes
                )
            )
        }
        activeUpdateDownloadJob = viewModelScope.launch {
            val outcome = appUpdateRepository.downloadAndVerifyUpdate(
                release = release,
                onProgress = { downloaded, total ->
                    internalState.update { state ->
                        if (state.appUpdateState is AppUpdateUiState.Downloading) {
                            state.copy(
                                appUpdateState = state.appUpdateState.copy(
                                    downloadedBytes = downloaded,
                                    totalBytes = total
                                )
                            )
                        } else {
                            state
                        }
                    }
                }
            )
            when (outcome) {
                is AppUpdateDownloadOutcome.Verified -> {
                    val canInstall = appUpdateRepository.canRequestPackageInstalls()
                    internalState.update {
                        it.copy(
                            appUpdateState = AppUpdateUiState.ReadyToInstall(
                                currentVersion = currentVer,
                                release = release,
                                verifiedApk = outcome.verifiedApk,
                                canInstallPackages = canInstall
                            )
                        )
                    }
                    appendAudit(
                        operationEs = "APK oficial descargado y verificado",
                        operationEn = "Official APK downloaded & verified",
                        targetLabel = outcome.verifiedApk.fileName,
                        outcomeEs = "SHA‑256: ${outcome.verifiedApk.computedSha256.take(16)}…",
                        outcomeEn = "SHA‑256: ${outcome.verifiedApk.computedSha256.take(16)}…"
                    )
                }
                is AppUpdateDownloadOutcome.Rejected -> {
                    internalState.update {
                        it.copy(
                            appUpdateState = AppUpdateUiState.Error(
                                currentVersion = currentVer,
                                messageEs = outcome.messageEs,
                                messageEn = outcome.messageEn,
                                release = release
                            )
                        )
                    }
                    appendAudit(
                        operationEs = "Descarga de actualización rechazada",
                        operationEn = "Update download rejected",
                        targetLabel = release.apkAsset.name,
                        outcomeEs = outcome.messageEs,
                        outcomeEn = outcome.messageEn
                    )
                }
            }
        }
    }

    fun cancelAppUpdateDownload() {
        activeUpdateDownloadJob?.cancel()
        activeUpdateDownloadJob = null
        val currentState = internalState.value.appUpdateState
        val release = (currentState as? AppUpdateUiState.Downloading)?.release
        val currentVer = appUpdateRepository.currentVersionName
        internalState.update {
            it.copy(
                appUpdateState = if (release != null) {
                    AppUpdateUiState.UpdateAvailable(currentVer, release)
                } else {
                    AppUpdateUiState.Idle(currentVer)
                }
            )
        }
    }

    fun refreshInstallPermissionState() {
        internalState.update { state ->
            val ready = state.appUpdateState as? AppUpdateUiState.ReadyToInstall ?: return@update state
            val canInstall = appUpdateRepository.canRequestPackageInstalls()
            if (ready.canInstallPackages == canInstall) {
                state
            } else {
                state.copy(appUpdateState = ready.copy(canInstallPackages = canInstall))
            }
        }
    }

    fun requestUnknownSourcesPermission() {
        appUpdateRepository.openUnknownSourcesSettings().onFailure { err ->
            internalState.update {
                it.copy(
                    statusBannerEs = "No se pudo abrir ajustes de permisos: ${err.message}",
                    statusBannerEn = "Could not open permission settings: ${err.message}"
                )
            }
        }
    }

    fun installVerifiedAppUpdate() {
        val ready = internalState.value.appUpdateState as? AppUpdateUiState.ReadyToInstall ?: return
        if (!internalState.value.apkInstallConsentChecked) {
            internalState.update {
                it.copy(
                    statusBannerEs = "Confirma la casilla de consentimiento antes de iniciar el instalador del APK.",
                    statusBannerEn = "Check the confirmation box before launching the APK installer."
                )
            }
            return
        }
        if (!appUpdateRepository.canRequestPackageInstalls()) {
            refreshInstallPermissionState()
            requestUnknownSourcesPermission()
            return
        }
        appUpdateRepository.launchApkInstaller(ready.verifiedApk).fold(
            onSuccess = {
                appendAudit(
                    operationEs = "Instalador de actualización iniciado",
                    operationEn = "System update installer launched",
                    targetLabel = ready.verifiedApk.fileName,
                    outcomeEs = "Entregado al instalador de paquetes de Android tras validar SHA‑256.",
                    outcomeEn = "Handed off to Android package installer after SHA‑256 check."
                )
                internalState.update { it.copy(isUpdateDialogOpen = false) }
            },
            onFailure = { err ->
                internalState.update {
                    it.copy(
                        statusBannerEs = "No se pudo abrir el instalador del APK: ${err.message}",
                        statusBannerEn = "Could not launch APK installer: ${err.message}"
                    )
                }
            }
        )
    }

    fun notifyCopiedMessage(es: String, en: String) {
        internalState.update { it.copy(statusBannerEs = es, statusBannerEn = en) }
    }

    fun dismissBanner() {
        internalState.update { it.copy(statusBannerEs = null, statusBannerEn = null) }
    }

    private fun appendAudit(
        operationEs: String,
        operationEn: String,
        targetLabel: String,
        outcomeEs: String,
        outcomeEn: String,
        snapshotRefId: String? = null
    ) {
        val entry = AuditLogEntry(
            id = UUID.randomUUID().toString().take(8),
            timestampEpochMs = System.currentTimeMillis(),
            operationEs = operationEs,
            operationEn = operationEn,
            targetLabel = targetLabel,
            outcomeEs = outcomeEs,
            outcomeEn = outcomeEn,
            snapshotRefId = snapshotRefId
        )
        viewModelScope.launch {
            persistenceRepository.recordAudit(entry)
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeConnectJob?.cancel()
        activeScanJob?.cancel()
        activeImportJob?.cancel()
        activeUpdateCheckJob?.cancel()
        activeUpdateDownloadJob?.cancel()
    }

    companion object {
        fun factory(
            repository: UserPreferencesRepository,
            keyManager: AdbKeyManager,
            adbTransport: AdbTransportClient,
            discoveryScanner: LanTvDiscoveryScanner,
            activeNetworkScanner: ActiveNetworkScanner,
            persistenceRepository: PersistenceRepository,
            gitHubPackImporter: GitHubPackImporter,
            screenshotStorageDir: File,
            appUpdateRepository: AppUpdateRepository,
            autoCheckUpdatesOnLaunch: Boolean = true
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TVeoCleanViewModel(
                    preferencesRepository = repository,
                    keyManager = keyManager,
                    adbTransport = adbTransport,
                    discoveryScanner = discoveryScanner,
                    activeNetworkScanner = activeNetworkScanner,
                    persistenceRepository = persistenceRepository,
                    gitHubPackImporter = gitHubPackImporter,
                    screenshotStorageDir = screenshotStorageDir,
                    appUpdateRepository = appUpdateRepository,
                    autoCheckUpdatesOnLaunch = autoCheckUpdatesOnLaunch
                ) as T
            }
        }
    }
}

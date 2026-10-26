package com.example.feature.connect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.adb.DiscoveredLanTv
import com.example.core.adb.LanScanDeviceHit
import com.example.core.engine.ChangePlan
import com.example.core.engine.InspectedPackageItem
import com.example.core.engine.PackageClassification
import com.example.core.engine.ProtectedPackageRegistry
import com.example.core.persistence.SavedTvDeviceEntity
import com.example.core.platform.CapabilitySupportLevel
import com.example.core.platform.OperationLifecycleState
import com.example.core.platform.PlatformCapabilityMatrix
import com.example.ui.AdbConnectionBanner
import com.example.ui.ConnectionDiscoveryMode
import com.example.ui.PairingGuideModalState
import com.example.ui.ReportPackageFilter
import com.example.ui.TVeoCleanUiState
import com.example.ui.design.CalmCollapsibleCard
import com.example.ui.design.CalmSectionCard
import com.example.ui.design.ProtectedBadgePill
import com.example.ui.design.ScreenHeaderBlock
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalTVeoExtendedColors

@Composable
fun MyTvConnectScreen(
    uiState: TVeoCleanUiState,
    onSelectConnectionMode: (ConnectionDiscoveryMode) -> Unit,
    onSubnetPrefixChange: (String) -> Unit,
    onStartNetworkScan: () -> Unit,
    onStopNetworkScan: () -> Unit,
    onConnectScannedDevice: (LanScanDeviceHit) -> Unit,
    onConnectSavedDevice: (SavedTvDeviceEntity) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onSelectDiscoveredTv: (DiscoveredLanTv) -> Unit,
    onConnectAndInspect: () -> Unit,
    onCancelConnection: () -> Unit,
    onDisconnectAdb: () -> Unit,
    onReRunReadOnlyInspection: () -> Unit,
    onOpenRemoteTools: () -> Unit,
    onOpenPrivacyReport: () -> Unit,
    onSelectReportFilter: (ReportPackageFilter) -> Unit,
    onTogglePackageForPlan: (String) -> Unit,
    onSelectRecommendedPackages: () -> Unit,
    onClearPlanSelection: () -> Unit,
    onOpenConfirmPlanDialog: () -> Unit,
    onDismissConfirmPlanDialog: () -> Unit,
    onExecuteConfirmedPlan: () -> Unit,
    onRevokeAdbKey: () -> Unit,
    onOpenPairingGuide: () -> Unit,
    onClosePairingGuide: () -> Unit,
    onUpdatePairingPort: (String) -> Unit,
    onUpdatePairingPin: (String) -> Unit,
    onCheckPairingInputs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = uiState.preferences.language
    val extendedColors = LocalTVeoExtendedColors.current
    val isConnectingOrBusy = uiState.operationState == OperationLifecycleState.CONNECTING ||
        uiState.operationState == OperationLifecycleState.ANALYZING ||
        uiState.operationState == OperationLifecycleState.APPLYING

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_my_tv")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .testTag("my_tv_lazy_list")
        ) {
            item {
                ScreenHeaderBlock(
                    title = strings.myTvTitle,
                    subtitle = strings.myTvSubtitle,
                    trailingContent = {
                        OutlinedButton(
                            onClick = onOpenPairingGuide,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("open_pairing_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.myTvPairingGuideButton,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                )
            }

            // Calm, subtle 1-line LAN privacy pill instead of a heavy warning block
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.myTvLanPrivatePill,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Segmented Discovery Mode Switcher: Automatic Wi-Fi Scanner vs Manual IP
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConnectionModeSegmentButton(
                        label = strings.myTvModeAutoTab,
                        selected = uiState.connectionMode == ConnectionDiscoveryMode.AUTOMATIC_SCANNER,
                        onClick = { onSelectConnectionMode(ConnectionDiscoveryMode.AUTOMATIC_SCANNER) },
                        testTag = "mode_tab_auto",
                        modifier = Modifier.weight(1f)
                    )
                    ConnectionModeSegmentButton(
                        label = strings.myTvModeManualTab,
                        selected = uiState.connectionMode == ConnectionDiscoveryMode.MANUAL_IP,
                        onClick = { onSelectConnectionMode(ConnectionDiscoveryMode.MANUAL_IP) },
                        testTag = "mode_tab_manual",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // AUTOMATIC WI-FI SUBNET SCANNER + LIVE mDNS UNIFIED CARD
            if (uiState.connectionMode == ConnectionDiscoveryMode.AUTOMATIC_SCANNER) {
                item {
                    val scan = uiState.networkScanProgress
                    CalmSectionCard(modifier = Modifier.testTag("auto_network_scanner_card")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.WifiFind,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.myTvAutoScanTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = strings.myTvAutoScanDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.customSubnetPrefixInput,
                            onValueChange = onSubnetPrefixChange,
                            enabled = !scan.isScanning,
                            label = {
                                Text(
                                    text = strings.myTvSubplotPrefixLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subnet_prefix_input")
                        )

                        if (scan.isScanning) {
                            OutlinedButton(
                                onClick = onStopNetworkScan,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("stop_network_scan_button")
                            ) {
                                Text(strings.myTvStopNetworkScanButton, style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Button(
                                onClick = onStartNetworkScan,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("start_network_scan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Radar,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.myTvStartNetworkScanButton, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (scan.isScanning || scan.scannedHosts > 0) {
                            LinearProgressIndicator(
                                progress = { scan.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = strings.myTvScanProgressLabel(
                                    scan.subnetPrefix ?: uiState.customSubnetPrefixInput.ifBlank { "192.168.1" },
                                    scan.scannedHosts,
                                    scan.totalHosts
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        scan.localizedError(lang)?.let { scanErr ->
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = scanErr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (scan.discoveredHosts.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                scan.discoveredHosts.forEach { hit ->
                                    ScannedTvDeviceRow(
                                        hit = hit,
                                        onConnect = { onConnectScannedDevice(hit) }
                                    )
                                }
                            }
                        }

                        // Integrated mDNS live devices when announced (zero empty-box noise)
                        if (uiState.discoveredDevices.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            Text(
                                text = strings.myTvDiscoveryTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.discoveredDevices.forEach { tv ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tv.serviceName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${tv.hostAddress}:${tv.port}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            OutlinedButton(
                                                onClick = { onSelectDiscoveredTv(tv) },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(38.dp)
                                            ) {
                                                Text(strings.myTvUseDeviceButton, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // MANUAL IP / DIRECT HOST MODE
                item {
                    CalmSectionCard(modifier = Modifier.testTag("manual_adb_connect_card")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lan,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.myTvManualConnectTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = strings.myTvConnectNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.manualHostInput,
                            onValueChange = onHostChange,
                            enabled = !isConnectingOrBusy,
                            label = {
                                Text(
                                    text = strings.myTvHostLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lan_host_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.manualPortInput,
                                onValueChange = onPortChange,
                                enabled = !isConnectingOrBusy,
                                label = {
                                    Text(
                                        text = strings.myTvPortLabel,
                                        maxLines = 1
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .width(124.dp)
                                    .testTag("lan_port_input")
                            )

                            if (isConnectingOrBusy) {
                                OutlinedButton(
                                    onClick = onCancelConnection,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .padding(top = 6.dp)
                                        .testTag("cancel_adb_connect_button")
                                ) {
                                    Text(
                                        text = strings.myTvCancelConnectButton,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onConnectAndInspect,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .padding(top = 6.dp)
                                        .testTag("connect_adb_button")
                                ) {
                                    Text(
                                        text = strings.myTvConnectLiveButton,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Saved / Previously Linked TVs in Room Database
            if (uiState.savedTvDevices.isNotEmpty() && !uiState.isAdbConnected) {
                item {
                    CalmSectionCard(modifier = Modifier.testTag("saved_tv_devices_card")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.myTvSavedDevicesTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.savedTvDevices.forEach { saved ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = saved.friendlyName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${saved.endpoint} · Android ${saved.androidVersion}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onConnectSavedDevice(saved) },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Text(strings.myTvConnectOneTap, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Active Session Controls & Connection Feedback Banner
            if (uiState.isAdbConnected || uiState.adbBanner !is AdbConnectionBanner.Idle) {
                item {
                    CalmSectionCard(modifier = Modifier.testTag("active_adb_session_card")) {
                        when (val banner = uiState.adbBanner) {
                            AdbConnectionBanner.Idle -> Unit
                            is AdbConnectionBanner.ConnectedSession -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("adb_feedback_connected")
                                ) {
                                    Text(
                                        text = "${strings.statusConnected}: ${banner.endpoint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            is AdbConnectionBanner.Error -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("lan_feedback_error")
                                ) {
                                    Text(
                                        text = banner.localized(lang),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.isAdbConnected) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = onOpenRemoteTools,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("open_remote_tools_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SettingsRemote,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.myTvOpenRemoteToolsButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = onOpenPrivacyReport,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("open_privacy_report_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PrivacyTip,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.myTvOpenPrivacyReportButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = onReRunReadOnlyInspection,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("rerun_readonly_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.myTvRunReadOnlyButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = onDisconnectAdb,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("disconnect_adb_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LinkOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.myTvDisconnectButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Inventory & Reversible Clean Plan Card (Primary Functional Focus)
            val report = uiState.latestReadOnlyReport
            if (report != null) {
                item {
                    CalmSectionCard(modifier = Modifier.testTag("readonly_report_card")) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("readonly_report_header")
                        ) {
                            Text(
                                text = strings.reportSectionTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            ProtectedBadgePill()
                        }

                        Text(
                            text = strings.reportDeviceHeader(
                                report.manufacturer,
                                report.model,
                                report.androidVersion,
                                report.sdkInt
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (uiState.preferences.showAdvancedAdbDetails) {
                            Text(
                                text = strings.reportActiveHomeLabel(report.currentHomePackage),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.reportZeroChangesGuarantee,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onSelectRecommendedPackages,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("select_recommended_packages_button")
                            ) {
                                Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.reportSelectRecommendedButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (uiState.selectedPackagesForPlan.isNotEmpty()) {
                                TextButton(
                                    onClick = onClearPlanSelection,
                                    modifier = Modifier
                                        .height(42.dp)
                                        .testTag("clear_plan_selection_button")
                                ) {
                                    Text(strings.reportClearSelectionButton, maxLines = 1)
                                }
                            }
                        }

                        if (uiState.selectedPackagesForPlan.isNotEmpty()) {
                            Button(
                                onClick = onOpenConfirmPlanDialog,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("review_reversible_plan_button")
                            ) {
                                Text(
                                    text = strings.reportReviewPlanButton(uiState.selectedPackagesForPlan.size),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedReportFilter == ReportPackageFilter.ALL,
                                    onClick = { onSelectReportFilter(ReportPackageFilter.ALL) },
                                    label = { Text("${strings.reportFilterAll} (${report.totalPackagesCount})") },
                                    modifier = Modifier.height(36.dp)
                                )
                            }
                            item {
                                val recCount = report.inspectedPackages.count {
                                    it.classification == PackageClassification.KNOWN_SAFE_RECOMMENDATION
                                }
                                FilterChip(
                                    selected = uiState.selectedReportFilter == ReportPackageFilter.RECOMMENDED,
                                    onClick = { onSelectReportFilter(ReportPackageFilter.RECOMMENDED) },
                                    label = { Text("${strings.reportFilterRecommended} ($recCount)") },
                                    modifier = Modifier.height(36.dp)
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.selectedReportFilter == ReportPackageFilter.PROTECTED,
                                    onClick = { onSelectReportFilter(ReportPackageFilter.PROTECTED) },
                                    label = { Text("${strings.reportFilterProtected} (${report.protectedObservedCount})") },
                                    modifier = Modifier.height(36.dp)
                                )
                            }
                            item {
                                val unknownCount = report.inspectedPackages.count {
                                    it.classification == PackageClassification.UNKNOWN_CONSERVATIVE
                                }
                                FilterChip(
                                    selected = uiState.selectedReportFilter == ReportPackageFilter.CONSERVATIVE,
                                    onClick = { onSelectReportFilter(ReportPackageFilter.CONSERVATIVE) },
                                    label = { Text("${strings.reportFilterUnknown} ($unknownCount)") },
                                    modifier = Modifier.height(36.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.filteredReportPackages.forEach { pkg ->
                                val isGuarded = pkg.classification == PackageClassification.PROTECTED_CRITICAL ||
                                    pkg.packageName == report.currentHomePackage
                                InspectedPackageCard(
                                    pkg = pkg,
                                    isGuarded = isGuarded,
                                    isSelectedForPlan = pkg.packageName in uiState.selectedPackagesForPlan,
                                    onToggleSelection = { onTogglePackageForPlan(pkg.packageName) },
                                    lang = lang,
                                    showTechnical = uiState.preferences.showAdvancedAdbDetails
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Security & Keystore Section (Accessible without cluttering the screen)
            item {
                CalmCollapsibleCard(
                    title = strings.myTvSecuritySectionTitle,
                    subtitle = strings.myTvSecuritySectionSubtitle,
                    icon = Icons.Filled.Security,
                    headerTestTag = "security_collapsible_header",
                    modifier = Modifier.testTag("keystore_identity_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.myTvKeystoreTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = strings.myTvKeystoreFingerprint(uiState.keystorePublicFingerprint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onRevokeAdbKey,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("revoke_keystore_key_button")
                    ) {
                        Text(strings.myTvRevokeKeyButton, maxLines = 1)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Text(
                        text = strings.myTvProtectedGuardTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.myTvProtectedGuardDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.preferences.showAdvancedAdbDetails) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            ProtectedPackageRegistry.allEntries().take(6).forEach { entry ->
                                Text(
                                    text = "• ${entry.packageName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Platform Compatibility Matrix (Clean progressive disclosure)
            item {
                CalmCollapsibleCard(
                    title = strings.myTvPlatformMatrixTitle,
                    subtitle = strings.myTvPlatformMatrixSubtitle,
                    icon = Icons.Filled.Devices,
                    headerTestTag = "platform_matrix_collapsible_header",
                    modifier = Modifier.testTag("platform_matrix_section")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.platformMatrices.forEach { matrix ->
                            PlatformMatrixCompactRow(matrix = matrix, lang = lang)
                        }
                    }
                }
            }
        }
    }

    // Explicit Plan Confirmation Dialog (Mandatory before executing any batch mutation)
    val pendingPlan = uiState.pendingConfirmationPlan
    if (pendingPlan != null) {
        ConfirmReversiblePlanDialog(
            plan = pendingPlan,
            lang = lang,
            onConfirm = onExecuteConfirmedPlan,
            onDismiss = onDismissConfirmPlanDialog
        )
    }

    val pairingModal = uiState.pairingGuideState
    if (pairingModal is PairingGuideModalState.Visible) {
        AlertDialog(
            onDismissRequest = onClosePairingGuide,
            title = { Text(strings.pairingDialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.pairingDialogGuideText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = pairingModal.pairingPort,
                            onValueChange = onUpdatePairingPort,
                            label = { Text(strings.pairingPortLabel, maxLines = 1) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pairingModal.sixDigitPin,
                            onValueChange = onUpdatePairingPin,
                            label = { Text(strings.pairingPinLabel, maxLines = 1) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pairing_pin_input")
                        )
                    }
                    Surface(
                        color = extendedColors.warningContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.pairingLimitationNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    pairingModal.localizedFeedback(lang)?.let { fb ->
                        Surface(
                            color = if (pairingModal.isError) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = fb,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (pairingModal.isError) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onCheckPairingInputs,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.pairingCheckButton)
                }
            },
            dismissButton = {
                TextButton(onClick = onClosePairingGuide) {
                    Text(strings.dialogClose)
                }
            }
        )
    }
}

@Composable
private fun ConnectionModeSegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp)
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScannedTvDeviceRow(
    hit: LanScanDeviceHit,
    onConnect: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (hit.isAdbPortOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanned_tv_item_${hit.ipAddress}")
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tv,
                    contentDescription = null,
                    tint = if (hit.isAdbPortOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = hit.friendlyName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append("${hit.ipAddress}:${hit.adbPort}")
                            hit.modelHint?.let { append(" · $it") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (hit.isAdbPortOpen) strings.myTvBadgeAdbOpen else strings.myTvBadgeCastOnly,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hit.isAdbPortOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("scan_hit_connect_${hit.ipAddress}")
            ) {
                Text(strings.myTvConnectOneTap, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ConfirmReversiblePlanDialog(
    plan: ChangePlan,
    lang: AppLanguage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(strings.planDialogTitle, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("confirm_plan_dialog")
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.planDialogSafetyNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                plan.items.forEach { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = item.localizedTitle(lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = item.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.localizedRisk(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_execute_plan_button")
            ) {
                Text(strings.planConfirmExecuteButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.dialogClose)
            }
        }
    )
}

@Composable
private fun InspectedPackageCard(
    pkg: InspectedPackageItem,
    isGuarded: Boolean,
    isSelectedForPlan: Boolean,
    onToggleSelection: () -> Unit,
    lang: AppLanguage,
    showTechnical: Boolean
) {
    val strings = LocalAppStrings.current
    Surface(
        color = if (isSelectedForPlan) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelectedForPlan) 1.5.dp else 1.dp,
            color = if (isSelectedForPlan) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isGuarded) Modifier.clickable(onClick = onToggleSelection) else Modifier
            )
            .testTag("inspected_pkg_${pkg.packageName}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!isGuarded) {
                        Checkbox(
                            checked = isSelectedForPlan,
                            onCheckedChange = { onToggleSelection() },
                            modifier = Modifier.testTag("pkg_checkbox_${pkg.packageName}")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = pkg.localizedTitle(lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (isGuarded) {
                    ProtectedBadgePill()
                } else {
                    Surface(
                        color = if (pkg.classification == PackageClassification.KNOWN_SAFE_RECOMMENDATION) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = pkg.classification.localizedBadge(lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pkg.classification == PackageClassification.KNOWN_SAFE_RECOMMENDATION) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (showTechnical) {
                Text(
                    text = pkg.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = pkg.localizedReason(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showTechnical) {
                Text(
                    text = "${pkg.readEvidence} · ${strings.reportRestoreHint(pkg.restoreMethodCommand)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlatformMatrixCompactRow(
    matrix: PlatformCapabilityMatrix,
    lang: AppLanguage
) {
    val strings = LocalAppStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("platform_card_${matrix.family.name.lowercase()}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = matrix.family.localizedName(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (matrix.family.supportsAdb) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (matrix.family.supportsAdb) strings.badgeAdbLocal else strings.badgeNoAdb,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (matrix.family.supportsAdb) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CapabilityRow(strings.capReadOnlyInspect, matrix.readOnlyInspect, lang)
                CapabilityRow(strings.capReversibleDisable, matrix.reversibleAppDisable, lang)
                CapabilityRow(strings.capStyleStudio, matrix.styleStudioThemes, lang)
            }

            Text(
                text = matrix.localizedNotes(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CapabilityRow(
    label: String,
    support: CapabilitySupportLevel,
    lang: AppLanguage
) {
    val extendedColors = LocalTVeoExtendedColors.current
    val (icon, tint) = when (support) {
        CapabilitySupportLevel.SUPPORTED -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
        CapabilitySupportLevel.CONSERVATIVE_LIMITED -> Icons.Filled.Info to MaterialTheme.colorScheme.secondary
        CapabilitySupportLevel.BLOCKED_FOR_SAFETY -> Icons.Filled.WarningAmber to extendedColors.warning
        CapabilitySupportLevel.UNAVAILABLE -> Icons.Filled.Block to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = support.localizedLabel(lang),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

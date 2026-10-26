package com.example.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.update.AppUpdateReleaseInfo
import com.example.core.update.AppUpdateSecurityConfig
import com.example.core.update.AppUpdateUiState
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import java.util.Locale

@Composable
fun AppUpdateDialog(
    updateState: AppUpdateUiState,
    language: AppLanguage,
    apkConsentChecked: Boolean,
    showTechnicalHashes: Boolean,
    onToggleConsent: (Boolean) -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onGrantInstallPermission: () -> Unit,
    onInstallVerifiedApk: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val uriHandler = LocalUriHandler.current

    val release: AppUpdateReleaseInfo? = when (updateState) {
        is AppUpdateUiState.UpdateAvailable -> updateState.release
        is AppUpdateUiState.Downloading -> updateState.release
        is AppUpdateUiState.ReadyToInstall -> updateState.release
        is AppUpdateUiState.Error -> updateState.release
        else -> null
    }
    if (release == null) return

    val newVersionStr = release.semanticVersion.normalizedString
    val currentVersionStr = updateState.currentVersion

    Dialog(
        onDismissRequest = {
            if (updateState !is AppUpdateUiState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .testTag("app_update_dialog")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.updateDialogTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.updateVersionTransition(currentVersionStr, newVersionStr),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (updateState !is AppUpdateUiState.Downloading) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_update_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = strings.dialogClose
                            )
                        }
                    }
                }

                // Asset badge row
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = strings.updateAssetSummary(
                                fileName = release.apkAsset.name,
                                sizeLabel = release.apkAsset.formattedSizeMb(),
                                dateLabel = release.formattedPublishedDate()
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Scrollable middle body: Changelog + Security verification
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 310.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Release notes
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.updateReleaseNotesTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = release.changelog.ifBlank { strings.updateReleaseNotesEmpty },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // APK Security & Integrity Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.updateSecurityCardTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = strings.updateSecuritySourceLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            when (updateState) {
                                is AppUpdateUiState.ReadyToInstall -> {
                                    val apk = updateState.verifiedApk
                                    val shaShort = if (showTechnicalHashes) apk.computedSha256 else apk.computedSha256.take(16)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (apk.sha256VerifiedAgainstRelease) {
                                                strings.updateSecurityShaReleaseVerified(shaShort)
                                            } else {
                                                strings.updateSecurityShaComputedLocal(shaShort)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (!apk.matchesInstalledPackageId || apk.matchesInstalledSigningCert == false) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Warning,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = strings.updateSecuritySignatureConflictWarning,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Text(
                                        text = strings.updateSecurityShaPendingLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Error notice if a previous download attempt failed
                    if (updateState is AppUpdateUiState.Error) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = updateState.localizedMessage(language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // State-specific Footer Controls
                when (updateState) {
                    is AppUpdateUiState.Downloading -> {
                        val downloadedMb = String.format(
                            Locale.US,
                            "%.1f MB",
                            updateState.downloadedBytes.toDouble() / (1024.0 * 1024.0)
                        )
                        val totalMb = String.format(
                            Locale.US,
                            "%.1f MB",
                            updateState.totalBytes.coerceAtLeast(1L).toDouble() / (1024.0 * 1024.0)
                        )
                        val pct = (updateState.progressFraction * 100f).toInt().coerceIn(0, 100)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.updateDownloadingStatus(downloadedMb, totalMb, pct),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LinearProgressIndicator(
                                progress = { updateState.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                            )
                            OutlinedButton(
                                onClick = onCancelDownload,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("cancel_update_download_button")
                            ) {
                                Text(text = strings.updateCancelDownloadButton)
                            }
                        }
                    }

                    is AppUpdateUiState.ReadyToInstall -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Explicit APK execution consent (unchecked by default)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleConsent(!apkConsentChecked) }
                                    .padding(vertical = 4.dp)
                                    .testTag("apk_install_consent_row")
                            ) {
                                Checkbox(
                                    checked = apkConsentChecked,
                                    onCheckedChange = onToggleConsent,
                                    modifier = Modifier.testTag("apk_install_consent_checkbox")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.updateExplicitApkConsentLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!updateState.canInstallPackages) {
                                Text(
                                    text = strings.updateUnknownSourcesNotice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onGrantInstallPermission,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("grant_install_permission_button")
                                ) {
                                    Text(text = strings.updateAllowInstallSourceButton)
                                }
                            } else {
                                Button(
                                    onClick = onInstallVerifiedApk,
                                    enabled = apkConsentChecked,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("install_verified_apk_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SystemUpdate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = strings.updateInstallNowButton(newVersionStr))
                                }
                            }
                        }
                    }

                    else -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val url = release.releaseHtmlUrl.ifBlank {
                                        AppUpdateSecurityConfig.OFFICIAL_RELEASES_HTML_URL
                                    }
                                    runCatching { uriHandler.openUri(url) }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("open_release_page_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = strings.settingsOpenGitHubReleasesButton,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Button(
                                onClick = onStartDownload,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("download_update_apk_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.updateDownloadButton(release.apkAsset.formattedSizeMb()),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

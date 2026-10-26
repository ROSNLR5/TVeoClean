package com.example.feature.remote

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.adb.TvRemoteKey
import com.example.core.privacy.SignalCertainty
import com.example.core.privacy.TvPrivacyReport
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import java.io.File

@Composable
fun TvRemoteControlDialog(
    isConnected: Boolean,
    latestScreenshotPath: String?,
    isCapturingScreenshot: Boolean,
    onSendKey: (TvRemoteKey) -> Unit,
    onSendSearchText: (String) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onGoToConnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var searchInput by remember { mutableStateOf("") }

    val decodedBitmap = remember(latestScreenshotPath) {
        if (latestScreenshotPath.isNullOrBlank()) null
        else runCatching {
            val f = File(latestScreenshotPath)
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
        }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.remoteDialogTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("remote_control_dialog")
            ) {
                if (!isConnected) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = strings.remoteDisconnectedNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    onDismiss()
                                    onGoToConnect()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text(strings.homeCtaConnectTv)
                            }
                        }
                    }
                } else {
                    // D-Pad Cluster
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onSendKey(TvRemoteKey.DPAD_UP) },
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(52.dp).testTag("remote_btn_up")
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = TvRemoteKey.DPAD_UP.labelEs)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { onSendKey(TvRemoteKey.DPAD_LEFT) },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(52.dp).testTag("remote_btn_left")
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = TvRemoteKey.DPAD_LEFT.labelEs)
                                }
                                Button(
                                    onClick = { onSendKey(TvRemoteKey.DPAD_CENTER) },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(60.dp).testTag("remote_btn_ok")
                                ) {
                                    Text("OK", fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(
                                    onClick = { onSendKey(TvRemoteKey.DPAD_RIGHT) },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(52.dp).testTag("remote_btn_right")
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = TvRemoteKey.DPAD_RIGHT.labelEs)
                                }
                            }
                            FilledTonalButton(
                                onClick = { onSendKey(TvRemoteKey.DPAD_DOWN) },
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(52.dp).testTag("remote_btn_down")
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = TvRemoteKey.DPAD_DOWN.labelEs)
                            }
                        }
                    }

                    // Navigation Row: Back / Home / Menu
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { onSendKey(TvRemoteKey.BACK) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("remote_btn_back")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = TvRemoteKey.BACK.labelEs, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { onSendKey(TvRemoteKey.HOME) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("remote_btn_home")
                        ) {
                            Icon(Icons.Filled.Home, contentDescription = TvRemoteKey.HOME.labelEs, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { onSendKey(TvRemoteKey.MENU) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("remote_btn_menu")
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = TvRemoteKey.MENU.labelEs, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Volume Row: Vol- / Mute / Vol+
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { onSendKey(TvRemoteKey.VOLUME_DOWN) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = TvRemoteKey.VOLUME_DOWN.labelEs, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { onSendKey(TvRemoteKey.MUTE) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = TvRemoteKey.MUTE.labelEs, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { onSendKey(TvRemoteKey.VOLUME_UP) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = TvRemoteKey.VOLUME_UP.labelEs, modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    // Safe Search Text Input
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it.take(80) },
                            label = { Text(strings.remoteSearchInputLabel, maxLines = 1) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (searchInput.isNotBlank()) {
                                    onSendSearchText(searchInput)
                                    searchInput = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text(strings.remoteSendTextButton)
                        }
                    }

                    // Private Screenshot Capture
                    OutlinedButton(
                        onClick = onCaptureScreenshot,
                        enabled = !isCapturingScreenshot,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("remote_btn_screenshot")
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.remoteScreenshotButton, maxLines = 1)
                    }

                    if (decodedBitmap != null) {
                        Text(
                            text = strings.remoteScreenshotSavedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = decodedBitmap,
                                contentDescription = strings.remoteScreenshotSavedLabel,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.dialogClose)
            }
        }
    )
}

@Composable
fun TvPrivacyReportDialog(
    report: TvPrivacyReport?,
    language: AppLanguage,
    onRefresh: () -> Unit,
    onCopiedNotice: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(strings.privacyDialogTitle, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("privacy_report_dialog")
            ) {
                Text(
                    text = strings.privacyDialogSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (report != null) {
                    report.signals.forEach { sig ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
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
                                    Text(
                                        text = sig.localizedTitle(language),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val badgeColor = when (sig.certainty) {
                                        SignalCertainty.VERIFIED_FACT -> MaterialTheme.colorScheme.primaryContainer
                                        SignalCertainty.CONSERVATIVE_OBSERVATION -> MaterialTheme.colorScheme.secondaryContainer
                                        SignalCertainty.NOT_VERIFIABLE_VIA_ADB -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                    Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = sig.certainty.localized(language),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = sig.localizedValue(language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = sig.evidenceSource,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.privacyRefreshButton, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(report.exportRedactedReport(language)))
                                onCopiedNotice()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.privacyCopyRedactedButton, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.dialogClose)
            }
        }
    )
}

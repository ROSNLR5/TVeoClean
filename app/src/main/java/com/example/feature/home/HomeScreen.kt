package com.example.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.update.AppUpdateUiState
import com.example.ui.TVeoCleanUiState
import com.example.ui.design.CalmSectionCard
import com.example.ui.design.ColorSwatchRow
import com.example.ui.design.ScreenHeaderBlock
import com.example.ui.design.StatusPill
import com.example.ui.i18n.LocalAppStrings

@Composable
fun OnboardingOverlay(
    stepIndex: Int,
    onSelectStep: (Int) -> Unit = {},
    onPreviousStep: () -> Unit = {},
    onNextStep: () -> Unit,
    onSkipOrFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val safeStep = stepIndex.coerceIn(0, 2)

    val titles = listOf(
        strings.onboardingStep1Title,
        strings.onboardingStep2Title,
        strings.onboardingStep3Title
    )
    val descriptions = listOf(
        strings.onboardingStep1Desc,
        strings.onboardingStep2Desc,
        strings.onboardingStep3Desc
    )
    val bulletLists = listOf(
        strings.onboardingStep1Bullets,
        strings.onboardingStep2Bullets,
        strings.onboardingStep3Bullets
    )
    val stepIcons = listOf(Icons.Filled.Wifi, Icons.Filled.Shield, Icons.Filled.AutoAwesome)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("onboarding_screen")
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Brand mark + Skip button
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = stepIcons[safeStep],
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.onboardingStepBadge(safeStep + 1, 3),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    TextButton(
                        onClick = onSkipOrFinish,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = strings.onboardingSkip,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Scrollable middle content on very small screens while buttons stay visible
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Friendly Bespoke Visual Scene for Each Step
                    OnboardingStepVisualScene(step = safeStep)

                    // Clear, Warm Copy & Scannable Highlights
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = titles[safeStep],
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = descriptions[safeStep],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3 Friendly Benefit Bullets
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            bulletLists[safeStep].forEach { point ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = point,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tappable Progress Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(3) { idx ->
                        val active = idx == safeStep
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(26.dp)
                                .padding(horizontal = 4.dp)
                                .clickable { onSelectStep(idx) }
                                .testTag("onboarding_dot_$idx")
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (active) 30.dp else 10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                                    )
                            )
                        }
                    }
                }

                // Always-visible Navigation Actions: Back (when step > 0) + Primary Continue / Start
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (safeStep > 0) {
                        OutlinedButton(
                            onClick = onPreviousStep,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.onboardingBack,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.onboardingBack,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (safeStep < 2) onNextStep() else onSkipOrFinish()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("onboarding_primary_button")
                    ) {
                        Text(
                            text = if (safeStep < 2) strings.onboardingNext else strings.onboardingStart,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom, friendly, immediately understandable visual scenes tailored to each of the 3 steps.
 */
@Composable
private fun OnboardingStepVisualScene(step: Int) {
    val strings = LocalAppStrings.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = when (step) {
                        0 -> listOf(Color(0xFF0E1D34), Color(0xFF133346), Color(0xFF0B1324))
                        1 -> listOf(Color(0xFF102038), Color(0xFF173647), Color(0xFF0D1628))
                        else -> listOf(Color(0xFF141D3B), Color(0xFF232554), Color(0xFF0C1528))
                    }
                )
            )
            .border(
                width = 1.dp,
                color = Color(0xFF55E6C1).copy(alpha = 0.32f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        when (step) {
            0 -> {
                // Step 1 Visual: Phone -> Wi-Fi Radar -> Living Room TV
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        color = Color(0x3355E6C1),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0x6655E6C1))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Radar,
                                contentDescription = null,
                                tint = Color(0xFF55E6C1),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.navMyTv,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFF3F6FC)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Phone icon card
                        Surface(
                            color = Color(0xFF162540),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PhoneAndroid,
                                    contentDescription = null,
                                    tint = Color(0xFFF3F6FC),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // Wi-Fi signal bridge
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF55E6C1)))
                            Icon(
                                imageVector = Icons.Filled.Wifi,
                                contentDescription = null,
                                tint = Color(0xFF55E6C1),
                                modifier = Modifier.size(24.dp)
                            )
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF55E6C1)))
                        }

                        // Living Room TV card
                        Surface(
                            color = Color(0xFF162540),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF55E6C1)),
                            modifier = Modifier
                                .width(92.dp)
                                .height(62.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tv,
                                    contentDescription = null,
                                    tint = Color(0xFF55E6C1),
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "Android TV",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF3F6FC)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Wi‑Fi Local · 192.168.1.x",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC5D1E6)
                    )
                }
            }

            1 -> {
                // Step 2 Visual: Optional App Disabled vs Essential System Locked + Remote badge
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color(0x3355E6C1),
                            shape = CircleShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF55E6C1),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.capReversibleDisable,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF3F6FC)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF182A46),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SettingsRemote,
                                    contentDescription = null,
                                    tint = Color(0xFF8585FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.homeActionRemote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD8E2F3)
                                )
                            }
                        }
                    }

                    // Row 1: Promo / Bloatware selected safely
                    Surface(
                        color = Color(0xFF162540),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF55E6C1).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF55E6C1),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Promos & Banners TV",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFF3F6FC)
                                )
                            }
                            Text(
                                text = "Desactivar",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF55E6C1)
                            )
                        }
                    }

                    // Row 2: System / Wi-Fi / Home automatically protected
                    Surface(
                        color = Color(0xFF121C31),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF8585FF),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Wi‑Fi · Mando · Inicio TV",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFC5D1E6)
                                )
                            }
                            Text(
                                text = strings.badgeProtected,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8585FF)
                            )
                        }
                    }
                }
            }

            else -> {
                // Step 3 Visual: Style Studio Mini TV Preview + 1-Tap Undo Restore
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ColorSwatchRow(
                            colors = listOf(
                                Color(0xFF55E6C1),
                                Color(0xFF8585FF),
                                Color(0xFF7CE8FF),
                                Color(0xFF16233D)
                            )
                        )
                        Surface(
                            color = Color(0x3355E6C1),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color(0x6655E6C1))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    tint = Color(0xFF55E6C1),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.navHistory,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF3F6FC)
                                )
                            }
                        }
                    }

                    // Mini TV Grid Preview inside Step 3
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Cine", "Series", "Música", "HDMI").forEachIndexed { idx, label ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF182644))
                                    .border(
                                        width = if (idx == 0) 1.8.dp else 1.dp,
                                        color = if (idx == 0) Color(0xFF55E6C1) else Color.White.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF3F6FC)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${strings.navStudio} + ${strings.homeActionRestore}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD8E2F3),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    uiState: TVeoCleanUiState,
    onNavigateToConnect: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenRemoteControl: () -> Unit,
    onOpenUpdateDialog: () -> Unit = {},
    onDismissUpdateBanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val report = uiState.latestReadOnlyReport
    val activeUpdateRelease = when (val upd = uiState.appUpdateState) {
        is AppUpdateUiState.UpdateAvailable -> upd.release
        is AppUpdateUiState.Downloading -> upd.release
        is AppUpdateUiState.ReadyToInstall -> upd.release
        else -> null
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_home")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
        ) {
            item {
                ScreenHeaderBlock(
                    title = strings.appName,
                    subtitle = strings.appTagline,
                    trailingContent = {
                        StatusPill(
                            label = if (uiState.isAdbConnected) strings.statusConnected else strings.statusDisconnected,
                            icon = Icons.Filled.LinkOff,
                            isConnected = uiState.isAdbConnected
                        )
                    }
                )
            }

            if (activeUpdateRelease != null && !uiState.isHomeUpdateBannerDismissed) {
                item {
                    val isReady = uiState.appUpdateState is AppUpdateUiState.ReadyToInstall
                    val newVer = activeUpdateRelease.semanticVersion.normalizedString
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_update_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.SystemUpdate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.homeUpdateBannerTitle(newVer),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.homeUpdateBannerSubtitle(
                                        currentVersion = uiState.appVersionName,
                                        sizeLabel = activeUpdateRelease.apkAsset.formattedSizeMb()
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = onOpenUpdateDialog,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("home_update_cta_button")
                            ) {
                                Text(
                                    text = if (isReady) strings.homeUpdateReadyButton else strings.homeUpdateBannerButton,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            IconButton(
                                onClick = onDismissUpdateBanner,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("home_update_dismiss_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = strings.dialogClose,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Clean, Uncluttered 16:9 Hero TV Card (without noisy package-count overlay)
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_hero_card")
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_tv_hero_calm_1793032441047),
                                contentDescription = strings.homeHeroDisconnectedTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x100B1020),
                                                Color(0xCC0B1020)
                                            )
                                        )
                                    )
                            )
                            Surface(
                                color = Color(0xAA0B1020),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0x5555E6C1)),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isAdbConnected) Icons.Filled.CheckCircle else Icons.Filled.Wifi,
                                        contentDescription = null,
                                        tint = Color(0xFF55E6C1),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.isAdbConnected) {
                                            strings.homeHeroReadyPillOnline
                                        } else {
                                            strings.homeHeroReadyPillOffline
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFF3F6FC),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(18.dp)
                        ) {
                            if (uiState.isAdbConnected && report != null) {
                                Text(
                                    text = strings.homeHeroConnectedTitle(report.manufacturer, report.model),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.homeHeroConnectedSubtitle(report.endpoint, report.androidVersion),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = strings.homeHeroDisconnectedTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.homeHeroDisconnectedBody,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Button(
                                onClick = onNavigateToConnect,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("home_connect_cta_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tv,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isAdbConnected) strings.homeCtaAnalyzeNow else strings.homeCtaConnectTv,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Clean, Proportionate 2x2 Quick Actions Grid — Functionality First
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.homeQuickActionsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionTile(
                            title = strings.homeActionAnalyze,
                            subtitle = if (uiState.isAdbConnected) strings.homeActionAnalyzeSubOnline else strings.homeActionAnalyzeSubOffline,
                            icon = Icons.Filled.Search,
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToConnect,
                            testTag = "quick_action_analyze",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            title = strings.homeActionStudio,
                            subtitle = strings.homeActionStudioSub,
                            icon = Icons.Filled.AutoAwesome,
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToStudio,
                            testTag = "quick_action_studio",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionTile(
                            title = strings.homeActionRestore,
                            subtitle = strings.homeActionRestoreSub(uiState.snapshots.size),
                            icon = Icons.Filled.History,
                            accent = MaterialTheme.colorScheme.secondary,
                            onClick = onNavigateToHistory,
                            testTag = "quick_action_restore",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            title = strings.homeActionRemote,
                            subtitle = if (uiState.isAdbConnected) strings.homeActionRemoteSubOnline else strings.homeActionRemoteSubOffline,
                            icon = Icons.Filled.SettingsRemote,
                            accent = if (uiState.isAdbConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            onClick = onOpenRemoteControl,
                            testTag = "quick_action_remote",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Concise Status Summary Card
            item {
                CalmSectionCard {
                    Text(
                        text = strings.homeVerifiedStateTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (report != null) {
                            strings.homeVerifiedReportSummary(
                                report.totalPackagesCount,
                                report.enabledPackagesCount,
                                report.disabledPackagesCount
                            )
                        } else {
                            strings.homeVerifiedEmpty
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val activeMobile = uiState.activeMobileOverride
                    if (activeMobile != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.homeActiveMobileThemeBanner(activeMobile.packName),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .heightIn(min = 122.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 122.dp)
                .padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

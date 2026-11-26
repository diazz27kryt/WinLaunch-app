/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.guide.guideNode
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.GameSettings
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.rememberAnimatedContext
import com.movtery.zalithlauncher.ui.guide.GuideKeys
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CommonVersionInfoLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import com.movtery.zalithlauncher.ui.theme.GlassCard
import com.movtery.zalithlauncher.ui.theme.GlassCardShape
import com.movtery.zalithlauncher.ui.theme.GlassPillShape
import com.movtery.zalithlauncher.ui.theme.WinLaunchGreen
import com.movtery.zalithlauncher.ui.theme.glass
import com.movtery.zalithlauncher.ui.theme.glassBorder
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/**
 * Главный экран WinLaunch: карточка запуска, баннер и нижний ряд блоков.
 */
@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
    @Suppress("UNUSED_PARAMETER") onQuickPlayServer: (Version?, String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenLink: (String) -> Unit,
    startGuideOnce: (GuideKeys.Keys) -> Unit,
) {
    LaunchedEffect(Unit) {
        startGuideOnce(GuideKeys.Main)
    }

    val toAccountManageScreen: () -> Unit = {
        backStackViewModel.mainScreen.navigateTo(
            screenKey = NormalNavKey.AccountManager(FirstLoginMenu.NONE)
        )
    }
    val toVersionManageScreen: () -> Unit = {
        backStackViewModel.mainScreen.removeAndNavigateTo(
            remove = NestedNavKey.VersionSettings::class,
            screenKey = NormalNavKey.VersionsManager
        )
    }
    val toVersionSettingsScreen: () -> Unit = {
        VersionsManager.currentVersion.value?.let(navigateToVersions)
    }
    val toDownload: (TitledNavKey) -> Unit = { target ->
        backStackViewModel.navigateToDownload(target)
    }

    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        val ctx = rememberAnimatedContext(isVisible = isVisible)
        WinLaunchDashboard(
            modifier = Modifier
                .fillMaxSize()
                .animated(ctx)
                .padding(12.dp),
            backStackViewModel = backStackViewModel,
            onOpenDownload = toDownload,
            onOpenAccount = toAccountManageScreen,
            onOpenVersions = toVersionManageScreen,
            onOpenVersionSettings = toVersionSettingsScreen,
            onLaunchGame = onLaunchGame,
        )
    }
}

/**
 * Главная панель WinLaunch — отдельная компоновка с центральной зоной запуска,
 * быстрым доступом к библиотеке и краткой информацией о конфигурации.
 */
@Composable
private fun WinLaunchDashboard(
    modifier: Modifier = Modifier,
    backStackViewModel: ScreenBackStackViewModel,
    onOpenDownload: (TitledNavKey) -> Unit,
    onOpenAccount: () -> Unit,
    onOpenVersions: () -> Unit,
    onOpenVersionSettings: () -> Unit,
    onLaunchGame: (Version?) -> Unit,
) {
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()
    val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val isReady = !isRefreshing && currentVersion != null && currentVersion.isValid()
    val ramMb = GameSettings.ramAllocation.state
    val ramText = when {
        ramMb == null -> "—"
        ramMb % 1024 == 0 -> "${ramMb / 1024} GB"
        else -> "${ramMb} MB"
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF4F5F7), Color(0xFF9299A5))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "W",
                        color = Color(0xFF111318),
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = "WINLAUNCH",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "GAME CONTROL CENTER",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        letterSpacing = 1.6.sp
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.045f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isReady) Color(0xFF75E0AE) else Color(0xFFFFC46B))
                )
                Text(
                    text = if (isRefreshing) stringResource(R.string.home_status_refreshing)
                    else if (isReady) stringResource(R.string.home_status_ready)
                    else stringResource(R.string.home_status_invalid),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Основная зона запуска
            Box(
                modifier = Modifier
                    .weight(1.55f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF20252E),
                                Color(0xFF12161D),
                                Color(0xFF0B0D12)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(26.dp))
                    .guideNode(
                        key = GuideKeys.Main.Step.CardDrag,
                        holeShape = RoundedCornerShape(26.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(250.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFBFCBDE).copy(alpha = 0.16f),
                                    Color(0xFF6D88B0).copy(alpha = 0.035f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(178.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 48.dp)
                        .size(130.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 78.dp),
                    text = "W",
                    color = Color.White.copy(alpha = 0.075f),
                    fontSize = 120.sp,
                    lineHeight = 120.sp,
                    fontWeight = FontWeight.Black
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "YOUR WORLD STARTS HERE",
                            color = Color(0xFFAAB4C4),
                            fontSize = 9.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "MINECRAFT",
                            color = Color(0xFFF5F6F8),
                            fontSize = 30.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Java Edition  /  персональная игровая среда",
                            color = Color(0xFF9CA6B5),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var showVersions by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ВЕРСИЯ",
                                color = Color(0xFF8F9AAA),
                                fontSize = 8.sp,
                                letterSpacing = 1.3.sp
                            )
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(Color.White.copy(alpha = 0.075f))
                                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
                                        .clickable {
                                            if (versions.isEmpty()) onOpenVersions() else showVersions = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_box),
                                        contentDescription = null,
                                        tint = Color(0xFFE2E7EF),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = currentVersion?.getVersionName()
                                            ?: stringResource(R.string.versions_manage_install_new),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                                        contentDescription = null,
                                        tint = Color(0xFF9CA6B5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showVersions && versions.isNotEmpty(),
                                    onDismissRequest = { showVersions = false }
                                ) {
                                    versions.forEach { item ->
                                        DropdownMenuItem(
                                            text = {
                                                CommonVersionInfoLayout(
                                                    version = item,
                                                    iconSize = 26.dp
                                                )
                                            },
                                            onClick = {
                                                if (item != currentVersion) VersionsManager.saveVersion(item)
                                                showVersions = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.home_manage_versions)) },
                                        onClick = {
                                            showVersions = false
                                            onOpenVersions()
                                        }
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "АККАУНТ",
                                color = Color(0xFF8F9AAA),
                                fontSize = 8.sp,
                                letterSpacing = 1.3.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color.White.copy(alpha = 0.075f))
                                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
                                    .clickable(onClick = onOpenAccount)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                if (account != null) {
                                    PlayerFace(account = account!!, avatarSize = 22.dp)
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_person_outlined),
                                        contentDescription = null,
                                        tint = Color(0xFFE2E7EF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = account?.username ?: stringResource(R.string.account_add_new_account),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFF0F2F5), Color(0xFFC1C8D2))
                                )
                            )
                            .clickable(enabled = !isRefreshing) {
                                if (currentVersion == null) onOpenVersions() else onLaunchGame(null)
                            }
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_arrow_filled),
                            contentDescription = null,
                            tint = Color(0xFF11151B),
                            modifier = Modifier.size(23.dp)
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = stringResource(R.string.home_play).uppercase(),
                            color = Color(0xFF11151B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            // Правая колонка: библиотека и системная сводка
            Column(
                modifier = Modifier
                    .weight(0.88f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color(0xFF151920))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(23.dp))
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = "БИБЛИОТЕКА",
                        color = Color(0xFFE9EDF3),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        DashboardShortcut(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            icon = R.drawable.ic_star_filled,
                            label = stringResource(R.string.download_category_favorites),
                            onClick = { onOpenDownload(backStackViewModel.downloadFavoritesScreen) }
                        )
                        DashboardShortcut(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            icon = R.drawable.ic_extension_outlined,
                            label = stringResource(R.string.home_quick_modpacks),
                            onClick = { onOpenDownload(backStackViewModel.downloadModPackScreen) }
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        DashboardShortcut(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            icon = R.drawable.ic_format_paint_outlined,
                            label = stringResource(R.string.home_quick_resource_packs),
                            onClick = { onOpenDownload(backStackViewModel.downloadResourcePackScreen) }
                        )
                        DashboardShortcut(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            icon = R.drawable.ic_blur_circular_outlined,
                            label = stringResource(R.string.home_quick_shaders),
                            onClick = { onOpenDownload(backStackViewModel.downloadShadersScreen) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(23.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF202630), Color(0xFF151A22))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(23.dp))
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = "СИСТЕМА",
                        color = Color(0xFFE9EDF3),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    DashboardMetric(
                        name = "Выделено памяти",
                        value = ramText
                    )
                    DashboardMetric(
                        name = "Установлено версий",
                        value = versions.size.toString()
                    )
                    DashboardMetric(
                        name = "Текущий профиль",
                        value = currentVersion?.getVersionName() ?: "Не выбран"
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardShortcut(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.075f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color(0xFFE2E8F0),
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = label,
            color = Color(0xFFCFD6E0),
            fontSize = 9.sp,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DashboardMetric(
    name: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = Color(0xFF9BA6B5),
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = Color(0xFFF1F3F6),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Карточка запуска: статус, версия, аккаунт и кнопка «Играть».
 */
@Composable
private fun LaunchCard(
    onLaunchGame: (Version?) -> Unit,
    toAccountManageScreen: () -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    val currentVersion = version
    val isReady = !isRefreshing && currentVersion != null && currentVersion.isValid()

    GlassCard(
        modifier = modifier.guideNode(
            key = GuideKeys.Main.Step.CardDrag,
            holeShape = GlassCardShape,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusChip(
                text = when {
                    isRefreshing -> stringResource(R.string.home_status_refreshing)
                    currentVersion == null -> stringResource(R.string.versions_manage_no_versions)
                    !currentVersion.isValid() -> stringResource(R.string.home_status_invalid)
                    else -> stringResource(R.string.home_status_ready)
                },
                dotColor = when {
                    isReady -> WinLaunchGreen
                    currentVersion != null && !isRefreshing -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VersionIconImage(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = currentVersion != null, onClick = toVersionSettingsScreen),
                    version = currentVersion,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Minecraft",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(stringResource(R.string.home_edition))
                            if (currentVersion != null && currentVersion.isValid()) {
                                append(" • ")
                                append(currentVersion.getVersionSummary())
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Выбор версии
            var showVersions by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.guideNode(
                    key = GuideKeys.Main.Step.VersionList,
                    holeShape = GlassPillShape,
                )
            ) {
                PillRow(
                    onClick = {
                        if (versions.isEmpty()) toVersionManageScreen() else showVersions = true
                    },
                    leading = {
                        Icon(
                            modifier = Modifier.size(22.dp),
                            painter = painterResource(R.drawable.ic_box),
                            contentDescription = null,
                        )
                    },
                    text = currentVersion?.getVersionName()
                        ?: stringResource(R.string.versions_manage_install_new),
                )
                DropdownMenu(
                    expanded = showVersions && versions.isNotEmpty(),
                    onDismissRequest = { showVersions = false },
                ) {
                    versions.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                CommonVersionInfoLayout(
                                    version = item,
                                    iconSize = 28.dp,
                                )
                            },
                            onClick = {
                                if (item != currentVersion) VersionsManager.saveVersion(item)
                                showVersions = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_manage_versions)) },
                        onClick = {
                            showVersions = false
                            toVersionManageScreen()
                        }
                    )
                }
            }

            // Аккаунт
            Box(
                modifier = Modifier.guideNode(
                    key = GuideKeys.Main.Step.Account,
                    holeShape = GlassPillShape,
                )
            ) {
                PillRow(
                    onClick = toAccountManageScreen,
                    leading = {
                        val acc = account
                        if (acc != null) {
                            PlayerFace(
                                account = acc,
                                avatarSize = 24.dp,
                            )
                        } else {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                painter = painterResource(R.drawable.ic_person_outlined),
                                contentDescription = null,
                            )
                        }
                    },
                    text = account?.username ?: stringResource(R.string.account_add_new_account),
                )
            }

            PlayButton(
                enabled = !isRefreshing,
                onClick = {
                    if (currentVersion == null) toVersionManageScreen() else onLaunchGame(null)
                }
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    dotColor: Color,
) {
    Row(
        modifier = Modifier
            .glass(GlassPillShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = dotColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Строка-«таблетка» с иконкой слева и стрелкой справа (выбор версии и аккаунта).
 */
@Composable
private fun PillRow(
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassPillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.55f))
            .glassBorder(GlassPillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading()
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(R.drawable.ic_keyboard_arrow_down),
            contentDescription = null,
        )
    }
}

/**
 * Светлая «серебряная» кнопка запуска.
 */
@Composable
private fun PlayButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = Color(0xFF0B0B0D)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(GlassPillShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF4F4F7), Color(0xFFB4B4BC))
                )
            )
            .glassBorder(GlassPillShape)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(26.dp),
            painter = painterResource(R.drawable.ic_play_arrow_filled),
            contentDescription = null,
            tint = contentColor,
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.home_play),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

/**
 * Баннер: картинка из макета и подпись внизу.
 */
@Composable
private fun BannerCard(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clip(GlassCardShape)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.img_home_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
                )
        )
        Text(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            text = stringResource(R.string.home_banner_tagline),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        // Рамка поверх картинки
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassBorder(GlassCardShape)
        )
    }
}

@Composable
private fun CardTitle(
    iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Быстрый доступ: переходы в каталоги загрузок.
 */
@Composable
private fun QuickAccessCard(
    backStackViewModel: ScreenBackStackViewModel,
    onOpen: (TitledNavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardTitle(
                iconRes = R.drawable.ic_rocket_launch_filled,
                title = stringResource(R.string.home_quick_access),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_star_filled,
                    text = stringResource(R.string.download_category_favorites),
                    onClick = { onOpen(backStackViewModel.downloadFavoritesScreen) },
                )
                QuickTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_extension_outlined,
                    text = stringResource(R.string.home_quick_modpacks),
                    onClick = { onOpen(backStackViewModel.downloadModPackScreen) },
                )
                QuickTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_format_paint_outlined,
                    text = stringResource(R.string.home_quick_resource_packs),
                    onClick = { onOpen(backStackViewModel.downloadResourcePackScreen) },
                )
                QuickTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_blur_circular_outlined,
                    text = stringResource(R.string.home_quick_shaders),
                    onClick = { onOpen(backStackViewModel.downloadShadersScreen) },
                )
            }
        }
    }
}

@Composable
private fun QuickTile(
    iconRes: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.55f))
            .glassBorder(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Новости. Пока без сетевого источника: показывается приветствие версии.
 */
@Composable
private fun NewsCard(
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CardTitle(
                iconRes = R.drawable.ic_info_outlined,
                title = stringResource(R.string.home_news),
            )
            Text(
                text = stringResource(R.string.home_news_title),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_news_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Информация о лаунчере: выделенная под игру память, версия Android и версия WinLaunch.
 */
@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
) {
    val ramMb = GameSettings.ramAllocation.state
    val ramText = when {
        ramMb == null -> "—"
        ramMb % 1024 == 0 -> "${ramMb / 1024} GB"
        else -> "$ramMb MB"
    }

    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CardTitle(
                iconRes = R.drawable.ic_dashboard_outlined,
                title = stringResource(R.string.home_info),
            )
            InfoLine(
                iconRes = R.drawable.ic_dns_outlined,
                text = stringResource(R.string.home_info_ram, ramText),
            )
            InfoLine(
                iconRes = R.drawable.ic_android_filled,
                text = stringResource(R.string.home_info_android, Build.VERSION.RELEASE),
            )
            InfoLine(
                iconRes = R.drawable.ic_box,
                text = stringResource(R.string.home_info_version, BuildConfig.VERSION_NAME),
            )
        }
    }
}

@Composable
private fun InfoLine(
    iconRes: Int,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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

package com.movtery.zalithlauncher.ui.screens.main

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.ui.screens.content.FirstLoginMenu
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.TextRailItem
import com.movtery.zalithlauncher.ui.guide.sendStartGuideOnce
import com.movtery.zalithlauncher.ui.screens.BackStackNavKey
import com.movtery.zalithlauncher.ui.screens.NavStateHost
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.AccountManageScreen
import com.movtery.zalithlauncher.ui.screens.content.DownloadScreen
import com.movtery.zalithlauncher.ui.screens.content.FileSelectorScreen
import com.movtery.zalithlauncher.ui.screens.content.LauncherScreen
import com.movtery.zalithlauncher.ui.screens.content.LicenseScreen
import com.movtery.zalithlauncher.ui.screens.content.LogViewScreen
import com.movtery.zalithlauncher.ui.screens.content.MultiplayerScreen
import com.movtery.zalithlauncher.ui.screens.content.SettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionExportScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionsManageScreen
import com.movtery.zalithlauncher.ui.screens.content.WebViewScreen
import com.movtery.zalithlauncher.ui.screens.content.addoninfo.AddonInfoScreen
import com.movtery.zalithlauncher.ui.screens.content.navigateToDownload
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.ui.theme.backgroundColor
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.festivals.FestivalTitleText
import com.movtery.zalithlauncher.ui.theme.onBackgroundColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.festival.LocalFestivals
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.ModifyVersionViewModel
import com.movtery.zalithlauncher.viewmodel.ModpackImportViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen
import com.movtery.zalithlauncher.viewmodel.sendToast

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    //监控当前是否有任务正在进行
    var taskRunning by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        TaskSystem.tasksFlow.collect { tasks ->
            val noTasks = tasks.isEmpty()
            eventViewModel.sendKeepScreen(!noTasks)
            taskRunning = noTasks
        }
    }

    val isTaskMenuExpanded = LauncherSettings.launcherTaskMenuExpanded.state

    fun changeTasksExpandedState() {
        LauncherSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    /** 回到主页面通用函数 */
    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    val mainScreenKey = screenBackStackModel.mainScreen.currentKey
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain

    val isBackgroundValid = LocalBackgroundViewModel.current?.isValid == true
    val launcherBackgroundOpacity = LauncherSettings.launcherBackgroundOpacity.state.toFloat() / 100f

    val backgroundColor = if (isBackgroundValid) {
        backgroundColor().copy(alpha = launcherBackgroundOpacity)
    } else backgroundColor()

    val inMultiplayerScreen = mainScreenKey is NormalNavKey.Multiplayer
    val inDownloadScreen = mainScreenKey is NestedNavKey.Download
    val inSettingsScreen = mainScreenKey is NestedNavKey.Settings
    val inAccountsScreen = mainScreenKey is NormalNavKey.AccountManager
    val downloadChild = (mainScreenKey as? BackStackNavKey<*>)?.currentKey
    val sidebarTab = when {
        inLauncherScreen -> SidebarTab.HOME
        inSettingsScreen -> SidebarTab.SETTINGS
        inAccountsScreen -> SidebarTab.ACCOUNTS
        inDownloadScreen && downloadChild is NestedNavKey.DownloadModPack -> SidebarTab.BUILDS
        inDownloadScreen && downloadChild is NestedNavKey.DownloadMod -> SidebarTab.MODS
        inDownloadScreen -> SidebarTab.DOWNLOAD
        else -> SidebarTab.NONE
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = onBackgroundColor()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isBackgroundValid) {
                Image(
                    painter = painterResource(R.drawable.winlaunch_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (inLauncherScreen) 0.30f else 0.74f))
                )
            }

            Row(modifier = Modifier.fillMaxSize()) {
                WinSidebar(
                    modifier = Modifier.fillMaxHeight(),
                    current = sidebarTab,
                    inMultiplayer = inMultiplayerScreen,
                    onSelect = { tab ->
                        if (tab != sidebarTab) when (tab) {
                            SidebarTab.HOME -> toMainScreen()
                            SidebarTab.BUILDS -> screenBackStackModel.navigateToDownload(
                                screenBackStackModel.downloadModPackScreen
                            )
                            SidebarTab.MODS -> screenBackStackModel.navigateToDownload(
                                screenBackStackModel.downloadModScreen
                            )
                            SidebarTab.SETTINGS -> screenBackStackModel.mainScreen.removeAndNavigateTo(
                                removes = screenBackStackModel.clearBeforeNavKeys,
                                screenKey = screenBackStackModel.settingsScreen
                            )
                            SidebarTab.ACCOUNTS -> screenBackStackModel.mainScreen.navigateTo(
                                screenKey = NormalNavKey.AccountManager(FirstLoginMenu.NONE)
                            )
                            SidebarTab.DOWNLOAD -> screenBackStackModel.navigateToDownload()
                            SidebarTab.NONE -> Unit
                        }
                    },
                    onMultiplayer = {
                        if (!inMultiplayerScreen) {
                            screenBackStackModel.mainScreen.removeAndNavigateTo(
                                removes = screenBackStackModel.clearBeforeNavKeys,
                                screenKey = NormalNavKey.Multiplayer
                            )
                        }
                    },
                    onFiles = {
                        eventViewModel.sendEvent(
                            EventViewModel.Event.OpenFileManager(
                                rootPath = PathManager.DIR_FILES_EXTERNAL.absolutePath
                            )
                        )
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    ContentHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        mainScreenKey = mainScreenKey,
                        inLauncherScreen = inLauncherScreen,
                        taskRunning = taskRunning,
                        isTasksExpanded = isTaskMenuExpanded,
                        contentColor = onBackgroundColor(),
                        onScreenBack = {
                            screenBackStackModel.mainScreen.backStack.removeFirstOrNull()
                        },
                        changeExpandedState = {
                            changeTasksExpandedState()
                        },
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        NavStateHost(rootKey = screenBackStackModel.mainScreen) {
                            NavigationUI(
                                modifier = Modifier.fillMaxSize(),
                                screenBackStackModel = screenBackStackModel,
                                toMainScreen = toMainScreen,
                                eventViewModel = eventViewModel,
                                modpackImportViewModel = modpackImportViewModel,
                                modifyVersionViewModel = modifyVersionViewModel,
                                submitError = submitError
                            )
                        }

                        TaskMenu(
                            isExpanded = isTaskMenuExpanded,
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.4f)
                                .align(Alignment.CenterStart)
                                .padding(all = 6.dp)
                        ) {
                            changeTasksExpandedState()
                        }
                    }
                }
            }
        }
    }
}

private enum class SidebarTab { HOME, BUILDS, MODS, SETTINGS, ACCOUNTS, DOWNLOAD, NONE }

/** Левая панель навигации WinLaunch: логотип и основные разделы. */
@Composable
private fun WinSidebar(
    current: SidebarTab,
    inMultiplayer: Boolean,
    onSelect: (SidebarTab) -> Unit,
    onMultiplayer: () -> Unit,
    onFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(118.dp)
            .background(Color.Black.copy(alpha = 0.58f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.winlaunch_tile),
            contentDescription = "WinLaunch",
            modifier = Modifier
                .size(58.dp)
                .padding(bottom = 4.dp),
            contentScale = ContentScale.Fit
        )
        SidebarItem(current == SidebarTab.HOME, R.drawable.ic_home_filled, "Главная") { onSelect(SidebarTab.HOME) }
        SidebarItem(current == SidebarTab.BUILDS, R.drawable.ic_box, "Сборки") { onSelect(SidebarTab.BUILDS) }
        SidebarItem(current == SidebarTab.MODS, R.drawable.ic_extension_outlined, "Моды") { onSelect(SidebarTab.MODS) }
        SidebarItem(current == SidebarTab.SETTINGS, R.drawable.ic_settings_filled, "Настройки") { onSelect(SidebarTab.SETTINGS) }
        SidebarItem(current == SidebarTab.ACCOUNTS, R.drawable.ic_group_filled, "Аккаунты") { onSelect(SidebarTab.ACCOUNTS) }
        SidebarItem(current == SidebarTab.DOWNLOAD, R.drawable.ic_download_2_filled, "Скачать") { onSelect(SidebarTab.DOWNLOAD) }

        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SidebarMiniAction(inMultiplayer, R.drawable.ic_public, "Мультиплеер", onMultiplayer)
            SidebarMiniAction(false, R.drawable.ic_folder_filled, "Файлы", onFiles)
        }
    }
}

@Composable
private fun SidebarItem(
    selected: Boolean,
    icon: Int,
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(Color(0xFFF6F7F9), Color(0xFFB4BBC6)))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
            )
            .border(1.dp, Color.White.copy(alpha = if (selected) 0.7f else 0.16f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (selected) Color(0xFF11151B) else Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (selected) Color(0xFF11151B) else Color(0xFFC3C8D1),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun SidebarMiniAction(
    selected: Boolean,
    icon: Int,
    description: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = if (selected) 0.18f else 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color(0xFFE2E6EC),
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Верхняя полоса: назад и заголовок, кнопка задач и плашка с ОЗУ и версией. */
@Composable
private fun <E: TitledNavKey> ContentHeader(
    mainScreenKey: E?,
    inLauncherScreen: Boolean,
    taskRunning: Boolean,
    isTasksExpanded: Boolean,
    contentColor: Color,
    onScreenBack: () -> Unit,
    changeExpandedState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parentRes = mainScreenKey?.title
    val childRes = (mainScreenKey as? BackStackNavKey<*>)?.currentKey?.title
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!inLauncherScreen) {
                IconButton(
                    onClick = { backDispatcher?.onBackPressed() ?: onScreenBack() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.generic_back),
                        tint = Color(0xFFE7EAF0)
                    )
                }
                if (parentRes != null) {
                    val titleText = if (childRes != null) {
                        androidText(parentRes, androidText(" / "), childRes)
                    } else {
                        parentRes
                    }
                    AndroidStringText(
                        modifier = Modifier.weight(1f),
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall,
                        softWrap = false,
                        maxLines = 1
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            AnimatedVisibility(
                visible = !(isTasksExpanded || taskRunning),
                enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -40 }) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = changeExpandedState)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.width(35.dp),
                        color = Color(0xFFE2E8F0),
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_assignment_filled),
                        contentDescription = stringResource(R.string.main_task_menu),
                        tint = Color(0xFFD8DEE8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (inLauncherScreen) {
                SystemPill()
            }
        }
    }
}

/** Плашка «занято / всего ОЗУ устройства | версия лаунчера» как в макете. */
@Composable
private fun SystemPill() {
    val context = LocalContext.current
    var memory by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am != null) {
                val info = ActivityManager.MemoryInfo()
                am.getMemoryInfo(info)
                val gb = 1024.0 * 1024.0 * 1024.0
                memory = String.format(Locale.US, "%.1f / %.1f GB", (info.totalMem - info.availMem) / gb, info.totalMem / gb)
            }
            delay(3000)
        }
    }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = memory, color = Color(0xFFD6DAE1), fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1)
            Text(text = "ОЗУ", color = Color(0xFF9AA2AF), fontSize = 8.sp, lineHeight = 9.sp)
        }
        Box(
            Modifier
                .width(1.dp)
                .height(20.dp)
                .background(Color.White.copy(alpha = 0.25f))
        )
        Icon(
            painter = painterResource(R.drawable.ic_box),
            contentDescription = null,
            tint = Color(0xFFD6DAE1),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "v" + BuildConfig.VERSION_NAME.substringBefore('-'),
            color = Color(0xFFD6DAE1),
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        /** 导航至版本详细信息屏幕 */
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }
        /** 导航至整合包导出屏幕 */
        val navigateToExport: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NestedNavKey.VersionExport(version),
                useClassEquality = true
            )
        }

        NestedNavDisplay(
            ownerKey = screenBackStackModel.mainScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    LauncherScreen(
                        backStackViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        onLaunchGame = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.Game(version)
                            )
                        },
                        onQuickPlayServer = { version, address ->
                            val target = (version ?: VersionsManager.currentVersion.value)?.takeIf { it.isValid() }
                            if (target != null) {
                                eventViewModel.sendEvent(
                                    EventViewModel.Event.Launch.PlayServer(target, address)
                                )
                            } else {
                                eventViewModel.sendToast(androidText(R.string.game_launch_no_version))
                                screenBackStackModel.mainScreen.navigateTo(NormalNavKey.VersionsManager)
                            }
                        },
                        onOpenLink = {
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it))
                        },
                        startGuideOnce = { keys ->
                            eventViewModel.sendStartGuideOnce(keys)
                        }
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw ->
                            backStack.navigateTo(NormalNavKey.License(raw))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.License> { key ->
                    LicenseScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.AccountManager> { key ->
                    AccountManageScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        openLink = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(
                        backScreenViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        navigateToExport = navigateToExport,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    VersionSettingsScreen(
                        key = key,
                        modifyViewModel = modifyVersionViewModel,
                        backScreenViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        onExportModpack = {
                            navigateToExport(key.version)
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.VersionExport> { key ->
                    VersionExportScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        backToMainScreen = toMainScreen
                    )
                }
                entry<NestedNavKey.Download> { key ->
                    DownloadScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.AddonInfo> { key ->
                    AddonInfoScreen(
                        key = key,
                        mainScreenKey = screenBackStackModel.mainScreen.currentKey,
                        addonInfoScreenKey = key.currentKey,
                        eventViewModel = eventViewModel,
                        submitError = submitError,
                    )
                }
                entry<NormalNavKey.Multiplayer> {
                    MultiplayerScreen(
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.LogView> { key ->
                    LogViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val show = isExpanded && tasks.isNotEmpty()

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeOut(),
        visible = show
    ) {
        BackgroundCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 6.dp),
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor(),
                contentColor = onBackgroundColor()
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                CardTitleLayout(blur = 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                            onClick = changeExpandedState
                        ) {
                            Icon(
                                modifier = Modifier.size(28.dp),
                                painter = painterResource(R.drawable.ic_arrow_left_rounded),
                                contentDescription = stringResource(R.string.generic_collapse)
                            )
                        }

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val taskProgress by task.progress.collectAsStateWithLifecycle()
                        val taskMessage by task.message.collectAsStateWithLifecycle()
                        val rateBytesPerSec by task.rateBytesPerSec.collectAsStateWithLifecycle()

                        TaskItem(
                            taskProgress = taskProgress,
                            taskMessage = taskMessage,
                            rateBytesPerSec = rateBytesPerSec,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            //取消任务
                            TaskSystem.cancelTask(task.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    taskProgress: Float,
    taskMessage: AndroidStringText?,
    rateBytesPerSec: Long?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                taskMessage?.let { message ->
                    AndroidStringText(
                        text = message,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (taskProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { taskProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    taskProgress.takeIf { it >= 0f }?.let { progress ->
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    rateBytesPerSec?.let { bytes ->
                        val text = remember(bytes) { "${formatFileSize(bytes)}/s" }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
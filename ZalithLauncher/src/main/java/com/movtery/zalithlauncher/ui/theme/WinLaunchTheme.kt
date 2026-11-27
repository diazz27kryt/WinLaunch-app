/*
 * WinLaunch
 * Based on Zalith Launcher 2, Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
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

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * WinLaunch: чёрно-серая «стеклянная» палитра с зелёным акцентом статуса.
 */

/** Зелёный индикатор «Готов к запуску» */
val WinLaunchGreen = Color(0xFF3DDC84)

/** Основная форма карточек: крупные скругления, как на макете */
val GlassCardShape = RoundedCornerShape(28.dp)

/** Форма кнопок и полей: «таблетка» */
val GlassPillShape = RoundedCornerShape(50)

internal val winLaunchDark = darkColorScheme(
    primary = Color(0xFFE9E9EE),
    onPrimary = Color(0xFF0B0B0D),
    primaryContainer = Color(0xFF2B2B31),
    onPrimaryContainer = Color(0xFFF2F2F6),
    secondary = Color(0xFFC6C6CD),
    onSecondary = Color(0xFF111114),
    secondaryContainer = Color(0xFF24242A),
    onSecondaryContainer = Color(0xFFE2E2E8),
    tertiary = WinLaunchGreen,
    onTertiary = Color(0xFF00391A),
    tertiaryContainer = Color(0xFF12452A),
    onTertiaryContainer = Color(0xFFBDF5D3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF050506),
    onBackground = Color(0xFFEDEDF1),
    surface = Color(0xFF0A0A0C),
    onSurface = Color(0xFFEDEDF1),
    surfaceVariant = Color(0xFF24242A),
    onSurfaceVariant = Color(0xFFC3C3CA),
    outline = Color(0xFF8D8D95),
    outlineVariant = Color(0xFF3A3A41),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEDEDF1),
    inverseOnSurface = Color(0xFF2A2A2F),
    inversePrimary = Color(0xFF4A4A52),
    surfaceDim = Color(0xFF050506),
    surfaceBright = Color(0xFF2E2E34),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0E0E10),
    surfaceContainer = Color(0xFF131316),
    surfaceContainerHigh = Color(0xFF1A1A1E),
    surfaceContainerHighest = Color(0xFF222227),
)

internal val winLaunchLight = lightColorScheme(
    primary = Color(0xFF1B1B1F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1E1E6),
    onPrimaryContainer = Color(0xFF101013),
    secondary = Color(0xFF55555C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E6EB),
    onSecondaryContainer = Color(0xFF1A1A1E),
    tertiary = Color(0xFF006D3A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB9F4CE),
    onTertiaryContainer = Color(0xFF002111),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F6F8),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFF6F6F8),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE3E3E8),
    onSurfaceVariant = Color(0xFF46464C),
    outline = Color(0xFF76767D),
    outlineVariant = Color(0xFFC7C7CE),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2F2F33),
    inverseOnSurface = Color(0xFFF1F1F5),
    inversePrimary = Color(0xFFC6C6CD),
    surfaceDim = Color(0xFFD9D9DE),
    surfaceBright = Color(0xFFF6F6F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F0F3),
    surfaceContainer = Color(0xFFEBEBEF),
    surfaceContainerHigh = Color(0xFFE5E5EA),
    surfaceContainerHighest = Color(0xFFDFDFE4),
)

private val GlassFill = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.10f),
        Color.White.copy(alpha = 0.03f),
        Color.White.copy(alpha = 0.06f),
    )
)

private val GlassBorder = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.40f),
        Color.White.copy(alpha = 0.06f),
        Color.White.copy(alpha = 0.22f),
    )
)

/**
 * «Стекло»: полупрозрачная градиентная заливка и тонкая светлая рамка.
 * Работает поверх тёмного фона, поэтому светлых оттенков в заливке нет.
 */
fun Modifier.glass(shape: Shape = GlassCardShape): Modifier = this
    .clip(shape)
    .background(brush = GlassFill, shape = shape)
    .border(border = BorderStroke(width = 1.dp, brush = GlassBorder), shape = shape)

/**
 * Только рамка «стекла». Нужна, когда внутри карточки лежит картинка и закрывает рамку.
 */
fun Modifier.glassBorder(shape: Shape = GlassCardShape): Modifier =
    this.border(border = BorderStroke(width = 1.dp, brush = GlassBorder), shape = shape)

/**
 * Карточка в стиле макета WinLaunch.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .glass(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}

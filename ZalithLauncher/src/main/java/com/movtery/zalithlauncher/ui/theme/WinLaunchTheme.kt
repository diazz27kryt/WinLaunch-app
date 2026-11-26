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
val WinLaunchGreen = Color(0xFF72D7A7)
val GlassCardShape = RoundedCornerShape(24.dp)
val GlassPillShape = RoundedCornerShape(50)

internal val winLaunchDark = darkColorScheme(
    primary = Color(0xFFE9EDF4),
    onPrimary = Color(0xFF11151C),
    primaryContainer = Color(0xFF303A49),
    onPrimaryContainer = Color(0xFFF2F5FA),
    secondary = Color(0xFFB8C3D2),
    onSecondary = Color(0xFF171D27),
    secondaryContainer = Color(0xFF283241),
    onSecondaryContainer = Color(0xFFE0E7F0),
    tertiary = Color(0xFF91C9F5),
    onTertiary = Color(0xFF092B43),
    tertiaryContainer = Color(0xFF244C69),
    onTertiaryContainer = Color(0xFFD4ECFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF090C12),
    onBackground = Color(0xFFE9EDF4),
    surface = Color(0xFF0F141D),
    onSurface = Color(0xFFE9EDF4),
    surfaceVariant = Color(0xFF242D3A),
    onSurfaceVariant = Color(0xFFB8C3D2),
    outline = Color(0xFF8491A3),
    outlineVariant = Color(0xFF394556),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE9EDF4),
    inverseOnSurface = Color(0xFF202833),
    inversePrimary = Color(0xFF48566A),
    surfaceDim = Color(0xFF090C12),
    surfaceBright = Color(0xFF303A49),
    surfaceContainerLowest = Color(0xFF07090E),
    surfaceContainerLow = Color(0xFF111720),
    surfaceContainer = Color(0xFF171E29),
    surfaceContainerHigh = Color(0xFF202A37),
    surfaceContainerHighest = Color(0xFF2A3544),
)

internal val winLaunchLight = lightColorScheme(
    primary = Color(0xFF1B2432),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE5F0),
    onPrimaryContainer = Color(0xFF17202D),
    secondary = Color(0xFF4D5C70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E8F0),
    onSecondaryContainer = Color(0xFF202B3A),
    tertiary = Color(0xFF286D9F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD0E9FC),
    onTertiaryContainer = Color(0xFF082B46),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF1F4F8),
    onBackground = Color(0xFF1B2432),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF1B2432),
    surfaceVariant = Color(0xFFE0E7EF),
    onSurfaceVariant = Color(0xFF48566A),
    outline = Color(0xFF718096),
    outlineVariant = Color(0xFFC5CFDC),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF273241),
    inverseOnSurface = Color(0xFFF1F4F8),
    inversePrimary = Color(0xFFBCC9D9),
    surfaceDim = Color(0xFFD8E0E9),
    surfaceBright = Color(0xFFF8FAFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F8),
    surfaceContainer = Color(0xFFE9EEF4),
    surfaceContainerHigh = Color(0xFFE1E8F0),
    surfaceContainerHighest = Color(0xFFD8E0E9),
)

private val GlassFill = Brush.linearGradient(
    colors = listOf(
        Color(0xFFDCE7F5).copy(alpha = 0.11f),
        Color.White.copy(alpha = 0.025f),
        Color(0xFF9BB8D9).copy(alpha = 0.065f),
    )
)

private val GlassBorder = Brush.linearGradient(
    colors = listOf(
        Color(0xFFE5EEF9).copy(alpha = 0.34f),
        Color.White.copy(alpha = 0.055f),
        Color(0xFF90A9C8).copy(alpha = 0.20f),
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

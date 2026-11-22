/*
 * WinLaunch PvP Pack catalogue card.
 */
package com.movtery.zalithlauncher.ui.screens.content.download

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.modpack.optimization.WinLaunchPvPPack

@Composable
fun WinLaunchPvPPackCard(
    modifier: Modifier = Modifier,
    isBusy: Boolean,
    isPreparing: Boolean,
    onInstall: (gameVersion: String) -> Unit
) {
    var selectedVersion by remember {
        mutableStateOf(WinLaunchPvPPack.supportedGameVersions.last())
    }
    var versionMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rocket_launch_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WinLaunch PvP Pack",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "FAIR-PLAY CLIENT PACK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "Оптимизация, визуальные настройки, удобство инвентаря и запись повторов. Без читов, боевых помощников и автоматизации.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy,
                        onClick = { versionMenuExpanded = true }
                    ) {
                        Text("Minecraft $selectedVersion  ▾", maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = versionMenuExpanded,
                        onDismissRequest = { versionMenuExpanded = false }
                    ) {
                        WinLaunchPvPPack.supportedGameVersions.asReversed().forEach { version ->
                            DropdownMenuItem(
                                text = { Text("Minecraft $version") },
                                onClick = {
                                    selectedVersion = version
                                    versionMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Button(enabled = !isBusy, onClick = { onInstall(selectedVersion) }) {
                    Text("Установить")
                }
            }
            if (isPreparing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Проверяем совместимость и готовим сборку…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Создаётся отдельный профиль. Состав зависит от доступных стабильных версий модов для выбранного Minecraft.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

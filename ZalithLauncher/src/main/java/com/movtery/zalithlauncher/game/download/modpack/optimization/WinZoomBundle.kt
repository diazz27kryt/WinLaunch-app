/*
 * Bundles the Win Zoom Fabric mod (hold Z to zoom) into the
 * WinLaunch packs. The jar is shipped inside the launcher as an asset and written to the
 * generated .mrpack as overrides/mods/win-zoom.jar, so the existing Modrinth importer
 * extracts it into the new profile together with the downloaded mods.
 */
package com.movtery.zalithlauncher.game.download.modpack.optimization

import android.content.Context
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WinZoomBundle {
    /** Put the built jar here: ZalithLauncher/src/main/assets/winzoom/win-zoom.jar */
    private const val ASSET_PATH = "winzoom/win-zoom.jar"

    /** The mod is built against Minecraft 1.21.11 only. */
    private val supportedGameVersions = setOf("1.21.11")

    fun isSupported(gameVersion: String): Boolean = gameVersion in supportedGameVersions

    /**
     * Adds the mod to the pack archive. Returns false (and adds nothing) when the Minecraft
     * version is not supported or the jar has not been placed into the launcher assets.
     */
    fun addTo(zip: ZipOutputStream, context: Context, gameVersion: String): Boolean {
        if (!isSupported(gameVersion)) return false
        return try {
            context.assets.open(ASSET_PATH).use { input ->
                zip.putNextEntry(ZipEntry("overrides/mods/win-zoom.jar"))
                input.copyTo(zip)
                zip.closeEntry()
            }
            true
        } catch (_: FileNotFoundException) {
            false
        } catch (e: IOException) {
            throw IllegalStateException("Could not bundle Win Zoom: ${e.message}", e)
        }
    }
}

/*
 * Bundles the Win Visuals Fabric mod (night sky with aurora, trails, hit/jump effects, zoom, Right Shift menu) into the
 * WinLaunch packs. The jar is shipped inside the launcher as an asset and written to the
 * generated .mrpack as overrides/mods/win-visuals.jar, so the existing Modrinth importer
 * extracts it into the new profile together with the downloaded mods.
 */
package com.movtery.zalithlauncher.game.download.modpack.optimization

import android.content.Context
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WinVisualsBundle {
    /**
     * One jar per Minecraft version (the game's mappings differ, so they are not interchangeable).
     * win-visuals-1.21.11.jar is rebuilt by the "Build Win Visuals" workflow; the 1.21.1 jar is the
     * last build of the old 1.21.1 branch of the mod.
     */
    private val assetByGameVersion = mapOf(
        "1.21.1" to "winvisuals/win-visuals-1.21.1.jar",
        "1.21.11" to "winvisuals/win-visuals-1.21.11.jar"
    )

    fun isSupported(gameVersion: String): Boolean = gameVersion in assetByGameVersion

    /**
     * Adds the mod to the pack archive. Returns false (and adds nothing) when the Minecraft
     * version is not supported or the jar has not been placed into the launcher assets.
     */
    fun addTo(zip: ZipOutputStream, context: Context, gameVersion: String): Boolean {
        val assetPath = assetByGameVersion[gameVersion] ?: return false
        return try {
            context.assets.open(assetPath).use { input ->
                zip.putNextEntry(ZipEntry("overrides/mods/win-visuals.jar"))
                input.copyTo(zip)
                zip.closeEntry()
            }
            true
        } catch (_: FileNotFoundException) {
            false
        } catch (e: IOException) {
            throw IllegalStateException("Could not bundle Win Visuals: ${e.message}", e)
        }
    }
}

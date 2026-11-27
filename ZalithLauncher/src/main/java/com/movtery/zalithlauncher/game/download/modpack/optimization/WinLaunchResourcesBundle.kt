/*
 * Bundles the WinLaunch resource pack (night-aurora title-screen panorama + WinLaunch logo) into the
 * WinLaunch PvP and Optimization packs. The pack is shipped inside the launcher as an asset and written to
 * the generated .mrpack as overrides/resourcepacks/WinLaunch.zip. An options.txt that enables it is added too:
 * it is the launcher's own default options (assets/game/options.txt) plus the resourcePacks line, because the
 * launcher only writes its defaults when no options.txt exists yet.
 *
 * Regenerate the pack with: python3 winlaunch-pack/gen_resourcepack.py
 */
package com.movtery.zalithlauncher.game.download.modpack.optimization

import android.content.Context
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WinLaunchResourcesBundle {
    private const val ASSET_PATH = "winlaunch/WinLaunch.zip"
    private const val PACK_FILE_NAME = "WinLaunch.zip"
    private const val DEFAULT_OPTIONS_ASSET = "game/options.txt"
    private const val ENABLED_PACKS_LINE = "resourcePacks:[\"vanilla\",\"file/$PACK_FILE_NAME\"]"

    /**
     * Adds the resource pack and the options.txt that enables it. Returns false (and adds nothing) when the
     * pack has not been placed into the launcher assets.
     */
    fun addTo(zip: ZipOutputStream, context: Context): Boolean {
        return try {
            val pack = context.assets.open(ASSET_PATH).use { it.readBytes() }
            val options = buildOptions(context)

            zip.putNextEntry(ZipEntry("overrides/resourcepacks/$PACK_FILE_NAME"))
            zip.write(pack)
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("overrides/options.txt"))
            zip.write(options.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            true
        } catch (_: FileNotFoundException) {
            false
        } catch (e: IOException) {
            throw IllegalStateException("Could not bundle the WinLaunch resource pack: ${e.message}", e)
        }
    }

    private fun buildOptions(context: Context): String {
        val defaults = try {
            context.assets.open(DEFAULT_OPTIONS_ASSET).use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (_: IOException) {
            ""
        }
        val lines = defaults.lines()
            .filter { it.isNotBlank() && !it.startsWith("resourcePacks:") }
            .toMutableList()
        lines.add(ENABLED_PACKS_LINE)
        return lines.joinToString("\n", postfix = "\n")
    }
}

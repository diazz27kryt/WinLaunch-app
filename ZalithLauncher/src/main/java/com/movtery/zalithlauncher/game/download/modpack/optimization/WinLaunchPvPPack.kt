/*
 * WinLaunch PvP Pack builder.
 * A fair-play client pack: performance, recording and everyday QoL only.
 * No combat automation, reach changes, x-ray, packet manipulation or PvP helper HUDs.
 */
package com.movtery.zalithlauncher.game.download.modpack.optimization

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformDependencyType
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.MODRINTH_API
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.models.ModrinthVersion
import com.movtery.zalithlauncher.utils.network.httpGetJson
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WinLaunchPvPPack {
    val supportedGameVersions = WinLaunchOptimizationPack.supportedGameVersions

    private data class Project(val slug: String, val required: Boolean = false)

    // Performance + general QoL/recording only. Deliberately excludes combat helpers,
    // inventory/armor advantage HUDs, auto-clickers, macros and reach modifications.
    private val projects = listOf(
        Project("sodium", required = true),
        Project("lithium"),
        Project("ferrite-core"),
        Project("immediatelyfast"),
        Project("entityculling"),
        Project("modernfix"),
        Project("moreculling"),
        Project("badoptimizations"),
        Project("sodium-extra"),
        Project("appleskin"),
        Project("mouse-tweaks"),
        Project("replaymod"),
        // Aspect-ratio / screen scaling, display only. Skipped when it has no release for the game version.
        Project("rescale"),
        // Required by the bundled Win Visuals mod.
        Project("fabric-api")
    )

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    suspend fun create(context: Context, gameVersion: String): File = withContext(Dispatchers.IO) {
        require(gameVersion in supportedGameVersions) {
            "Unsupported WinLaunch PvP Pack version: $gameVersion"
        }

        val fabricLoader = FabricVersions.fetchFabricLoaderList(gameVersion)
            ?.firstOrNull { it.stable }
            ?: FabricVersions.fetchFabricLoaderList(gameVersion)?.firstOrNull()
            ?: throw IllegalStateException("Fabric loader is unavailable for Minecraft $gameVersion.")

        val selected = linkedMapOf<String, ModrinthVersion>()
        for (project in projects) {
            val version = findCompatibleVersion(project.slug, gameVersion)
            if (version == null) {
                if (project.required) {
                    throw IllegalStateException(
                        "Required mod ${project.slug} has no compatible Fabric release for Minecraft $gameVersion."
                    )
                }
                continue
            }
            val previousKeys = selected.keys.toSet()
            try {
                resolveRequiredDependencies(version, gameVersion, selected)
            } catch (e: ClientRequestException) {
                if (project.required || e.response.status.value != 404) throw e
                (selected.keys - previousKeys).forEach(selected::remove)
            }
        }

        if (selected.values.none { it.projectId == "sodium" || it.projectId == "AANobbMI" }) {
            throw IllegalStateException("Sodium is unavailable for Minecraft $gameVersion.")
        }

        val files = JsonArray()
        selected.values.sortedBy { it.projectId }.forEach { version ->
            val file = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull()
                ?: throw IllegalStateException("A selected Modrinth release has no downloadable file.")
            val indexFile = JsonObject().apply {
                val safeName = file.fileName.replace("/", "_").replace("\\", "_")
                addProperty("path", "mods/${version.projectId}-$safeName")
                add("hashes", JsonObject().apply {
                    addProperty("sha1", file.hashes.sha1)
                    addProperty("sha512", file.hashes.sha512)
                })
                add("env", JsonObject().apply {
                    addProperty("client", "required")
                    addProperty("server", "unsupported")
                })
                add("downloads", JsonArray().apply { add(file.url) })
                addProperty("fileSize", file.size)
            }
            files.add(indexFile)
        }

        val manifest = JsonObject().apply {
            addProperty("game", "minecraft")
            addProperty("formatVersion", 1)
            addProperty("versionId", "winlaunch-pvp-$gameVersion")
            addProperty("name", "WinLaunch PvP Pack $gameVersion")
            addProperty(
                "summary",
                "Fair-play Fabric pack: performance, visual settings, inventory QoL and replay recording. No combat automation or PvP helper HUDs."
            )
            add("files", files)
            add("dependencies", JsonObject().apply {
                addProperty("minecraft", gameVersion)
                addProperty("fabric-loader", fabricLoader.version)
            })
        }

        val packDirectory = File(context.cacheDir, "winlaunch-pvp-pack").apply {
            if (!exists() && !mkdirs()) {
                throw IllegalStateException("Could not create the WinLaunch PvP Pack cache directory.")
            }
        }
        val packFile = File(packDirectory, "winlaunch-pvp-pack-$gameVersion.mrpack")
        if (packFile.exists() && !packFile.delete()) {
            throw IllegalStateException("Could not replace the cached PvP Pack.")
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(packFile))).use { zip ->
            zip.putNextEntry(ZipEntry("modrinth.index.json"))
            zip.write(gson.toJson(manifest).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            WinVisualsBundle.addTo(zip, context, gameVersion)
            WinZoomBundle.addTo(zip, context, gameVersion)
            WinLaunchResourcesBundle.addTo(zip, context)
        }
        packFile
    }

    private suspend fun findCompatibleVersion(projectSlug: String, gameVersion: String): ModrinthVersion? {
        val versions = httpGetJson<List<ModrinthVersion>>(
            url = "$MODRINTH_API/project/$projectSlug/version",
            parameters = Parameters.build {
                append("loaders", """["fabric"]""")
                append("game_versions", """["$gameVersion"]""")
                append("include_changelog", "false")
            }
        )
        val compatible = versions
            .filter { gameVersion in it.gameVersions && it.loaders.any { loader -> loader.equals("fabric", true) } }
            .sortedByDescending { it.datePublished }
        return compatible.firstOrNull { it.versionType == PlatformReleaseType.RELEASE }
            ?: compatible.firstOrNull { it.versionType == PlatformReleaseType.BETA }
    }

    private suspend fun resolveRequiredDependencies(
        version: ModrinthVersion,
        gameVersion: String,
        selected: MutableMap<String, ModrinthVersion>
    ) {
        val previous = selected[version.projectId]
        if (previous != null) {
            if (previous.id != version.id) {
                throw IllegalStateException("Conflicting required versions for project ${version.projectId}.")
            }
            return
        }
        if (gameVersion !in version.gameVersions || version.loaders.none { it.equals("fabric", true) }) {
            throw IllegalStateException("A required dependency is incompatible with Minecraft $gameVersion and Fabric.")
        }
        selected[version.projectId] = version

        version.dependencies
            .filter { it.dependencyType == PlatformDependencyType.REQUIRED }
            .forEach { dependency ->
                val dependencyVersion = when {
                    dependency.versionId != null -> httpGetJson<ModrinthVersion>(
                        url = "$MODRINTH_API/version/${dependency.versionId}"
                    )
                    dependency.projectId != null -> findCompatibleVersion(dependency.projectId, gameVersion)
                        ?: throw IllegalStateException(
                            "A required dependency has no Fabric release for Minecraft $gameVersion."
                        )
                    else -> null
                } ?: return@forEach

                resolveRequiredDependencies(dependencyVersion, gameVersion, selected)
            }
    }
}

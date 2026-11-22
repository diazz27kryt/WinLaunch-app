/*
 * WinLaunch Optimization pack builder.
 * Builds a Modrinth .mrpack manifest from current, version-compatible Fabric releases.
 * Mod files remain hosted by their original Modrinth authors; this launcher only creates
 * a manifest containing their download URLs and hashes.
 */

package com.movtery.zalithlauncher.game.download.modpack.optimization

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.GsonBuilder
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformDependencyType
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.MODRINTH_API
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.models.ModrinthVersion
import com.movtery.zalithlauncher.utils.network.httpGetJson
import io.ktor.http.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * WinLaunch's pure-performance Fabric pack.
 *
 * The supported versions are intentionally explicit: each selected Minecraft version
 * gets a separate Modrinth pack and only receives mod builds published for that exact
 * game version and the Fabric loader.
 */
object WinLaunchOptimizationPack {
    val supportedGameVersions = listOf(
        "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
        "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5",
        "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11"
    )

    private data class CoreProject(
        val projectId: String,
        val displayName: String,
        val required: Boolean = false
    )

    // Sodium is the core renderer. The remaining mods are added only when a stable
    // Fabric release exists for the selected Minecraft version.
    private val coreProjects = listOf(
        CoreProject("AANobbMI", "Sodium", required = true),
        CoreProject("gvQqBUqZ", "Lithium"),
        CoreProject("uXXizFIs", "FerriteCore"),
        CoreProject("5ZwdcRci", "ImmediatelyFast"),
        CoreProject("NNAgCjsB", "EntityCulling"),
        CoreProject("nmDcB62a", "ModernFix"),
        CoreProject("51shyZVL", "MoreCulling"),
        CoreProject("g96Z4WVZ", "BadOptimizations")
    )

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    /**
     * Creates a small .mrpack archive. The importer later downloads the actual mod JARs
     * from Modrinth and creates a separate Minecraft profile for the selected version.
     */
    suspend fun create(context: Context, gameVersion: String): File = withContext(Dispatchers.IO) {
        require(gameVersion in supportedGameVersions) {
            "Unsupported Optimization version: $gameVersion"
        }

        val fabricLoader = FabricVersions.fetchFabricLoaderList(gameVersion)
            ?.firstOrNull { it.stable }
            ?: FabricVersions.fetchFabricLoaderList(gameVersion)?.firstOrNull()
            ?: throw IllegalStateException("Fabric loader is unavailable for Minecraft $gameVersion.")

        val selectedVersions = linkedMapOf<String, ModrinthVersion>()

        for (project in coreProjects) {
            val version = findCompatibleVersion(project.projectId, gameVersion)
            if (version == null) {
                if (project.required) {
                    throw IllegalStateException(
                        "${project.displayName} does not have a compatible Fabric release for Minecraft $gameVersion."
                    )
                }
                continue
            }
            resolveRequiredDependencies(
                version = version,
                gameVersion = gameVersion,
                selected = selectedVersions
            )
        }

        if (selectedVersions.values.none { it.projectId == coreProjects.first().projectId }) {
            throw IllegalStateException("Sodium is unavailable for Minecraft $gameVersion.")
        }

        val files = JsonArray()
        selectedVersions.values.sortedBy { it.projectId }.forEach { version ->
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
            addProperty("versionId", "winlaunch-optimization-$gameVersion")
            addProperty("name", "WinLaunch Optimization $gameVersion")
            addProperty(
                "summary",
                "Pure performance Fabric pack for Minecraft $gameVersion. Mod files are selected from compatible stable releases."
            )
            add("files", files)
            add("dependencies", JsonObject().apply {
                addProperty("minecraft", gameVersion)
                addProperty("fabric-loader", fabricLoader.version)
            })
        }

        val packDirectory = File(context.cacheDir, "winlaunch-optimization").apply {
            if (!exists() && !mkdirs()) {
                throw IllegalStateException("Could not create the Optimization cache directory.")
            }
        }
        val packFile = File(packDirectory, "winlaunch-optimization-$gameVersion.mrpack")
        if (packFile.exists() && !packFile.delete()) {
            throw IllegalStateException("Could not replace the cached Optimization pack.")
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(packFile))).use { zip ->
            zip.putNextEntry(ZipEntry("modrinth.index.json"))
            zip.write(gson.toJson(manifest).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        packFile
    }

    private suspend fun findCompatibleVersion(
        projectId: String,
        gameVersion: String
    ): ModrinthVersion? {
        val versions = httpGetJson<List<ModrinthVersion>>(
            url = "$MODRINTH_API/project/$projectId/version",
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
        val existing = selected[version.projectId]
        if (existing != null) {
            if (existing.id != version.id) {
                throw IllegalStateException(
                    "Conflicting required Modrinth versions for project ${version.projectId}."
                )
            }
            return
        }

        if (gameVersion !in version.gameVersions ||
            version.loaders.none { it.equals("fabric", true) }
        ) {
            throw IllegalStateException(
                "A required dependency is not compatible with Minecraft $gameVersion and Fabric."
            )
        }

        selected[version.projectId] = version

        version.dependencies
            .filter { it.dependencyType == PlatformDependencyType.REQUIRED }
            .forEach { dependency ->
                val dependencyVersion = when {
                    dependency.versionId != null -> {
                        httpGetJson<ModrinthVersion>(
                            url = "$MODRINTH_API/version/${dependency.versionId}"
                        )
                    }
                    dependency.projectId != null -> {
                        findCompatibleVersion(dependency.projectId, gameVersion)
                            ?: throw IllegalStateException(
                                "A required dependency has no Fabric release for Minecraft $gameVersion."
                            )
                    }
                    else -> null
                } ?: return@forEach

                resolveRequiredDependencies(
                    version = dependencyVersion,
                    gameVersion = gameVersion,
                    selected = selected
                )
            }
    }
}

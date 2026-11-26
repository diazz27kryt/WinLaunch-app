/*
 * Writes the graphics API preference into options.txt for every key name Mojang has used.
 *
 * Minecraft 26.2 snapshots stored it as `preferredGraphicsBackend:"opengl"`; 26.2+ releases and
 * 26.3 / 26.4 use `graphicsApiPreference:"prefer_opengl"` (and briefly `graphicsApi`). A launcher that
 * only writes the old key leaves the new one unset, so the game falls back to its own default.
 * From 26.4 that default is Vulkan, which crashes on Android devices without the needed Vulkan stack.
 */
package com.movtery.zalithlauncher.game.launch

import com.movtery.zalithlauncher.game.version.installed.GraphicsApi

object GraphicsOptions {
    /** 26.2 snapshots: values "default" / "opengl" / "vulkan" */
    private const val KEY_SNAPSHOT = "preferredGraphicsBackend"

    /** Short-lived key, values "prefer_opengl" / "prefer_vulkan" */
    private const val KEY_LEGACY = "graphicsApi"

    /** 26.2+ / 26.3 / 26.4: values "default" / "prefer_opengl" / "prefer_vulkan" */
    private const val KEY_CURRENT = "graphicsApiPreference"

    private fun String.unquoted() = trim().removeSurrounding("\"")

    /**
     * Applies [api] to options.txt (the caller saves it).
     *
     * - [GraphicsApi.OPENGL] / [GraphicsApi.VULKAN] always override whatever the game stored.
     * - [GraphicsApi.DEFAULT_OPENGL] chooses OpenGL unless the player picked something explicit.
     *   A stored "default" does not count as a choice: the game writes it for every untouched
     *   setting, and from 26.4 it means Vulkan.
     * - [GraphicsApi.DEFAULT] leaves the game's own setting alone.
     */
    fun apply(api: GraphicsApi, get: (String) -> String?, set: (String, String) -> Unit) {
        val keys = listOf(
            KEY_SNAPSHOT to listOf("default", "opengl", "vulkan"),
            KEY_LEGACY to listOf("default", "prefer_opengl", "prefer_vulkan"),
            KEY_CURRENT to listOf("default", "prefer_opengl", "prefer_vulkan")
        )
        for ((key, values) in keys) {
            val (defaultValue, openglValue, vulkanValue) = values
            val existing = get(key)?.unquoted()?.takeIf { it.isNotEmpty() }
            val target = when (api) {
                GraphicsApi.OPENGL -> openglValue
                GraphicsApi.VULKAN -> vulkanValue
                GraphicsApi.DEFAULT_OPENGL ->
                    if (existing == null || existing == defaultValue) openglValue else existing
                GraphicsApi.DEFAULT -> existing ?: defaultValue
            }
            set(key, "\"$target\"")
        }
    }
}

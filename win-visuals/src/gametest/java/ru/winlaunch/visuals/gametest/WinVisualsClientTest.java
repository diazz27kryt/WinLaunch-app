package ru.winlaunch.visuals.gametest;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import ru.winlaunch.visuals.VisualsConfig;
import ru.winlaunch.visuals.WinVisuals;
import ru.winlaunch.visuals.gui.WinVisualsScreen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Boots a real client, checks Right Shift opens the menu, and screenshots the sky and every tab. */
public class WinVisualsClientTest implements FabricClientGameTest {
    private static final Path SHOTS = Path.of(System.getProperty("java.io.tmpdir"), "wvshots");

    /** Writes the main framebuffer (world + GUI) to a PNG that CI prints afterwards. */
    private static void shot(ClientGameTestContext context, String name) {
        context.runOnClient(client -> Screenshot.takeScreenshot(client.getMainRenderTarget(), image -> {
            try (NativeImage img = image) {
                Files.createDirectories(SHOTS);
                img.writeToFile(SHOTS.resolve(name + ".png"));
                System.out.println("WVSHOT saved " + name);
            } catch (IOException e) {
                System.out.println("WVSHOT failed " + name + " " + e);
            }
        }));
        context.waitTicks(6);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        // Software rendering on a CI runner is slow, so keep chunk generation and drawing minimal.
        context.runOnClient(client -> {
            client.options.renderDistance().set(2);
            client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(10);   // keep the CPU free for the server while it generates the world
        });
        try (TestSingleplayerContext world = context.worldBuilder()
            .setUseConsistentSettings(false)
            .adjustSettings(ui -> {
                // A flat world is far cheaper to generate on a slow CI runner.
                for (var entry : ui.getNormalPresetList()) {
                    if (entry.preset() != null && entry.preset().is(WorldPresets.FLAT)) {
                        ui.setWorldType(entry);
                        break;
                    }
                }
                ui.setGenerateStructures(false);
            })
            .create()) {
            context.runOnClient(client -> {
                client.options.renderDistance().set(3);
                client.options.framerateLimit().set(30);
            });
            world.getServer().runCommand("time set midnight");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @p ~ ~ ~ 20 -38");
            world.getClientWorld().waitForChunksRender();
            context.waitTicks(40);

            VisualsConfig cfg = VisualsConfig.get();
            cfg.enabled = true;
            cfg.auroraSky = true;
            cfg.skyStars = true;
            cfg.skyNebula = true;
            context.waitTicks(10);
            shot(context, "wv_sky_a");
            world.getServer().runCommand("tp @p ~ ~ ~ 200 -60");
            context.waitTicks(10);
            shot(context, "wv_sky_b");
            world.getServer().runCommand("tp @p ~ ~ ~ 20 -12");
            context.waitTicks(10);
            shot(context, "wv_sky_horizon");

            // Holding Z zooms in.
            context.runOnClient(client -> KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_Z), true));
            context.waitTicks(20);
            shot(context, "wv_zoom");
            context.runOnClient(client -> KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_Z), false));
            context.waitTicks(10);

            // Right Shift must open the menu.
            context.getInput().pressKey(WinVisuals.openKey());
            context.waitTicks(10);
            boolean opened = context.computeOnClient(client -> client.screen instanceof WinVisualsScreen);
            System.out.println("WVTEST right-shift-opens-menu=" + opened);
            if (!opened) {
                throw new AssertionError("Right Shift did not open the Win Visuals menu");
            }
            context.waitTicks(30);
            shot(context, "wv_gui_0");
            for (int tab = 1; tab < 5; tab++) {
                final int t = tab;
                context.runOnClient(client -> ((WinVisualsScreen) client.screen).selectTab(t));
                context.waitTicks(10);
                shot(context, "wv_gui_" + tab);
            }

            context.runOnClient(client -> client.setScreen(null));
            cfg.auroraSky = false;
            cfg.skyNebula = false;
            cfg.skyStars = false;
            context.waitTicks(5);
            shot(context, "wv_sky_effects_off");
            System.out.println("WVTEST done");
        }
    }
}

package ru.winlaunch.visuals.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import ru.winlaunch.visuals.VisualsConfig;
import ru.winlaunch.visuals.WinVisuals;
import ru.winlaunch.visuals.gui.WinVisualsScreen;

/** Boots a real client, checks Right Shift opens the menu, and screenshots the sky and every tab. */
public class WinVisualsClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        // Software rendering on a CI runner is slow, so keep chunk generation and drawing minimal.
        context.runOnClient(client -> {
            client.options.renderDistance().set(3);
            client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(30);
        });
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
            world.getServer().runCommand("time set midnight");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @p ~ ~ ~ 0 -55");
            world.getClientWorld().waitForChunksRender();
            context.waitTicks(40);

            VisualsConfig cfg = VisualsConfig.get();
            cfg.enabled = true;
            cfg.auroraSky = true;
            cfg.skyStars = true;
            cfg.skyNebula = true;
            context.takeScreenshot("wv_sky_aurora");

            // Right Shift must open the menu.
            context.getInput().pressKey(WinVisuals.openKey());
            context.waitTicks(10);
            boolean opened = context.computeOnClient(client -> client.screen instanceof WinVisualsScreen);
            System.out.println("WVTEST right-shift-opens-menu=" + opened);
            if (!opened) {
                throw new AssertionError("Right Shift did not open the Win Visuals menu");
            }
            context.waitTicks(20);
            context.takeScreenshot("wv_gui_0_sky");
            for (int tab = 1; tab < 5; tab++) {
                final int t = tab;
                context.runOnClient(client -> ((WinVisualsScreen) client.screen).selectTab(t));
                context.waitTicks(5);
                context.takeScreenshot("wv_gui_" + tab);
            }

            context.runOnClient(client -> client.setScreen(null));
            cfg.auroraSky = false;
            cfg.skyNebula = false;
            cfg.skyStars = false;
            context.waitTicks(5);
            context.takeScreenshot("wv_sky_effects_off");
            System.out.println("WVTEST done");
        }
    }
}

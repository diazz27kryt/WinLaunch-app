package ru.winlaunch.zoom;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Hold Z: 2x zoom. While holding, hotbar slot 1 switches to 3x and slot 2 to 5x
 * (the slot keys do not change the selected slot during zoom). Nothing goes beyond 5x.
 */
public class WinZoom implements ClientModInitializer {
    private static KeyMapping zoomKey;
    private static double target = 1.0;
    private static double current = 1.0;
    private static double level = 2.0;

    @Override
    public void onInitializeClient() {
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.winzoom.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("winzoom", "main"))));

        // Start of tick runs before vanilla handles hotbar keys, so we can swallow them.
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        boolean zooming = mc.screen == null && mc.player != null && zoomKey.isDown();
        if (zooming) {
            KeyMapping[] slots = mc.options.keyHotbarSlots;
            while (slots[0].consumeClick()) level = 3.0;
            while (slots[1].consumeClick()) level = 5.0;
            target = level;
        } else {
            target = 1.0;
            level = 2.0; // every new hold starts at 2x
        }
    }

    /** Smoothed zoom factor for the current frame, 1.0 = no zoom. */
    public static double factor(float partialTick) {
        current += (target - current) * 0.35;
        if (Math.abs(target - current) < 0.001) current = target;
        return current;
    }
}

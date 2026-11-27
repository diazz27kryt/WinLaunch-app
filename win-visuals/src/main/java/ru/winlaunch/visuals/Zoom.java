package ru.winlaunch.visuals;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Hold-to-zoom (default Z). The field of view is divided by a smoothed factor in GameRendererMixin. */
public final class Zoom {
    private static KeyMapping key;
    private static float current = 1f;
    private static long lastNanos = 0L;

    /** The standalone Win Zoom mod, bundled in some packs, also owns Z; then it does the zooming and we stay out of the way. */
    private static final boolean STANDALONE = FabricLoader.getInstance().isModLoaded("winzoom");

    private Zoom() { }

    public static void bind(KeyMapping mapping) {
        key = mapping;
    }

    /** Smoothed zoom factor, 1.0 when not zooming. Safe to call several times per frame. */
    public static float factor() {
        if (STANDALONE) return 1f;
        VisualsConfig cfg = VisualsConfig.get();
        Minecraft mc = Minecraft.getInstance();
        boolean held = key != null && key.isDown() && cfg.enabled && cfg.zoomEnabled
            && mc.screen == null && mc.player != null;
        float target = held ? Math.max(1.1f, cfg.zoomFactor) : 1f;
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        float k = cfg.uiAnimations ? Math.min(1f, dt * 14f) : 1f;
        current += (target - current) * k;
        if (Math.abs(current - target) < 0.002f) current = target;
        return current;
    }
}

package ru.winlaunch.visuals;

import java.awt.Color;

/** Picks the particle colour according to the config. */
public final class Palette {
    private Palette() {}

    public static int rgb(long gameTime, float offset) {
        VisualsConfig cfg = VisualsConfig.get();
        float t = gameTime * 0.01f * cfg.colorSpeed + offset;
        switch (cfg.colorMode) {
            case SOLID:
                return cfg.solidRgb();
            case PULSE: {
                int base = cfg.solidRgb();
                float k = 0.65f + 0.35f * (float) Math.sin(t * 6.0f);
                int r = Math.min(255, (int) (((base >> 16) & 0xFF) * k));
                int g = Math.min(255, (int) (((base >> 8) & 0xFF) * k));
                int b = Math.min(255, (int) ((base & 0xFF) * k));
                return (r << 16) | (g << 8) | b;
            }
            case RAINBOW:
            default:
                return Color.HSBtoRGB(t % 1.0f, 0.75f, 1.0f) & 0xFFFFFF;
        }
    }
}

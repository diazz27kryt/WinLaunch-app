package ru.winlaunch.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Compact "island" at the top centre of the screen: mod name, FPS, Moscow time and, when the device
 * exposes it, the battery charge.
 */
public final class Island {
    private static final ZoneOffset MSK = ZoneOffset.ofHours(3);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Path[] BATTERY_FILES = {
        Path.of("/sys/class/power_supply/battery/capacity"),
        Path.of("/sys/class/power_supply/BAT0/capacity"),
        Path.of("/sys/class/power_supply/BAT1/capacity")
    };

    private static int battery = -1;
    private static long batteryReadAt = 0;

    private Island() {}

    /** Battery percent, or -1 when it cannot be read. Re-read every 15 seconds. */
    private static int battery() {
        long now = System.currentTimeMillis();
        if (now - batteryReadAt < 15_000L) return battery;
        batteryReadAt = now;
        battery = -1;
        for (Path p : BATTERY_FILES) {
            try {
                if (Files.isReadable(p)) {
                    int v = Integer.parseInt(Files.readString(p).trim());
                    if (v >= 0 && v <= 100) {
                        battery = v;
                        break;
                    }
                }
            } catch (Exception ignored) {
                // not readable on this device, keep -1
            }
        }
        return battery;
    }

    public static void render(GuiGraphics gui, Minecraft mc) {
        String name = "WinVisuals";
        String fps = Math.round(PerfGovernor.fps()) + " FPS";
        String time = OffsetDateTime.now(MSK).format(TIME) + " МСК";
        int bat = battery();
        String charge = bat >= 0 ? bat + "%" : null;

        int pad = 8;
        int gap = 6;
        int w = pad + mc.font.width(name) + gap * 2 + 1 + mc.font.width(fps) + gap * 2 + 1 + mc.font.width(time) + pad;
        if (charge != null) w += gap * 2 + 1 + mc.font.width(charge);
        int h = 16;
        int x = (gui.guiWidth() - w) / 2;
        int y = 5;

        long t = mc.level == null ? 0L : mc.level.getGameTime();
        int accent = 0xFF000000 | Palette.rgb(t, 0f);

        pill(gui, x - 1, y - 1, w + 2, h + 2, 0x55FFFFFF);
        pill(gui, x, y, w, h, 0xE60A0C12);

        int ty = y + 4;
        int cx = x + pad;
        gui.drawString(mc.font, name, cx, ty, accent, false);
        cx += mc.font.width(name) + gap;
        cx = separator(gui, cx, y, h, gap);
        gui.drawString(mc.font, fps, cx, ty, 0xFFE8ECF6, false);
        cx += mc.font.width(fps) + gap;
        cx = separator(gui, cx, y, h, gap);
        gui.drawString(mc.font, time, cx, ty, 0xFFB4B8C6, false);
        cx += mc.font.width(time) + gap;
        if (charge != null) {
            cx = separator(gui, cx, y, h, gap);
            int color = bat <= 15 ? 0xFFFF6B6B : 0xFF8FE8B0;
            gui.drawString(mc.font, charge, cx, ty, color, false);
        }
    }

    private static int separator(GuiGraphics gui, int x, int y, int h, int gap) {
        gui.fill(x, y + 4, x + 1, y + h - 4, 0x44FFFFFF);
        return x + 1 + gap;
    }

    /** Filled rectangle with fully rounded ends. */
    private static void pill(GuiGraphics gui, int x, int y, int w, int h, int color) {
        int r = h / 2;
        for (int i = 0; i < h; i++) {
            double dy = (i + 0.5) - r;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, r * r - dy * dy)));
            gui.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
    }
}

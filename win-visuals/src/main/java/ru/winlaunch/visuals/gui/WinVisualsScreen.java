package ru.winlaunch.visuals.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import ru.winlaunch.visuals.Palette;
import ru.winlaunch.visuals.VisualsConfig;
import ru.winlaunch.visuals.VisualsConfig.ColorMode;
import ru.winlaunch.visuals.WinVisuals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Win Visuals settings screen. Opens on Right Shift, custom-drawn glass UI. */
public class WinVisualsScreen extends Screen {
    private static final int PANEL_W = 420;
    private static final int PANEL_H = 284;
    private static final int SIDEBAR_W = 108;

    private static final int BG = 0xE6101420;
    private static final int SIDEBAR = 0xF20B0E18;
    private static final int TEXT = 0xFFE8ECF6;
    private static final int TEXT_DIM = 0xFF8A93AB;

    private static final String[] TABS = {"sky", "visuals", "particles", "interface", "settings"};
    private static final int[] PRESETS = {
        0x55AAFF, 0x9B6BFF, 0xFF5FA2, 0xFF5555, 0xFFA03C, 0xFFE14D, 0x55FF8A, 0xFFFFFF
    };

    private final VisualsConfig cfg = VisualsConfig.get();
    private final long openedAt = System.currentTimeMillis();
    private final List<Row> rows = new ArrayList<>();
    private int tab = 0;
    private Row active;
    private int px, py;

    public WinVisualsScreen() {
        super(Component.literal("Win Visuals"));
        buildTab();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Own dim, no vanilla blur.
        int alpha = Math.max(0, Math.min(220, (int) (cfg.backgroundDim * 255f)));\n        g.fill(0, 0, this.width, this.height, (alpha << 24));
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float open = cfg.uiAnimations ? Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f) : 1f;
        float ease = 1f - (1f - open) * (1f - open);
        g.pose().pushPose();
        g.pose().translate(this.width / 2.0f, this.height / 2.0f, 0);
        g.pose().scale(cfg.uiScale, cfg.uiScale, 1f);
        g.pose().translate(-this.width / 2.0f, -this.height / 2.0f, 0);
        renderBackground(g, mouseX, mouseY, partialTick);

        px = (this.width - PANEL_W) / 2;
        py = (this.height - PANEL_H) / 2 + (int) ((1f - ease) * 12);
        int accent = accent(0f);

        // shadow + panel
        rrect(g, px - 2, py - 2, PANEL_W + 4, PANEL_H + 4, 8, a(0x55000000, ease));
        rrect(g, px, py, PANEL_W, PANEL_H, 6, a(BG, ease));
        rrect(g, px, py, SIDEBAR_W, PANEL_H, 6, a(SIDEBAR, ease));
        g.fill(px + SIDEBAR_W - 6, py, px + SIDEBAR_W, py + PANEL_H, a(SIDEBAR, ease));

        // title with animated accent bar
        g.drawString(this.font, "WIN", px + 14, py + 14, a(accent, ease), true);
        g.drawString(this.font, "VISUALS", px + 14 + this.font.width("WIN ") , py + 14, a(TEXT, ease), true);
        for (int i = 0; i < SIDEBAR_W - 28; i++) {
            g.fill(px + 14 + i, py + 28, px + 15 + i, py + 30, a(accent((float) i / (SIDEBAR_W - 28)), ease));
        }

        // tabs
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 42 + i * 24;
            boolean sel = i == tab;
            boolean hov = inside(mouseX, mouseY, px + 8, ty, SIDEBAR_W - 16, 20);
            if (sel) {
                rrect(g, px + 8, ty, SIDEBAR_W - 16, 20, 4, a((accent & 0xFFFFFF) | 0x44000000, ease));
                g.fill(px + 8, ty + 4, px + 10, ty + 16, a(accent, ease));
            } else if (hov) {
                rrect(g, px + 8, ty, SIDEBAR_W - 16, 20, 4, a(0x22FFFFFF, ease));
            }
            g.drawString(this.font, t("tab." + TABS[i]), px + 18, ty + 6, a(sel ? TEXT : TEXT_DIM, ease), false);
        }

        String hint = t("hint");
        g.drawString(this.font, hint, px + 10, py + PANEL_H - 16, a(TEXT_DIM, ease), false);

        // content header
        int cx = px + SIDEBAR_W + 14;
        int cw = PANEL_W - SIDEBAR_W - 28;
        g.drawString(this.font, t("tab." + TABS[tab]), cx, py + 14, a(TEXT, ease), true);
        g.fill(cx, py + 28, cx + cw, py + 29, a(0x22FFFFFF, ease));

        int y = py + 38;
        for (Row r : rows) {
            r.render(g, cx, y, cw, mouseX, mouseY, ease);
            y += r.h + 6;
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        for (int i = 0; i < TABS.length; i++) {
            if (inside(mx, my, px + 8, py + 42 + i * 24, SIDEBAR_W - 16, 20)) {
                if (tab != i) {
                    tab = i;
                    buildTab();
                }
                return true;
            }
        }
        int cx = px + SIDEBAR_W + 14;
        int cw = PANEL_W - SIDEBAR_W - 28;
        int y = py + 38;
        for (Row r : rows) {
            if (r.click(cx, y, cw, mx, my)) {
                active = r;
                return true;
            }
            y += r.h + 6;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (active != null) {
            int cx = px + SIDEBAR_W + 14;
            int cw = PANEL_W - SIDEBAR_W - 28;
            active.drag(cx, cw, mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (active != null) {
            active = null;
            cfg.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (WinVisuals.openKey().matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        cfg.save();
        super.onClose();
    }

    // ---------------------------------------------------------------- tabs

    private void buildTab() {
        rows.clear();
        active = null;
        switch (TABS[tab]) {
            case "sky" -> {
                rows.add(toggle("aurora", () -> cfg.auroraSky, v -> cfg.auroraSky = v));
                rows.add(toggle("stars", () -> cfg.skyStars, v -> cfg.skyStars = v));
                rows.add(toggle("nebula", () -> cfg.skyNebula, v -> cfg.skyNebula = v));
                rows.add(toggle("fog", () -> cfg.skyFog, v -> cfg.skyFog = v));
                rows.add(slider("intensity", 0.1f, 2f, 0.1f, () -> cfg.skyIntensity, v -> cfg.skyIntensity = v, true));
            }
            case "visuals" -> {
                rows.add(toggle("trails", () -> cfg.trails, v -> cfg.trails = v));
                rows.add(toggle("sprint", () -> cfg.trailOnlySprint, v -> cfg.trailOnlySprint = v));
                rows.add(toggle("elytra", () -> cfg.trailElytra, v -> cfg.trailElytra = v));
                rows.add(toggle("hits", () -> cfg.hitEffects, v -> cfg.hitEffects = v));
                rows.add(toggle("jump", () -> cfg.jumpEffects, v -> cfg.jumpEffects = v));
                rows.add(toggle("landing", () -> cfg.landingEffects, v -> cfg.landingEffects = v));
            }
            case "particles" -> {
                rows.add(modes());
                rows.add(swatches());
                rows.add(slider("speed", 0.1f, 5f, 0.1f, () -> cfg.colorSpeed, v -> cfg.colorSpeed = v, true));
                rows.add(slider("size", 0.3f, 3f, 0.1f, () -> cfg.particleSize, v -> cfg.particleSize = v, true));
                rows.add(slider("density", 0, 10, 1, () -> (float) cfg.trailDensity, v -> cfg.trailDensity = Math.round(v), false));
            }
            case "interface" -> {
                rows.add(toggle("hud", () -> cfg.showHud, v -> cfg.showHud = v));
                rows.add(toggle("animations", () -> cfg.uiAnimations, v -> cfg.uiAnimations = v));
                rows.add(toggle("compact", () -> cfg.compactUi, v -> cfg.compactUi = v));
                rows.add(slider("scale", 0.75f, 1.5f, 0.05f, () -> cfg.uiScale, v -> cfg.uiScale = v, true));
                rows.add(toggle("others", () -> cfg.showOthers, v -> cfg.showOthers = v));
            }
            case "settings" -> {
                rows.add(toggle("enabled", () -> cfg.enabled, v -> cfg.enabled = v));
                rows.add(toggle("reduced", () -> cfg.reducedParticles, v -> cfg.reducedParticles = v));
                rows.add(slider("dim", 0.2f, 0.9f, 0.05f, () -> cfg.backgroundDim, v -> cfg.backgroundDim = v, true));
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- rows

    private abstract class Row {
        final int h;
        Row(int h) { this.h = h; }
        abstract void render(GuiGraphics g, int x, int y, int w, int mx, int my, float alpha);
        abstract boolean click(int x, int y, int w, double mx, double my);
        void drag(int x, int w, double mx) { }
    }

    private Row toggle(String key, BooleanSupplier get, Consumer<Boolean> set) {
        return new Row(24) {
            float anim = get.getAsBoolean() ? 1f : 0f;

            @Override
            void render(GuiGraphics g, int x, int y, int w, int mx, int my, float alpha) {
                boolean hov = inside(mx, my, x, y, w, h);
                rrect(g, x, y, w, h, 4, a(hov ? 0x2CFFFFFF : 0x1AFFFFFF, alpha));
                g.drawString(font, t("opt." + key), x + 8, y + 8, a(TEXT, alpha), false);
                anim += ((get.getAsBoolean() ? 1f : 0f) - anim) * 0.3f;
                int sw = 28, sh = 12;
                int sx = x + w - sw - 8, sy = y + (h - sh) / 2;
                int on = accent(0f);
                rrect(g, sx, sy, sw, sh, 5, a(blend(0xFF3A4054, on, anim), alpha));
                int kx = sx + 2 + (int) ((sw - sh) * anim);
                rrect(g, kx, sy + 2, sh - 4, sh - 4, 3, a(0xFFFFFFFF, alpha));
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                if (!inside(mx, my, x, y, w, h)) return false;
                set.accept(!get.getAsBoolean());
                cfg.save();
                return true;
            }

            @Override
            void drag(int x, int w, double mx) { }
        };
    }

    private Row slider(String key, float min, float max, float step, Supplier<Float> get, Consumer<Float> set, boolean decimal) {
        return new Row(30) {
            @Override
            void render(GuiGraphics g, int x, int y, int w, int mx, int my, float alpha) {
                boolean hov = inside(mx, my, x, y, w, h);
                rrect(g, x, y, w, h, 4, a(hov ? 0x2CFFFFFF : 0x1AFFFFFF, alpha));
                g.drawString(font, t("opt." + key), x + 8, y + 6, a(TEXT, alpha), false);
                float v = get.get();
                String s = decimal ? String.format("%.1f", v) : String.valueOf(Math.round(v));
                g.drawString(font, s, x + w - 8 - font.width(s), y + 6, a(TEXT_DIM, alpha), false);
                int tx = x + 8, tw = w - 16, ty = y + 20;
                float p = (v - min) / (max - min);
                rrect(g, tx, ty, tw, 4, 2, a(0xFF2A3042, alpha));
                int fw = Math.max(3, (int) (tw * p));
                for (int i = 0; i < fw; i++) {
                    g.fill(tx + i, ty, tx + i + 1, ty + 4, a(accent((float) i / tw), alpha));
                }
                rrect(g, tx + fw - 3, ty - 2, 6, 8, 3, a(0xFFFFFFFF, alpha));
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                if (!inside(mx, my, x, y, w, h)) return false;
                drag(x, w, mx);
                return true;
            }

            @Override
            void drag(int x, int w, double mx) {
                float p = (float) ((mx - (x + 8)) / (w - 16));
                p = Math.max(0f, Math.min(1f, p));
                float v = min + p * (max - min);
                v = Math.round(v / step) * step;
                set.accept(Math.max(min, Math.min(max, v)));
            }
        };
    }

    private Row modes() {
        return new Row(24) {
            @Override
            void render(GuiGraphics g, int x, int y, int w, int mx, int my, float alpha) {
                ColorMode[] all = ColorMode.values();
                int cw = (w - 6 * (all.length - 1)) / all.length;
                for (int i = 0; i < all.length; i++) {
                    int bx = x + i * (cw + 6);
                    boolean sel = cfg.colorMode == all[i];
                    boolean hov = inside(mx, my, bx, y, cw, h);
                    rrect(g, bx, y, cw, h, 4,
                        a(sel ? ((accent(0f) & 0xFFFFFF) | 0x66000000) : (hov ? 0x2CFFFFFF : 0x1AFFFFFF), alpha));
                    String s = t("mode." + all[i].name().toLowerCase());
                    g.drawString(font, s, bx + (cw - font.width(s)) / 2, y + 8, a(sel ? TEXT : TEXT_DIM, alpha), false);
                }
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                ColorMode[] all = ColorMode.values();
                int cw = (w - 6 * (all.length - 1)) / all.length;
                for (int i = 0; i < all.length; i++) {
                    if (inside(mx, my, x + i * (cw + 6), y, cw, h)) {
                        cfg.colorMode = all[i];
                        cfg.save();
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private Row swatches() {
        return new Row(28) {
            @Override
            void render(GuiGraphics g, int x, int y, int w, int mx, int my, float alpha) {
                rrect(g, x, y, w, h, 4, a(0x1AFFFFFF, alpha));
                int gap = (w - 16 - PRESETS.length * 16) / (PRESETS.length - 1);
                for (int i = 0; i < PRESETS.length; i++) {
                    int sx = x + 8 + i * (16 + gap), sy = y + 6;
                    boolean sel = cfg.solidRgb() == PRESETS[i];
                    if (sel) rrect(g, sx - 2, sy - 2, 20, 20, 5, a(0xFFFFFFFF, alpha));
                    rrect(g, sx, sy, 16, 16, 4, a(0xFF000000 | PRESETS[i], alpha));
                }
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                int gap = (w - 16 - PRESETS.length * 16) / (PRESETS.length - 1);
                for (int i = 0; i < PRESETS.length; i++) {
                    if (inside(mx, my, x + 8 + i * (16 + gap), y + 6, 16, 16)) {
                        cfg.color = String.format("%06X", PRESETS[i]);
                        if (cfg.colorMode == ColorMode.RAINBOW) cfg.colorMode = ColorMode.SOLID;
                        cfg.save();
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private Row preview() {
        return new Row(22) {
            @Override
            void render(GuiGraphics g, int x, int y, int w, int mx, int my, float alpha) {
                rrect(g, x, y, w, h, 4, a(0x1AFFFFFF, alpha));
                int n = w - 12;
                for (int i = 0; i < n; i++) {
                    g.fill(x + 6 + i, y + 6, x + 7 + i, y + h - 6, a(accent((float) i / n), alpha));
                }
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) { return false; }
        };
    }

    // ---------------------------------------------------------------- helpers

    private static String t(String key) {
        return I18n.get("winvisuals.gui." + key);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Accent colour follows the configured palette, so the UI previews it. */
    private static int accent(float offset) {
        return 0xFF000000 | Palette.rgb(System.currentTimeMillis() / 50L, offset);
    }

    private static int a(int argb, float alpha) {
        int base = argb >>> 24;
        int na = Math.max(0, Math.min(255, (int) (base * alpha)));
        return (na << 24) | (argb & 0xFFFFFF);
    }

    private static int blend(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Rounded rectangle built from scanlines, good enough for radii up to ~8. */
    private static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int dy = r - i;
            int inset = r - (int) Math.floor(Math.sqrt((double) r * r - (double) dy * dy + 0.0001));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }
}

package ru.winlaunch.visuals.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import ru.winlaunch.visuals.GameTuner;
import ru.winlaunch.visuals.KillEffects;
import ru.winlaunch.visuals.PerfGovernor;
import ru.winlaunch.visuals.VisualsConfig;
import ru.winlaunch.visuals.VisualsConfig.ColorMode;
import ru.winlaunch.visuals.VisualsConfig.KillMode;
import ru.winlaunch.visuals.WinVisuals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Win Visuals settings screen. Opens on Right Shift. A black "liquid glass" window: sidebar with the
 * logo and five tabs, a header with window buttons, an animated sky preview and rows with icons.
 */
public class WinVisualsScreen extends Screen {
    // ---- layout (GUI units)
    private static final int PW = 400;
    private static final int PH = 232;
    private static final int SW = 96;
    private static final int PAD = 8;
    private static final int GAP = 2;
    private static final int PREVIEW_H = 44;

    // ---- colours
    private static final int PANEL = 0xDC0C0C0F;
    private static final int SIDEBAR = 0xE6070709;
    private static final int ROW = 0xA6000000;
    private static final int BORDER = 0x40FFFFFF;
    private static final int BORDER_HOT = 0x99FFFFFF;
    private static final int TEXT = 0xFFF5F5F7;
    private static final int TEXT_DIM = 0xFF8E8E96;

    private static final String[] TABS = {"sky", "visuals", "combat", "particles", "cosmetics", "interface", "performance", "settings"};
    private static final String[] TAB_ICONS = {"moon", "sparkle", "burst", "particles", "star", "monitor", "bolt", "gear"};
    private static final int[] PRESETS = {
        0xFFFFFF, 0xC8C8CE, 0xFF8FB8, 0xFF5C8A, 0xB48CFF, 0x6FB8FF, 0x5CF2C0, 0xFFD86B
    };
    private static final int[] AURORA = {0x2CFF9A, 0x27D8F0, 0x8C6CFF};

    private static final Identifier FILL = tex("round_fill");
    private static final Identifier STROKE = tex("round_stroke");
    private static final Identifier LOGO = tex("logo");
    private static final Identifier GLOW = tex("glow");

    private final VisualsConfig cfg = VisualsConfig.get();
    private final long openedAt = System.currentTimeMillis();
    private final List<Row> rows = new ArrayList<>();
    private int tab = 0;
    private long tabChangedAt = 0;
    private Row active;
    private boolean collapsed;
    private int px, py;
    private float scale = 1f;
    private long flashUntil;

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
        float open = cfg.uiAnimations ? Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f) : 1f;
        int alpha = Math.max(0, Math.min(220, (int) (cfg.backgroundDim * 255f * open)));
        g.fill(0, 0, this.width, this.height, (alpha << 24));
    }

    // ---------------------------------------------------------------- layout helpers

    private float fit() {
        return Math.min(1f, Math.min((this.width - 8f) / PW, (this.height - 8f) / PH));
    }

    private double toX(double mx) {
        return (mx - this.width / 2.0) / scale + this.width / 2.0;
    }

    private double toY(double my) {
        return (my - this.height / 2.0) / scale + this.height / 2.0;
    }

    private int panelH() {
        return collapsed ? 30 : PH;
    }

    private int contentX() {
        return px + SW + PAD;
    }

    private int contentW() {
        return PW - SW - PAD * 2;
    }

    private boolean hasPreview() {
        return TABS[tab].equals("sky") && !cfg.compactUi;
    }

    private int rowsTop() {
        return py + 36 + (hasPreview() ? PREVIEW_H + 4 : 0);
    }

    private int rowGap() {
        return cfg.compactUi ? 1 : GAP;
    }

    private int buttonsY() {
        return py + PH - PAD - 18;
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float open = cfg.uiAnimations ? Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f) : 1f;
        float ease = 1f - (1f - open) * (1f - open);
        renderBackground(g, mouseX, mouseY, partialTick);

        scale = Math.max(0.3f, cfg.uiScale * fit());
        double mx = toX(mouseX);
        double my = toY(mouseY);
        g.pose().pushMatrix();
        g.pose().translate(this.width / 2.0f, this.height / 2.0f);
        g.pose().scale(scale, scale);
        g.pose().translate(-this.width / 2.0f, -this.height / 2.0f);

        px = (this.width - PW) / 2;
        py = (this.height - PH) / 2 + (int) ((1f - ease) * 10);

        int h = panelH();
        // window body
        panel(g, FILL, px - 1, py - 1, PW + 2, h + 2, 9, a(0x30000000, ease));
        panel(g, FILL, px, py, PW, h, 8, a(PANEL, ease));
        panel(g, STROKE, px, py, PW, h, 8, a(0x5CFFFFFF, ease));

        if (collapsed) {
            renderBar(g, mx, my, ease);
        } else {
            renderSidebar(g, mx, my, ease);
            renderHeader(g, mx, my, ease);
            renderContent(g, mx, my, ease);
            renderButtons(g, mx, my, ease);
        }
        g.pose().popMatrix();
    }

    private void renderBar(GuiGraphics g, double mx, double my, float ease) {
        icon(g, LOGO, px + 8, py + 4, 22, 0xFFFFFFFF, ease);
        text(g, "WinVisuals", px + 36, py + 10, a(TEXT, ease), 1f, true);
        windowButtons(g, mx, my, ease);
    }

    private void renderSidebar(GuiGraphics g, double mx, double my, float ease) {
        panel(g, FILL, px + 4, py + 4, SW - 4, PH - 8, 6, a(SIDEBAR, ease));
        panel(g, STROKE, px + 4, py + 4, SW - 4, PH - 8, 6, a(0x2EFFFFFF, ease));

        int sx = px + 4;
        int sw = SW - 4;
        icon(g, LOGO, sx + (sw - 46) / 2, py + 10, 46, 0xFFFFFFFF, ease);
        String name = "WinVisuals";
        int nameW = (int) (font.width(name) * 1.2f);
        text(g, name, sx + (sw - nameW) / 2, py + 60, a(TEXT, ease), 1.2f, true);
        String sub = t("subtitle");
        float subScale = Math.min(0.75f, (sw - 10f) / Math.max(1, font.width(sub)));
        int subW = (int) (font.width(sub) * subScale);
        text(g, sub, sx + (sw - subW) / 2, py + 73, a(TEXT_DIM, ease), subScale, false);

        for (int i = 0; i < TABS.length; i++) {
            int ty = navY(i);
            boolean sel = i == tab;
            boolean hov = inside(mx, my, sx + 6, ty, sw - 12, 13);
            panel(g, FILL, sx + 6, ty, sw - 12, 13, 6,
                a(sel ? 0x38FFFFFF : (hov ? 0x1CFFFFFF : 0x66000000), ease));
            panel(g, STROKE, sx + 6, ty, sw - 12, 13, 6, a(sel ? 0xE6FFFFFF : (hov ? 0x66FFFFFF : 0x33FFFFFF), ease));
            icon(g, tex("icon_" + TAB_ICONS[i]), sx + 13, ty + 1, 11, sel ? 0xFFFFFFFF : 0xFFB4B4BC, ease);
            text(g, t("tab." + TABS[i]), sx + 30, ty + 3, a(sel ? TEXT : 0xFFC4C4CA, ease), 1f, false);
        }

        // footer card
        int fy = py + PH - 4 - 30;
        panel(g, FILL, sx + 6, fy, sw - 12, 24, 6, a(0x66000000, ease));
        panel(g, STROKE, sx + 6, fy, sw - 12, 24, 6, a(0x33FFFFFF, ease));
        icon(g, tex("icon_cube"), sx + 12, fy + 6, 12, 0xFFD0D0D6, ease);
        text(g, "Minecraft " + mcVersion(), sx + 29, fy + 5, a(0xFFD8D8DE, ease), 0.7f, false);
        text(g, "Fabric", sx + 29, fy + 14, a(TEXT_DIM, ease), 0.7f, false);
    }

    private int navY(int i) {
        return py + 80 + i * 14;
    }

    private void renderHeader(GuiGraphics g, double mx, double my, float ease) {
        int cx = contentX();
        icon(g, tex("icon_" + TAB_ICONS[tab]), cx, py + 8, 18, 0xFFFFFFFF, ease);
        text(g, t("tab." + TABS[tab]), cx + 25, py + 8, a(TEXT, ease), 1.5f, true);
        text(g, t("tabdesc." + TABS[tab]), cx + 25, py + 22, a(TEXT_DIM, ease), 0.8f, false);
        windowButtons(g, mx, my, ease);
    }

    private void windowButtons(GuiGraphics g, double mx, double my, float ease) {
        int cx = closeX();
        int by = py + 7;
        boolean hovClose = inside(mx, my, cx, by, 16, 16);
        boolean hovMin = inside(mx, my, cx - 20, by, 16, 16);
        circleButton(g, cx - 20, by, hovMin, ease, "minus");
        circleButton(g, cx, by, hovClose, ease, "close");
    }

    private int closeX() {
        return px + PW - PAD - 16;
    }

    private void circleButton(GuiGraphics g, int x, int y, boolean hov, float ease, String icon) {
        panel(g, FILL, x, y, 16, 16, 8, a(hov ? 0x55FFFFFF : 0x99000000, ease));
        panel(g, STROKE, x, y, 16, 16, 8, a(hov ? 0xCCFFFFFF : 0x55FFFFFF, ease));
        icon(g, tex("icon_" + icon), x + 4, y + 4, 8, 0xFFFFFFFF, ease);
    }

    private void renderContent(GuiGraphics g, double mx, double my, float ease) {
        int cx = contentX();
        int cw = contentW();
        float slide = 1f;
        if (cfg.uiAnimations && tabChangedAt > 0) {
            slide = Math.min(1f, (System.currentTimeMillis() - tabChangedAt) / 160f);
            slide = 1f - (1f - slide) * (1f - slide);
        }
        float ta = ease * (0.25f + 0.75f * slide);
        int off = (int) ((1f - slide) * 6);

        if (hasPreview()) {
            drawPreview(g, cx, py + 36, cw, PREVIEW_H, ta);
        }
        int y = rowsTop() + off;
        for (Row r : rows) {
            r.render(g, cx, y, cw, mx, my, ta);
            y += r.h() + rowGap();
        }
    }

    private void renderButtons(GuiGraphics g, double mx, double my, float ease) {
        int cx = contentX();
        int cw = contentW();
        int by = buttonsY();
        int rw = Math.min(150, cw / 2 - 4);
        int aw = 96;
        button(g, cx, by, rw, 18, "refresh", t("reset"), inside(mx, my, cx, by, rw, 18), false, ease);
        boolean flash = System.currentTimeMillis() < flashUntil;
        button(g, cx + cw - aw, by, aw, 18, "check", t("apply"),
            inside(mx, my, cx + cw - aw, by, aw, 18), flash, ease);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String icon, String label, boolean hov,
                        boolean flash, float ease) {
        panel(g, FILL, x, y, w, h, 7, a(flash ? 0x55FFFFFF : (hov ? 0x33FFFFFF : 0x99000000), ease));
        panel(g, STROKE, x, y, w, h, 7, a(hov || flash ? 0xDDFFFFFF : 0x66FFFFFF, ease));
        int tw = font.width(label);
        int total = 12 + 5 + tw;
        int bx = x + (w - total) / 2;
        icon(g, tex("icon_" + icon), bx, y + 3, 12, 0xFFFFFFFF, ease);
        text(g, label, bx + 17, y + 5, a(TEXT, ease), 1f, false);
    }

    // ---------------------------------------------------------------- sky preview

    private void drawPreview(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        float t = (System.currentTimeMillis() % 3_600_000L) / 1000f;
        float intensity = Math.max(0.1f, Math.min(1f, cfg.skyIntensity));
        int r = 6;
        g.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < w; i++) {
            int inset = cornerInset(i, w, r);
            int y0 = y + inset;
            int y1 = y + h - inset;
            g.fillGradient(x + i, y0, x + i + 1, y1, a(0xFF03040A, alpha), a(0xFF121729, alpha));

            if (cfg.auroraSky) {
                for (int k = 0; k < 3; k++) {
                    float centre = y + h * (0.52f + 0.09f * k)
                        + (float) Math.sin(i * 0.034f * (1f + 0.25f * k) + t * (0.5f + 0.15f * k) + k * 1.7f) * h * 0.12f;
                    float ray = 0.5f + 0.5f * (float) Math.sin(i * 0.37f + t * (1.3f + 0.2f * k) + k * 5f);
                    float env = 0.55f + 0.45f * (float) Math.sin(i * 0.05f - t * 0.4f + k * 2.1f);
                    float amp = (0.35f + 0.65f * ray) * env * intensity;
                    int top = Math.max(y0, (int) (centre - h * 0.46f * (0.6f + 0.4f * ray)));
                    int mid = Math.max(y0, Math.min(y1, (int) centre));
                    int c = AURORA[k];
                    int cTop = AURORA[(k + 2) % 3];
                    if (mid > top) {
                        g.fillGradient(x + i, top, x + i + 1, mid, a(argb(0, cTop), alpha), a(argb((int) (150 * amp), c), alpha));
                    }
                    int low = Math.min(y1, mid + 5);
                    if (low > mid) {
                        g.fillGradient(x + i, mid, x + i + 1, low, a(argb((int) (110 * amp), c), alpha), a(argb(0, c), alpha));
                    }
                }
            }

            // pine silhouettes along the bottom
            float tri = Math.abs((i % 9) / 4.5f - 1f);
            float envTree = 0.5f + 0.5f * (float) Math.sin(i * 0.05f + 2f);
            int th = (int) (h * 0.08f + (1f - tri) * h * 0.10f * envTree + envTree * h * 0.05f);
            g.fill(x + i, Math.max(y0, y1 - th), x + i + 1, y1, a(0xFF020204, alpha));
        }

        if (cfg.skyNebula) {
            blit(g, GLOW, x + (int) (w * 0.08f), y - 4, (int) (w * 0.5f), h + 8, a(0x40603CB4, alpha));
            blit(g, GLOW, x + (int) (w * 0.46f), y + 2, (int) (w * 0.45f), h, a(0x303060A8, alpha));
        }
        if (cfg.skyStars) {
            Random rnd = new Random(1207);
            for (int i = 0; i < 70; i++) {
                int sx = x + rnd.nextInt(w);
                int sy = y + 2 + rnd.nextInt((int) (h * 0.62f));
                float tw = 0.55f + 0.45f * (float) Math.sin(t * (1.2f + rnd.nextFloat() * 2.5f) + rnd.nextFloat() * 20f);
                int al = (int) (255 * tw * (0.4f + 0.6f * rnd.nextFloat()));
                g.fill(sx, sy, sx + 1, sy + 1, a(argb(al, 0xDCE6FF), alpha));
                if (i % 11 == 0) {
                    g.fill(sx - 1, sy, sx + 2, sy + 1, a(argb(al / 3, 0xDCE6FF), alpha));
                    g.fill(sx, sy - 1, sx + 1, sy + 2, a(argb(al / 3, 0xDCE6FF), alpha));
                }
            }
        }
        // moon
        int mxp = x + (int) (w * 0.78f);
        int myp = y + (int) (h * 0.26f);
        blit(g, GLOW, mxp - 14, myp - 14, 28, 28, a(0x55FFFFFF, alpha));
        g.fill(mxp - 3, myp - 3, mxp + 3, myp + 3, a(0xFFF2F2F6, alpha));
        g.fill(mxp - 1, myp - 2, mxp + 2, myp, a(0xFFC8C8D0, alpha));
        g.disableScissor();
        panel(g, STROKE, x, y, w, h, r, a(0x66FFFFFF, alpha));
    }

    private static int cornerInset(int i, int w, int r) {
        int d = Math.min(i, w - 1 - i);
        if (d >= r) return 0;
        double dx = r - d - 0.5;
        return r - (int) Math.floor(Math.sqrt(Math.max(0.0, r * r - dx * dx)));
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = toX(event.x());
        double my = toY(event.y());

        int cx = closeX();
        int by = py + 7;
        if (inside(mx, my, cx, by, 16, 16)) {
            onClose();
            return true;
        }
        if (inside(mx, my, cx - 20, by, 16, 16)) {
            collapsed = !collapsed;
            active = null;
            return true;
        }
        if (collapsed) return true;

        for (int i = 0; i < TABS.length; i++) {
            if (inside(mx, my, px + 10, navY(i), SW - 16, 13)) {
                selectTab(i);
                return true;
            }
        }

        int bx = contentX();
        int bw = contentW();
        int rw = Math.min(150, bw / 2 - 4);
        if (inside(mx, my, bx, buttonsY(), rw, 18)) {
            resetAll();
            return true;
        }
        if (inside(mx, my, bx + bw - 96, buttonsY(), 96, 18)) {
            cfg.save();
            flashUntil = System.currentTimeMillis() + 350;
            onClose();
            return true;
        }

        int y = rowsTop();
        for (Row r : rows) {
            if (r.click(bx, y, bw, mx, my)) {
                active = r;
                return true;
            }
            y += r.h() + rowGap();
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (active != null) {
            active.drag(contentX(), contentW(), toX(event.x()));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (active != null) {
            active = null;
            cfg.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (WinVisuals.openKey().matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        cfg.save();
        super.onClose();
    }

    /** Closes the menu and plays the selected kill effect a few blocks in front of the player. */
    private void previewKill() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
            KillEffects.start(mc.level, mc.player.getX() + look.x * 4.0, mc.player.getY(), mc.player.getZ() + look.z * 4.0,
                1.8, cfg.killMode);
        }
        onClose();
    }

    private void resetAll() {
        cfg.resetToDefaults();
        cfg.save();
        buildTab();
    }

    // ---------------------------------------------------------------- tabs

    /** Switches to a tab by index (used by the in-CI game test as well). */
    public void selectTab(int index) {
        if (index >= 0 && index < TABS.length && index != tab) {
            tab = index;
            tabChangedAt = System.currentTimeMillis();
            buildTab();
        }
    }

    private void buildTab() {
        rows.clear();
        active = null;
        switch (TABS[tab]) {
            case "sky" -> {
                rows.add(toggle("aurora", "aurora", () -> cfg.auroraSky, v -> cfg.auroraSky = v));
                rows.add(slider("sun", "intensity", 0.1f, 2f, 0.05f, () -> cfg.skyIntensity, v -> cfg.skyIntensity = v, Fmt.PERCENT));
                rows.add(toggle("star", "stars", () -> cfg.skyStars, v -> cfg.skyStars = v));
                rows.add(toggle("spiral", "nebula", () -> cfg.skyNebula, v -> cfg.skyNebula = v));
                rows.add(toggle("cloud", "fog", () -> cfg.skyFog, v -> cfg.skyFog = v));
            }
            case "visuals" -> {
                rows.add(toggle("sun", "nightvision", () -> cfg.nightVision, v -> cfg.nightVision = v));
                rows.add(toggle("particles", "trails", () -> cfg.trails, v -> cfg.trails = v));
                rows.add(toggle("bolt", "sprint", () -> cfg.trailOnlySprint, v -> cfg.trailOnlySprint = v));
                rows.add(toggle("sparkle", "elytra", () -> cfg.trailElytra, v -> cfg.trailElytra = v));
                rows.add(toggle("burst", "hits", () -> cfg.hitEffects, v -> cfg.hitEffects = v));
                rows.add(toggle("arrow_up", "jump", () -> cfg.jumpEffects, v -> cfg.jumpEffects = v));
                rows.add(toggle("ring", "landing", () -> cfg.landingEffects, v -> cfg.landingEffects = v));
            }
            case "combat" -> {
                rows.add(toggle("users", "targetesp", () -> cfg.targetEsp, v -> cfg.targetEsp = v));
                rows.add(toggle("monitor", "targethud", () -> cfg.targetHud, v -> cfg.targetHud = v));
                rows.add(toggle("burst", "killeffect", () -> cfg.killEffects, v -> cfg.killEffects = v));
                rows.add(action("sparkle", "killmode", t("kill." + cfg.killMode.name().toLowerCase()), () -> {
                    KillMode[] all = KillMode.values();
                    cfg.killMode = all[(cfg.killMode.ordinal() + 1) % all.length];
                    cfg.save();
                    buildTab();
                }));
                rows.add(toggle("bolt", "killsound", () -> cfg.killSound, v -> cfg.killSound = v));
                rows.add(action("check", "killtest", t("do.test"), this::previewKill));
            }
            case "particles" -> {
                rows.add(modes());
                rows.add(swatches());
                rows.add(slider("bolt", "speed", 0.1f, 5f, 0.1f, () -> cfg.colorSpeed, v -> cfg.colorSpeed = v, Fmt.TIMES));
                rows.add(slider("ring", "size", 0.3f, 3f, 0.1f, () -> cfg.particleSize, v -> cfg.particleSize = v, Fmt.TIMES));
                rows.add(slider("particles", "density", 0, 10, 1, () -> (float) cfg.trailDensity, v -> cfg.trailDensity = Math.round(v), Fmt.INT));
                rows.add(toggle("sparkle", "reduced", () -> cfg.reducedParticles, v -> cfg.reducedParticles = v));
            }
            case "cosmetics" -> {
                rows.add(toggle("ring", "halo", () -> cfg.cosmeticHalo, v -> cfg.cosmeticHalo = v));
                rows.add(toggle("burst", "hat", () -> cfg.cosmeticHat, v -> cfg.cosmeticHat = v));
                rows.add(toggle("sparkle", "wings", () -> cfg.cosmeticWings, v -> cfg.cosmeticWings = v));
            }
            case "interface" -> {
                rows.add(toggle("sparkle", "island", () -> cfg.showIsland, v -> cfg.showIsland = v));
                rows.add(toggle("monitor", "hud", () -> cfg.showHud, v -> cfg.showHud = v));
                rows.add(toggle("sparkle", "animations", () -> cfg.uiAnimations, v -> cfg.uiAnimations = v));
                rows.add(toggle("grid", "compact", () -> cfg.compactUi, v -> cfg.compactUi = v));
                rows.add(slider("expand", "scale", 0.75f, 1.5f, 0.05f, () -> cfg.uiScale, v -> cfg.uiScale = v, Fmt.PERCENT));
                rows.add(slider("moon", "dim", 0.2f, 0.9f, 0.05f, () -> cfg.backgroundDim, v -> cfg.backgroundDim = v, Fmt.PERCENT));
                rows.add(toggle("users", "others", () -> cfg.showOthers, v -> cfg.showOthers = v));
            }
            case "performance" -> {
                rows.add(action("bolt", "perfmode", t("perf." + cfg.perfMode.name().toLowerCase()), () -> {
                    VisualsConfig.PerfMode[] all = VisualsConfig.PerfMode.values();
                    cfg.perfMode = all[(cfg.perfMode.ordinal() + 1) % all.length];
                    cfg.perfProfileApplied = 0;
                    cfg.save();
                    buildTab();
                }));
                rows.add(slider("monitor", "targetfps", 30, 144, 5, () -> (float) cfg.targetFps, v -> cfg.targetFps = Math.round(v), Fmt.INT));
                rows.add(toggle("gear", "tune", () -> cfg.tuneGameSettings, v -> cfg.tuneGameSettings = v));
                rows.add(action("refresh", "applynow", t("do.apply"), () -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    GameTuner.apply(mc, cfg);
                    flashUntil = System.currentTimeMillis() + 500;
                }));
                rows.add(toggle("users", "showfps", () -> cfg.showFps, v -> cfg.showFps = v));
                rows.add(info("cube", "stats", Math.round(PerfGovernor.fps()) + " FPS, Q" + (PerfGovernor.MAX_LEVEL - PerfGovernor.level())));
            }
            case "settings" -> {
                rows.add(toggle("gear", "enabled", () -> cfg.enabled, v -> cfg.enabled = v));
                rows.add(action("check", "save", t("do.save"), () -> {
                    cfg.save();
                    flashUntil = System.currentTimeMillis() + 500;
                }));
                rows.add(action("refresh", "reset", t("do.reset"), this::resetAll));
                rows.add(info("cube", "config", configPath()));
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- rows

    private enum Fmt { PERCENT, TIMES, INT }

    private abstract class Row {
        final String icon;
        final String key;

        Row(String icon, String key) {
            this.icon = icon;
            this.key = key;
        }

        int h() {
            return cfg.compactUi ? 17 : 22;
        }

        abstract void render(GuiGraphics g, int x, int y, int w, double mx, double my, float alpha);

        abstract boolean click(int x, int y, int w, double mx, double my);

        void drag(int x, int w, double mx) { }

        /** Background, icon and the two text lines. */
        void base(GuiGraphics g, int x, int y, int w, boolean hov, float alpha) {
            panel(g, FILL, x, y, w, h(), 6, a(hov ? 0xB0141418 : ROW, alpha));
            panel(g, STROKE, x, y, w, h(), 6, a(hov ? BORDER_HOT : BORDER, alpha));
            if (icon != null) {
                icon(g, tex("icon_" + icon), x + 7, y + (h() - 12) / 2, 12, 0xFFE6E6EA, alpha);
            }
            if (cfg.compactUi) {
                text(g, t("opt." + key), x + 25, y + (h() - 8) / 2, a(TEXT, alpha), 1f, false);
            } else {
                text(g, t("opt." + key), x + 25, y + 3, a(TEXT, alpha), 1f, false);
                text(g, t("desc." + key), x + 25, y + 13, a(TEXT_DIM, alpha), 0.75f, false);
            }
        }
    }

    private Row toggle(String icon, String key, BooleanSupplier get, Consumer<Boolean> set) {
        return new Row(icon, key) {
            float anim = get.getAsBoolean() ? 1f : 0f;

            @Override
            void render(GuiGraphics g, int x, int y, int w, double mx, double my, float alpha) {
                boolean hov = inside(mx, my, x, y, w, h());
                base(g, x, y, w, hov, alpha);
                anim += ((get.getAsBoolean() ? 1f : 0f) - anim) * 0.3f;
                int sw = 26, sh = 12;
                int sx = x + w - sw - 8, sy = y + (h() - sh) / 2;
                panel(g, FILL, sx, sy, sw, sh, 6, a(blend(0xFF2B2B30, 0xFF7C7C84, anim), alpha));
                panel(g, STROKE, sx, sy, sw, sh, 6, a(0x55FFFFFF, alpha));
                int kx = sx + 1 + (int) ((sw - sh) * anim);
                panel(g, FILL, kx, sy + 1, sh - 2, sh - 2, 5, a(0xFFF4F4F7, alpha));
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                if (!inside(mx, my, x, y, w, h())) return false;
                set.accept(!get.getAsBoolean());
                cfg.save();
                return true;
            }
        };
    }

    private Row slider(String icon, String key, float min, float max, float step, Supplier<Float> get,
                       Consumer<Float> set, Fmt fmt) {
        return new Row(icon, key) {
            int trackX(int x, int w) {
                return x + (int) (w * 0.46f);
            }

            int trackW(int x, int w) {
                return x + w - 38 - trackX(x, w);
            }

            @Override
            void render(GuiGraphics g, int x, int y, int w, double mx, double my, float alpha) {
                boolean hov = inside(mx, my, x, y, w, h());
                base(g, x, y, w, hov, alpha);
                float v = get.get();
                String s = switch (fmt) {
                    case PERCENT -> Math.round(v * 100f) + "%";
                    case TIMES -> String.format("%.1fx", v);
                    case INT -> String.valueOf(Math.round(v));
                };
                text(g, s, x + w - 8 - font.width(s), y + (h() - 8) / 2, a(TEXT, alpha), 1f, false);
                int tx = trackX(x, w);
                int tw = trackW(x, w);
                int ty = y + h() / 2 - 2;
                panel(g, FILL, tx, ty, tw, 4, 2, a(0xFF2A2A30, alpha));
                float p = Math.max(0f, Math.min(1f, (v - min) / (max - min)));
                int fw = Math.max(4, (int) (tw * p));
                for (int i = 0; i < fw; i++) {
                    float k = (float) i / Math.max(1, tw);
                    int c = blend(0xFFEDEDF2, gradient(k), 0.30f);
                    g.fill(tx + i, ty + 1, tx + i + 1, ty + 3, a(c, alpha));
                }
                panel(g, FILL, tx + fw - 4, ty - 2, 8, 8, 4, a(0xFFFFFFFF, alpha));
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                if (!inside(mx, my, trackX(x, w) - 6, y, trackW(x, w) + 12, h())) return false;
                drag(x, w, mx);
                return true;
            }

            @Override
            void drag(int x, int w, double mx) {
                int tx = trackX(x, w);
                float p = (float) ((mx - tx) / trackW(x, w));
                p = Math.max(0f, Math.min(1f, p));
                float v = min + p * (max - min);
                v = Math.round(v / step) * step;
                set.accept(Math.max(min, Math.min(max, v)));
            }
        };
    }

    private Row action(String icon, String key, String label, Runnable run) {
        return new Row(icon, key) {
            @Override
            void render(GuiGraphics g, int x, int y, int w, double mx, double my, float alpha) {
                boolean hov = inside(mx, my, x, y, w, h());
                base(g, x, y, w, hov, alpha);
                int bw = font.width(label) + 16;
                int bx = x + w - bw - 6;
                int by = y + (h() - 14) / 2;
                boolean hovB = inside(mx, my, bx, by, bw, 14);
                panel(g, FILL, bx, by, bw, 14, 6, a(hovB ? 0x44FFFFFF : 0x80000000, alpha));
                panel(g, STROKE, bx, by, bw, 14, 6, a(hovB ? 0xCCFFFFFF : 0x66FFFFFF, alpha));
                text(g, label, bx + 8, by + 3, a(TEXT, alpha), 1f, false);
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                int bw = font.width(label) + 16;
                if (!inside(mx, my, x + w - bw - 6, y + (h() - 14) / 2, bw, 14)) return false;
                run.run();
                return true;
            }
        };
    }

    private Row info(String icon, String key, String value) {
        return new Row(icon, key) {
            @Override
            void render(GuiGraphics g, int x, int y, int w, double mx, double my, float alpha) {
                base(g, x, y, w, false, alpha);
                text(g, value, x + w - 8 - (int) (font.width(value) * 0.75f), y + (h() - 6) / 2,
                    a(TEXT_DIM, alpha), 0.75f, false);
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                return false;
            }
        };
    }

    private Row modes() {
        return new Row(null, "mode") {
            @Override
            void render(GuiGraphics g, int x, int y, int w, double mx, double my, float alpha) {
                ColorMode[] all = ColorMode.values();
                int gap = 4;
                int cw = (w - gap * (all.length - 1)) / all.length;
                for (int i = 0; i < all.length; i++) {
                    int bx = x + i * (cw + gap);
                    boolean sel = cfg.colorMode == all[i];
                    boolean hov = inside(mx, my, bx, y, cw, h());
                    panel(g, FILL, bx, y, cw, h(), 6, a(sel ? 0x38FFFFFF : (hov ? 0x1CFFFFFF : ROW), alpha));
                    panel(g, STROKE, bx, y, cw, h(), 6, a(sel ? 0xE6FFFFFF : (hov ? BORDER_HOT : BORDER), alpha));
                    String s = t("mode." + all[i].name().toLowerCase());
                    text(g, s, bx + (cw - font.width(s)) / 2, y + (h() - 8) / 2, a(sel ? TEXT : 0xFFC4C4CA, alpha), 1f, false);
                }
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                ColorMode[] all = ColorMode.values();
                int gap = 4;
                int cw = (w - gap * (all.length - 1)) / all.length;
                for (int i = 0; i < all.length; i++) {
                    if (inside(mx, my, x + i * (cw + gap), y, cw, h())) {
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
        return new Row("palette", "color") {
            @Override
            void render(GuiGraphics g, int x, int y, int w, double mx, double my, float alpha) {
                panel(g, FILL, x, y, w, h(), 6, a(ROW, alpha));
                panel(g, STROKE, x, y, w, h(), 6, a(BORDER, alpha));
                icon(g, tex("icon_palette"), x + 7, y + (h() - 12) / 2, 12, 0xFFE6E6EA, alpha);
                int start = x + 28;
                int span = w - 28 - 8;
                int gap = (span - PRESETS.length * 12) / (PRESETS.length - 1);
                for (int i = 0; i < PRESETS.length; i++) {
                    int sx = start + i * (12 + gap), sy = y + (h() - 12) / 2;
                    boolean sel = cfg.solidRgb() == PRESETS[i];
                    if (sel) panel(g, STROKE, sx - 2, sy - 2, 16, 16, 8, a(0xFFFFFFFF, alpha));
                    panel(g, FILL, sx, sy, 12, 12, 6, a(0xFF000000 | PRESETS[i], alpha));
                }
            }

            @Override
            boolean click(int x, int y, int w, double mx, double my) {
                int start = x + 28;
                int span = w - 28 - 8;
                int gap = (span - PRESETS.length * 12) / (PRESETS.length - 1);
                for (int i = 0; i < PRESETS.length; i++) {
                    if (inside(mx, my, start + i * (12 + gap) - 1, y + (h() - 12) / 2 - 1, 14, 14)) {
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

    // ---------------------------------------------------------------- drawing helpers

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath("winvisuals", "textures/gui/" + name + ".png");
    }

    private static String t(String key) {
        return I18n.get("winvisuals.gui." + key);
    }

    private static String mcVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("1.21.11");
    }

    private static String configPath() {
        return ".minecraft/config/winvisuals.json";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int a(int argb, float alpha) {
        int base = argb >>> 24;
        int na = Math.max(0, Math.min(255, (int) (base * alpha)));
        return (na << 24) | (argb & 0xFFFFFF);
    }

    private static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private static int blend(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Aurora gradient (green, cyan, violet) sampled at k in 0..1. */
    private static int gradient(float k) {
        k = Math.max(0f, Math.min(1f, k)) * 2f;
        return k < 1f ? blend(0xFF000000 | AURORA[0], 0xFF000000 | AURORA[1], k)
            : blend(0xFF000000 | AURORA[1], 0xFF000000 | AURORA[2], k - 1f);
    }

    private void text(GuiGraphics g, String s, int x, int y, int color, float sc, boolean shadow) {
        if ((color >>> 24) < 6) return;
        if (sc == 1f) {
            g.drawString(font, s, x, y, color, shadow);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(sc, sc);
        g.drawString(font, s, 0, 0, color, shadow);
        g.pose().popMatrix();
    }

    private void icon(GuiGraphics g, Identifier id, int x, int y, int size, int color, float alpha) {
        int c = a(color, alpha);
        if ((c >>> 24) < 4) return;
        int tex = id == LOGO ? 256 : 64;
        g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, size, size, tex, tex, tex, tex, c);
    }

    private void blit(GuiGraphics g, Identifier id, int x, int y, int w, int h, int color) {
        if ((color >>> 24) < 3) return;
        g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h, 64, 64, 64, 64, color);
    }

    /** Nine-slice draw of a rounded-rect texture (24 px corners in a 64 px texture), tinted by colour. */
    private void panel(GuiGraphics g, Identifier tex, int x, int y, int w, int h, int r, int color) {
        if ((color >>> 24) < 3 || w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        final int c = 24, t = 64, m = t - 2 * c;
        var p = RenderPipelines.GUI_TEXTURED;
        g.blit(p, tex, x, y, 0f, 0f, r, r, c, c, t, t, color);
        g.blit(p, tex, x + w - r, y, (float) (t - c), 0f, r, r, c, c, t, t, color);
        g.blit(p, tex, x, y + h - r, 0f, (float) (t - c), r, r, c, c, t, t, color);
        g.blit(p, tex, x + w - r, y + h - r, (float) (t - c), (float) (t - c), r, r, c, c, t, t, color);
        if (w > 2 * r) {
            g.blit(p, tex, x + r, y, (float) c, 0f, w - 2 * r, r, m, c, t, t, color);
            g.blit(p, tex, x + r, y + h - r, (float) c, (float) (t - c), w - 2 * r, r, m, c, t, t, color);
        }
        if (h > 2 * r) {
            g.blit(p, tex, x, y + r, 0f, (float) c, r, h - 2 * r, c, m, t, t, color);
            g.blit(p, tex, x + w - r, y + r, (float) (t - c), (float) c, r, h - 2 * r, c, m, t, t, color);
        }
        if (w > 2 * r && h > 2 * r) {
            g.blit(p, tex, x + r, y + r, (float) c, (float) c, w - 2 * r, h - 2 * r, m, m, t, t, color);
        }
    }
}

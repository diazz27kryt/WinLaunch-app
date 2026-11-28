package ru.winlaunch.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ru.winlaunch.visuals.GlowParticle.Shape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side combat helpers: the Target ESP (particle marker and a small HUD card for the current target)
 * and the detection of kills that triggers {@link KillEffects}. Nothing here reads or sends anything to
 * the server, and nothing is drawn through walls.
 */
public final class CombatTracker {
    private CombatTracker() {}

    /** How long (ticks) an attacked entity is watched for its death. */
    private static final int WATCH_TICKS = 200;
    /** How long (ticks) the target stays selected after the last hit or after leaving the crosshair. */
    private static final int TARGET_TICKS = 100;
    private static final double MAX_DIST_SQ = 32.0 * 32.0;

    private static final class Watched {
        final LivingEntity entity;
        final long until;

        Watched(LivingEntity entity, long until) {
            this.entity = entity;
            this.until = until;
        }
    }

    private static final List<Watched> WATCHED = new ArrayList<>();
    private static LivingEntity target;
    private static long targetUntil;
    private static float shownHealth = -1f;

    public static void clear() {
        WATCHED.clear();
        target = null;
        shownHealth = -1f;
    }

    /** Called when the local player attacks an entity. */
    public static void onAttack(ClientLevel level, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        long now = level.getGameTime();
        for (Watched w : WATCHED) {
            if (w.entity == living) {
                WATCHED.remove(w);
                break;
            }
        }
        WATCHED.add(new Watched(living, now + WATCH_TICKS));
        target = living;
        targetUntil = now + TARGET_TICKS;
    }

    public static void tick(Minecraft mc, ClientLevel level, VisualsConfig cfg) {
        long now = level.getGameTime();

        // The entity under the crosshair becomes the target as well.
        if (mc.crosshairPickEntity instanceof LivingEntity looked && looked != mc.player && !looked.isDeadOrDying()) {
            if (target != looked) shownHealth = -1f;
            target = looked;
            targetUntil = now + 60;
        }

        // Kill detection: an entity that we hit and that is now dead or dying.
        Iterator<Watched> it = WATCHED.iterator();
        while (it.hasNext()) {
            Watched w = it.next();
            LivingEntity e = w.entity;
            if (e.isDeadOrDying()) {
                it.remove();
                if (cfg.killEffects && (!cfg.killOnlyPlayers || e instanceof Player)) {
                    KillEffects.start(level, e);
                }
            } else if (e.isRemoved() || now > w.until) {
                it.remove();
            }
        }

        if (target != null && (target.isRemoved() || target.isDeadOrDying() || now > targetUntil
            || mc.player == null || target.distanceToSqr(mc.player) > MAX_DIST_SQ)) {
            target = null;
            shownHealth = -1f;
        }
        if (target != null && cfg.targetEsp && !target.isInvisible()) {
            marker(level, target, cfg, now);
        }
    }

    /** Pulsing ring on the ground and three glowing orbs circling the target. */
    private static void marker(ClientLevel level, LivingEntity e, VisualsConfig cfg, long t) {
        float size = cfg.particleSize;
        double w = Math.max(0.5, e.getBbWidth());
        double h = e.getBbHeight();

        if (t % 4 == 0) {
            GlowParticle.spawn(level, Shape.RING, e.getX(), e.getY() + 0.05, e.getZ(), 0, 0, 0,
                Palette.rgb(t, 0f), (float) (w * 1.15) * (0.7f + 0.3f * size), 8, 0.9f);
        }
        int orbs = cfg.reducedParticles ? 2 : 3;
        for (int i = 0; i < orbs; i++) {
            double a = t * 0.22 + i * (Math.PI * 2.0 / orbs);
            double y = e.getY() + h * (0.5 + 0.45 * Math.sin(t * 0.12 + i * 2.1));
            GlowParticle.spawn(level, Shape.GLOW,
                e.getX() + Math.cos(a) * w * 0.95, y, e.getZ() + Math.sin(a) * w * 0.95,
                0, 0, 0, Palette.rgb(t, i * 0.08f), 0.1f * size, 4, 0.95f);
        }
    }

    /** Small card with name and health bar, placed next to the crosshair. */
    public static void renderHud(GuiGraphics gui, Minecraft mc, VisualsConfig cfg) {
        LivingEntity e = target;
        if (e == null || !cfg.targetHud || mc.level == null) return;

        float max = Math.max(1f, e.getMaxHealth());
        float hp = Math.max(0f, e.getHealth());
        shownHealth = shownHealth < 0f ? hp : shownHealth + (hp - shownHealth) * 0.15f;
        float ratio = Math.max(0f, Math.min(1f, shownHealth / max));

        int w = 130;
        int h = 34;
        int x = gui.guiWidth() / 2 + 24;
        int y = gui.guiHeight() / 2 + 24;
        int accent = 0xFF000000 | Palette.rgb(mc.level.getGameTime(), 0f);
        int bar = ratio > 0.6f ? 0xFF55E08A : ratio > 0.3f ? 0xFFF2C94C : 0xFFEB5757;

        String name = e.getName().getString();
        if (mc.font.width(name) > 70) {
            name = mc.font.plainSubstrByWidth(name, 66) + "..";
        }
        String hpText = String.format("%.1f / %.0f", hp, max);

        gui.fill(x, y, x + w, y + h, 0xB20B0E18);
        gui.fill(x, y, x + 2, y + h, accent);
        gui.drawString(mc.font, name, x + 8, y + 6, 0xFFE8ECF6, false);
        gui.drawString(mc.font, hpText, x + w - 8 - mc.font.width(hpText), y + 6, 0xFFB4B8C6, false);
        gui.fill(x + 8, y + 21, x + w - 8, y + 26, 0xFF23262F);
        gui.fill(x + 8, y + 21, x + 8 + (int) ((w - 16) * ratio), y + 26, bar);
    }
}

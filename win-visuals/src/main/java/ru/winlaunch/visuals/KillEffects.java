package ru.winlaunch.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ru.winlaunch.visuals.GlowParticle.Shape;
import ru.winlaunch.visuals.VisualsConfig.KillMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Animated kill effects. Every effect is a short "scene" that is stepped once per client tick and
 * spawns {@link GlowParticle}s (plus a few vanilla particles) around the place where the target died.
 * Purely cosmetic and client-side.
 */
public final class KillEffects {
    private KillEffects() {}

    private static final int ORANGE = 0xFF8A1E;
    private static final int YELLOW = 0xFFD54A;
    private static final int EMBER = 0xFF4A12;
    private static final int WHITE = 0xFFFFFF;
    private static final int ICE = 0x8FE3FF;
    private static final int FROST = 0xDDF6FF;
    private static final int VIOLET = 0xA05CFF;
    private static final int VOID = 0x1B0833;
    private static final int GOLD = 0xFFD36B;
    private static final int STEEL = 0xDDE6F2;
    private static final int GREY = 0xB8BCC8;

    private static final int MAX_SCENES = 12;

    private static final class Scene {
        final ClientLevel level;
        final KillMode mode;
        final double x, y, z, h;
        /** Direction from the victim towards the local player, in radians. */
        final double facing;
        int age;

        Scene(ClientLevel level, KillMode mode, double x, double y, double z, double h) {
            this.level = level;
            this.mode = mode;
            this.x = x;
            this.y = y;
            this.z = z;
            this.h = Math.max(0.6, h);
            Player p = Minecraft.getInstance().player;
            this.facing = p == null ? level.random.nextDouble() * Math.PI * 2.0
                : Math.atan2(p.getZ() - z, p.getX() - x);
        }

        double cy() {
            return y + h * 0.5;
        }
    }

    private static final List<Scene> SCENES = new ArrayList<>();

    public static void clear() {
        SCENES.clear();
    }

    public static void start(ClientLevel level, Entity e) {
        start(level, e.getX(), e.getY(), e.getZ(), e.getBbHeight(), VisualsConfig.get().killMode);
    }

    public static void start(ClientLevel level, double x, double y, double z, double height, KillMode mode) {
        if (SCENES.size() >= MAX_SCENES) SCENES.remove(0);
        SCENES.add(new Scene(level, mode, x, y, z, height));
    }

    public static void tick(ClientLevel level) {
        Iterator<Scene> it = SCENES.iterator();
        while (it.hasNext()) {
            Scene s = it.next();
            if (s.level != level) {
                it.remove();
                continue;
            }
            step(s);
            s.age++;
            if (s.age > lifetime(s.mode)) it.remove();
        }
    }

    private static int lifetime(KillMode mode) {
        switch (mode) {
            case SHAHED: return 24;
            case FREEZE: return 30;
            case SINGULARITY: return 34;
            case DUEL: return 22;
            case EXPLOSION:
            default: return 18;
        }
    }

    private static void step(Scene s) {
        switch (s.mode) {
            case SHAHED -> shahed(s);
            case FREEZE -> freeze(s);
            case SINGULARITY -> singularity(s);
            case DUEL -> duel(s);
            default -> explosion(s);
        }
    }

    // ------------------------------------------------------------------ modes

    /** A loitering drone dives from above the player onto the victim and blows up. */
    private static void shahed(Scene s) {
        int a = s.age;
        double cx = s.x, cy = s.cy(), cz = s.z;
        if (a == 0) sound(s, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.9f, 0.6f);
        if (a < 16) {
            double dx = Math.cos(s.facing), dz = Math.sin(s.facing);
            double wx = -dz, wz = dx;
            for (int j = 0; j < 4; j++) {
                double k = (a + j / 4.0) / 16.0;
                double u = 1.0 - k;
                double px = cx + dx * 14.0 * u;
                double pz = cz + dz * 14.0 * u;
                double py = cy + 14.0 * Math.pow(u, 1.3);
                glow(s, Shape.GLOW, px, py, pz, 0, 0, 0, WHITE, 0f, 0.18f, 2, 1f);
                glow(s, Shape.GLOW, px + dx * 0.35, py + 0.08, pz + dz * 0.35, 0, 0, 0, ORANGE, 0.05f, 0.15f, 9, 0.85f);
                // Delta wings: two small grey points behind the nose.
                glow(s, Shape.GLOW, px + dx * 0.25 + wx * 0.4, py, pz + dz * 0.25 + wz * 0.4, 0, 0, 0, GREY, 0.1f, 0.07f, 2, 1f);
                glow(s, Shape.GLOW, px + dx * 0.25 - wx * 0.4, py, pz + dz * 0.25 - wz * 0.4, 0, 0, 0, GREY, 0.1f, 0.07f, 2, 1f);
                if (j == 0) s.level.addParticle(ParticleTypes.SMOKE, px + dx * 0.4, py, pz + dz * 0.4, 0, 0, 0);
            }
        } else if (a == 16) {
            explode(s, 1.5f, true);
        } else if (a <= 22) {
            embers(s, 3);
        }
    }

    /** The victim is wrapped in a growing ice tower that then shatters. */
    private static void freeze(Scene s) {
        int a = s.age;
        RandomSource r = s.level.random;
        if (a == 0) {
            glow(s, Shape.GLOW, s.x, s.cy(), s.z, 0, 0, 0, ICE, 0f, 1.1f, 6, 0.8f);
            ring(s, 0, 1.8f, 14, ICE);
            sound(s, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.7f);
        }
        if (a <= 10) {
            ring(s, a * 0.18, 0.7f + 0.1f * (a % 3), 8, a % 2 == 0 ? ICE : FROST);
            for (int i = 0; i < n(3); i++) {
                double ang = r.nextDouble() * Math.PI * 2.0;
                glow(s, Shape.STAR, s.x + Math.cos(ang) * 0.5, s.y + r.nextDouble() * s.h, s.z + Math.sin(ang) * 0.5,
                    0, 0.02, 0, FROST, 0.1f, 0.08f, 14, 1f);
            }
            s.level.addParticle(ParticleTypes.SNOWFLAKE, s.x + (r.nextDouble() - 0.5), s.y + r.nextDouble() * s.h,
                s.z + (r.nextDouble() - 0.5), 0, 0.02, 0);
        }
        if (a == 12) {
            glow(s, Shape.GLOW, s.x, s.cy(), s.z, 0, 0, 0, WHITE, 0f, 1.3f, 5, 0.9f);
            burst(s, s.x, s.cy(), s.z, n(50), 0.3, ICE, 0.1f, 20);
            burst(s, s.x, s.cy(), s.z, n(20), 0.16, WHITE, 0.07f, 16);
            ring(s, 0, 2.6f, 12, ICE);
            for (int i = 0; i < n(14); i++) {
                s.level.addParticle(ParticleTypes.SNOWFLAKE, s.x, s.cy(), s.z,
                    (r.nextDouble() - 0.5) * 0.3, r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.3);
            }
            sound(s, SoundEvents.GLASS_BREAK, 1f, 0.9f);
        }
    }

    /** A dark core pulls particles in along spirals and collapses into a shockwave. */
    private static void singularity(Scene s) {
        int a = s.age;
        RandomSource r = s.level.random;
        double cx = s.x, cy = s.cy(), cz = s.z;
        if (a <= 22) {
            glow(s, Shape.GLOW, cx, cy, cz, 0, 0, 0, VIOLET, 0f, 0.55f, 3, 0.35f);
            glow(s, Shape.GLOW, cx, cy, cz, 0, 0, 0, VOID, 0f, 0.25f + a * 0.012f, 3, 1f);
            for (int arm = 0; arm < 3; arm++) {
                double ang = a * 0.45 + arm * 2.094;
                double rad = 2.4 * (1.0 - a / 24.0) + 0.15;
                glow(s, Shape.STAR, cx + Math.cos(ang) * rad, cy + Math.sin(a * 0.3 + arm) * 0.3, cz + Math.sin(ang) * rad,
                    0, 0, 0, VIOLET, 0.07f * arm, 0.09f, 6, 1f);
            }
            for (int i = 0; i < n(3); i++) {
                double th = r.nextDouble() * Math.PI * 2.0;
                double cp = r.nextDouble() * 2.0 - 1.0;
                double sp = Math.sqrt(1.0 - cp * cp);
                double dx = Math.cos(th) * sp, dy = cp, dz = Math.sin(th) * sp;
                glow(s, Shape.SPARKLE, cx + dx * 2.6, cy + dy * 2.6, cz + dz * 2.6,
                    -dx * 0.26, -dy * 0.26, -dz * 0.26, VIOLET, 0.12f, 0.08f, 8, 1f);
            }
        }
        if (a == 23) {
            glow(s, Shape.GLOW, cx, cy, cz, 0, 0, 0, WHITE, 0f, 1.5f, 5, 0.95f);
            ring(s, 0, 3.0f, 12, VIOLET);
            ring(s, 0, 4.5f, 16, VIOLET);
            burst(s, cx, cy, cz, n(40), 0.45, VIOLET, 0.1f, 18);
            sound(s, SoundEvents.ENDERMAN_TELEPORT, 1f, 0.5f);
        }
    }

    /** Two sword slashes cross over the victim; the clash ends in a golden burst. */
    private static void duel(Scene s) {
        int a = s.age;
        double cx = s.x, cy = s.cy(), cz = s.z;
        if (a == 0) {
            ring(s, 0, 1.8f, 16, GOLD);
            ring(s, 0, 1.2f, 12, STEEL);
            sound(s, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        }
        // Unit vector perpendicular to the view direction, so the X faces the player.
        double rx = -Math.sin(s.facing), rz = Math.cos(s.facing);
        if (a <= 5) slash(s, a, 1, cx, cy, cz, rx, rz);
        if (a >= 4 && a <= 9) slash(s, a - 4, -1, cx, cy, cz, rx, rz);
        if (a == 10) {
            glow(s, Shape.GLOW, cx, cy, cz, 0, 0, 0, WHITE, 0f, 1.1f, 5, 0.95f);
            burst(s, cx, cy, cz, n(30), 0.26, GOLD, 0.1f, 14);
            ring(s, 0, 2.4f, 12, GOLD);
            sound(s, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1f);
        }
    }

    private static void slash(Scene s, int i, int dir, double cx, double cy, double cz, double rx, double rz) {
        for (int j = 0; j < 3; j++) {
            double sp = -1.3 + i * 0.45 + j * 0.15;
            double px = cx + rx * sp;
            double py = cy + dir * sp * 0.9;
            double pz = cz + rz * sp;
            glow(s, Shape.GLOW, px, py, pz, 0, 0, 0, STEEL, 0f, 0.13f, 6, 1f);
            glow(s, Shape.SPARKLE, px, py, pz, rx * 0.01, 0.01, rz * 0.01, GOLD, 0.05f, 0.1f, 10, 1f);
        }
    }

    private static void explosion(Scene s) {
        if (s.age == 0) explode(s, 1f, false);
        else if (s.age <= 10) embers(s, 3);
    }

    // ---------------------------------------------------------------- helpers

    private static void explode(Scene s, float scale, boolean fire) {
        RandomSource r = s.level.random;
        double cx = s.x, cy = s.cy(), cz = s.z;
        glow(s, Shape.GLOW, cx, cy, cz, 0, 0, 0, WHITE, 0f, 1.4f * scale, 6, 0.9f);
        glow(s, Shape.GLOW, cx, cy, cz, 0, 0, 0, ORANGE, 0.05f, 0.9f * scale, 9, 0.8f);
        ring(s, 0, 1.6f * scale, 10, ORANGE);
        ring(s, 0, 2.4f * scale, 14, YELLOW);
        ring(s, 0, 3.2f * scale, 18, EMBER);
        burst(s, cx, cy, cz, n(36), 0.34 * scale, ORANGE, 0.11f, 18);
        burst(s, cx, cy, cz, n(16), 0.2 * scale, YELLOW, 0.08f, 16);
        s.level.addParticle(ParticleTypes.EXPLOSION, cx, cy, cz, 0, 0, 0);
        if (fire) {
            for (int i = 0; i < n(8); i++) {
                s.level.addParticle(ParticleTypes.FLAME, cx + (r.nextDouble() - 0.5), s.y + 0.1, cz + (r.nextDouble() - 0.5),
                    (r.nextDouble() - 0.5) * 0.2, 0.05 + r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.2);
            }
        }
        for (int i = 0; i < n(6); i++) {
            s.level.addParticle(ParticleTypes.LARGE_SMOKE, cx + (r.nextDouble() - 0.5), cy, cz + (r.nextDouble() - 0.5),
                0, 0.06 + r.nextDouble() * 0.05, 0);
        }
        sound(s, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1f, 0.7f);
    }

    private static void embers(Scene s, int count) {
        RandomSource r = s.level.random;
        for (int i = 0; i < n(count); i++) {
            glow(s, Shape.GLOW, s.x + (r.nextDouble() - 0.5), s.y + 0.2, s.z + (r.nextDouble() - 0.5),
                (r.nextDouble() - 0.5) * 0.04, 0.08 + r.nextDouble() * 0.06, (r.nextDouble() - 0.5) * 0.04,
                ORANGE, 0.1f, 0.08f, 14, 0.9f);
        }
    }

    /** Stars and sparkles flying out of a point in every direction. */
    private static void burst(Scene s, double cx, double cy, double cz, int count, double speed, int rgb, float sz, int life) {
        RandomSource r = s.level.random;
        for (int i = 0; i < count; i++) {
            double th = r.nextDouble() * Math.PI * 2.0;
            double cp = r.nextDouble() * 2.0 - 1.0;
            double sp = Math.sqrt(1.0 - cp * cp);
            double v = speed * (0.5 + 0.5 * r.nextDouble());
            glow(s, i % 2 == 0 ? Shape.STAR : Shape.SPARKLE, cx, cy, cz,
                Math.cos(th) * sp * v, cp * v, Math.sin(th) * sp * v, rgb, (float) i / Math.max(1, count), sz, life + r.nextInt(6), 1f);
        }
    }

    /** Flat ring on the ground; {@code dy} lifts it above the feet. */
    private static void ring(Scene s, double dy, float radius, int life, int rgb) {
        // A tiny height offset per radius keeps overlapping rings from z-fighting.
        GlowParticle.spawn(s.level, Shape.RING, s.x, s.y + 0.04 + dy + radius * 0.01, s.z, 0, 0, 0,
            tint(s, rgb, radius * 0.1f), radius * (0.7f + 0.3f * VisualsConfig.get().particleSize), life, 1f);
    }

    private static void glow(Scene s, Shape shape, double x, double y, double z, double vx, double vy, double vz,
                             int rgb, float offset, float size, int life, float alpha) {
        GlowParticle.spawn(s.level, shape, x, y, z, vx, vy, vz, tint(s, rgb, offset),
            size * VisualsConfig.get().particleSize, life, alpha);
    }

    private static int tint(Scene s, int natural, float offset) {
        return VisualsConfig.get().killThemeColor ? Palette.rgb(s.level.getGameTime(), offset) : natural;
    }

    private static int n(int base) {
        return VisualsConfig.get().reducedParticles ? Math.max(1, base / 2) : base;
    }

    private static void sound(Scene s, SoundEvent event, float volume, float pitch) {
        if (!VisualsConfig.get().killSound) return;
        s.level.playLocalSound(s.x, s.cy(), s.z, event, SoundSource.PLAYERS, volume, pitch, false);
    }
}

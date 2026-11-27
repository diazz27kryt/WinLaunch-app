package ru.winlaunch.visuals;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ru.winlaunch.visuals.GlowParticle.Shape;

/** Particle spawning for all effects. Everything here is client-side and cosmetic. */
public final class Effects {
    private Effects() {}

    /** Smooth glowing ribbon behind a moving player, with a few stars falling off it. */
    public static void trail(ClientLevel level, Player p, double dx, double dz) {
        VisualsConfig cfg = VisualsConfig.get();
        int density = cfg.reducedParticles ? cfg.trailDensity / 2 : cfg.trailDensity;
        if (density <= 0) return;
        RandomSource r = level.random;
        long t = level.getGameTime();
        float size = cfg.particleSize;

        // Fill the path covered during this tick, so the ribbon has no gaps.
        int steps = 2 + density * 2;
        for (int i = 0; i < steps; i++) {
            double k = (double) i / steps;
            double x = p.getX() - dx * k;
            double y = p.getY() + 0.35;
            double z = p.getZ() - dz * k;
            GlowParticle.spawn(level, Shape.GLOW, x, y, z, 0, 0, 0,
                Palette.rgb(t, (float) k * 0.05f), 0.11f * size, 9, 0.55f);
        }
        for (int i = 0; i < density; i++) {
            if (r.nextInt(4) != 0) continue;
            Shape shape = r.nextBoolean() ? Shape.STAR : Shape.SPARKLE;
            GlowParticle.spawn(level, shape,
                p.getX() + (r.nextDouble() - 0.5) * 0.7,
                p.getY() + 0.1 + r.nextDouble() * 1.0,
                p.getZ() + (r.nextDouble() - 0.5) * 0.7,
                (r.nextDouble() - 0.5) * 0.02, 0.005 + r.nextDouble() * 0.015, (r.nextDouble() - 0.5) * 0.02,
                Palette.rgb(t, i * 0.07f), (0.07f + r.nextFloat() * 0.06f) * size, 16 + r.nextInt(14), 1f);
        }
    }

    /** Glowing circle under the feet and a few sparkles when jumping. */
    public static void jump(ClientLevel level, Player p) {
        VisualsConfig cfg = VisualsConfig.get();
        int count = cfg.reducedParticles ? cfg.jumpParticles / 2 : cfg.jumpParticles;
        if (count <= 0) return;
        circle(level, p, 0.9f, 14);
        sparks(level, p, count, 0.09);
    }

    /** Two bigger circles and more sparkles on landing. */
    public static void landing(ClientLevel level, Player p) {
        VisualsConfig cfg = VisualsConfig.get();
        int count = cfg.reducedParticles ? cfg.landingParticles / 2 : cfg.landingParticles;
        if (count <= 0) return;
        circle(level, p, 1.3f, 16);
        circle(level, p, 0.8f, 11);
        sparks(level, p, count, 0.14);
    }

    private static void circle(ClientLevel level, Player p, float radius, int life) {
        long t = level.getGameTime();
        // Slightly different heights keep overlapping circles from flickering.
        GlowParticle.spawn(level, Shape.RING, p.getX(), p.getY() + 0.04 + radius * 0.01, p.getZ(), 0, 0, 0,
            Palette.rgb(t, radius * 0.1f), radius * (0.7f + 0.3f * VisualsConfig.get().particleSize), life, 1f);
    }

    private static void sparks(ClientLevel level, Player p, int count, double speed) {
        RandomSource r = level.random;
        long t = level.getGameTime();
        float size = VisualsConfig.get().particleSize;
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2.0 * i) / count;
            Shape shape = i % 3 == 0 ? Shape.STAR : Shape.SPARKLE;
            GlowParticle.spawn(level, shape,
                p.getX() + Math.cos(a) * 0.3, p.getY() + 0.1, p.getZ() + Math.sin(a) * 0.3,
                Math.cos(a) * speed, 0.02 + r.nextDouble() * 0.04, Math.sin(a) * speed,
                Palette.rgb(t, (float) i / count), (0.05f + r.nextFloat() * 0.05f) * size, 12 + r.nextInt(10), 1f);
        }
    }

    /** Burst of glowing stars around a hit entity. */
    public static void hit(ClientLevel level, Entity target) {
        VisualsConfig cfg = VisualsConfig.get();
        int count = cfg.reducedParticles ? cfg.hitParticles / 2 : cfg.hitParticles;
        if (count <= 0) return;
        RandomSource r = level.random;
        long t = level.getGameTime();
        float size = cfg.particleSize;
        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.5;
        double cz = target.getZ();
        GlowParticle.spawn(level, Shape.GLOW, cx, cy, cz, 0, 0, 0, Palette.rgb(t, 0f), 0.5f * size, 5, 0.6f);
        for (int i = 0; i < count; i++) {
            Shape shape = i % 2 == 0 ? Shape.STAR : Shape.SPARKLE;
            GlowParticle.spawn(level, shape, cx, cy, cz,
                (r.nextDouble() - 0.5) * 0.36, (r.nextDouble() - 0.35) * 0.28, (r.nextDouble() - 0.5) * 0.36,
                Palette.rgb(t, (float) i / count), (0.07f + r.nextFloat() * 0.08f) * size, 14 + r.nextInt(14), 1f);
        }
    }
}

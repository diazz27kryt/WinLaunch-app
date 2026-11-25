package ru.winlaunch.visuals;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Particle spawning for all effects. Everything here is client-side and cosmetic. */
public final class Effects {
    private Effects() {}

    /** Coloured trail behind a moving player. */
    public static void trail(ClientLevel level, Player p, double dx, double dz) {
        VisualsConfig cfg = VisualsConfig.get();
        RandomSource r = level.random;
        long t = level.getGameTime();
        for (int i = 0; i < cfg.trailDensity; i++) {
            double x = p.getX() - dx * 0.5 + (r.nextDouble() - 0.5) * 0.35;
            double y = p.getY() + 0.1 + r.nextDouble() * 0.25;
            double z = p.getZ() - dz * 0.5 + (r.nextDouble() - 0.5) * 0.35;
            level.addParticle(Palette.dust(t, i * 0.03f), x, y, z, 0, 0.01, 0);
        }
    }

    /** Expanding coloured ring at the feet when jumping. */
    public static void jump(ClientLevel level, Player p) {
        ring(level, p, VisualsConfig.get().jumpParticles, 0.35, 0.12, false);
    }

    /** Bigger ring plus dust puff on landing. */
    public static void landing(ClientLevel level, Player p) {
        ring(level, p, VisualsConfig.get().landingParticles, 0.4, 0.2, true);
    }

    private static void ring(ClientLevel level, Player p, int count, double radius, double speed, boolean puff) {
        if (count <= 0) return;
        long t = level.getGameTime();
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2.0 * i) / count;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            double x = p.getX() + cos * radius;
            double z = p.getZ() + sin * radius;
            double y = p.getY() + 0.05;
            // Dust particles damp their initial velocity, so the speed is scaled up.
            level.addParticle(Palette.dust(t, (float) i / count), x, y, z, cos * speed * 4, 0.05, sin * speed * 4);
            if (puff && i % 3 == 0) {
                level.addParticle(ParticleTypes.CLOUD, x, y, z, cos * speed * 0.5, 0.02, sin * speed * 0.5);
            }
        }
    }

    /** Burst of coloured particles plus crit sparks around a hit entity. */
    public static void hit(ClientLevel level, Entity target) {
        VisualsConfig cfg = VisualsConfig.get();
        if (cfg.hitParticles <= 0) return;
        RandomSource r = level.random;
        long t = level.getGameTime();
        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.5;
        double cz = target.getZ();
        for (int i = 0; i < cfg.hitParticles; i++) {
            double vx = (r.nextDouble() - 0.5) * 2.0;
            double vy = (r.nextDouble() - 0.2) * 1.5;
            double vz = (r.nextDouble() - 0.5) * 2.0;
            level.addParticle(Palette.dust(t, (float) i / cfg.hitParticles), cx, cy, cz, vx, vy, vz);
        }
        for (int i = 0; i < Math.max(4, cfg.hitParticles / 4); i++) {
            level.addParticle(ParticleTypes.CRIT, cx, cy, cz,
                (r.nextDouble() - 0.5) * 0.8, r.nextDouble() * 0.6, (r.nextDouble() - 0.5) * 0.8);
        }
    }
}

package ru.winlaunch.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import ru.winlaunch.visuals.VisualsConfig.PerfMode;

/**
 * Applies a light graphics profile to the game options (render distance, simulation distance, particles,
 * entity distance, shadows). It runs once per profile version, so players can still change the
 * options by hand afterwards; the menu button applies it again on demand.
 */
public final class GameTuner {
    /** Bump this number to apply the profile again on the next start. */
    public static final int PROFILE_VERSION = 1;

    private GameTuner() {}

    public static void tickOnce(Minecraft mc, VisualsConfig cfg) {
        if (!cfg.enabled || !cfg.tuneGameSettings || cfg.perfMode == PerfMode.OFF) return;
        if (cfg.perfProfileApplied == PROFILE_VERSION) return;
        apply(mc, cfg);
    }

    public static void apply(Minecraft mc, VisualsConfig cfg) {
        boolean max = cfg.perfMode == PerfMode.MAX;
        Options o = mc.options;
        o.renderDistance().set(max ? 5 : 8);
        // Vanilla does not accept a simulation distance below 5.
        o.simulationDistance().set(max ? 5 : 6);
        o.framerateLimit().set(120);
        o.particles().set(max ? ParticleStatus.MINIMAL : ParticleStatus.DECREASED);
        o.entityDistanceScaling().set(max ? 0.5 : 0.75);
        o.biomeBlendRadius().set(max ? 0 : 1);
        o.entityShadows().set(false);
        o.save();
        cfg.perfProfileApplied = PROFILE_VERSION;
        cfg.save();
        WinVisuals.LOGGER.info("Win Visuals applied the {} performance profile", cfg.perfMode);
    }
}

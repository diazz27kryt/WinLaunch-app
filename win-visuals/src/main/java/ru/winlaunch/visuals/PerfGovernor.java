package ru.winlaunch.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import ru.winlaunch.visuals.VisualsConfig.PerfMode;

/**
 * Adaptive quality control. It measures real frame rate once per second and, when the game cannot
 * hold the target, steps the mod's own cost down: first the heavy night-sky shader, then the number
 * of particles. When there is headroom again it steps back up slowly (and never straight after a
 * drop), so quality does not flicker.
 *
 * <pre>
 * level 0  full sky, all particles
 * level 1  sky without nebulae (the most expensive part of the shader), ~60% particles
 * level 2  sky with stars only, ~35% particles
 * level 3  vanilla sky (our shader is not drawn at all), ~15% particles
 * </pre>
 */
public final class PerfGovernor {
    public static final int MAX_LEVEL = 3;
    private static final float[] SCALE = {1f, 0.6f, 0.35f, 0.15f};
    private static final int[] CAP = {240, 150, 90, 45};

    private static int level = 0;
    private static float fps = 0f;

    private static long windowStart = 0;
    private static long lastFrame = 0;
    private static long warmupUntil = 0;
    private static long blockUpUntil = 0;
    private static int frames = 0;
    private static int spikes = 0;
    private static int slow = 0;
    private static int fast = 0;

    /** Our own particles currently alive. */
    private static int activeParticles = 0;

    private PerfGovernor() {}

    public static int level() {
        return level;
    }

    public static float fps() {
        return fps;
    }

    private static boolean active(VisualsConfig cfg) {
        return cfg.enabled && cfg.perfMode != PerfMode.OFF;
    }

    /** MAX FPS mode never goes above level 2 quality. */
    private static int floorLevel(VisualsConfig cfg) {
        return cfg.perfMode == PerfMode.MAX ? 2 : 0;
    }

    /** True when our sky shader must not be drawn at all (vanilla sky is used instead). */
    public static boolean skyDisabled() {
        VisualsConfig cfg = VisualsConfig.get();
        return active(cfg) && Math.max(level, floorLevel(cfg)) >= MAX_LEVEL;
    }

    public static boolean nebulaAllowed() {
        VisualsConfig cfg = VisualsConfig.get();
        return !active(cfg) || Math.max(level, floorLevel(cfg)) < 1;
    }

    public static boolean auroraAllowed() {
        VisualsConfig cfg = VisualsConfig.get();
        return !active(cfg) || Math.max(level, floorLevel(cfg)) < 2;
    }

    /** Asks whether one more decorative particle may be created right now. */
    public static boolean allowParticle(RandomSource random) {
        VisualsConfig cfg = VisualsConfig.get();
        if (!active(cfg)) return true;
        int l = Math.max(0, Math.min(MAX_LEVEL, Math.max(level, floorLevel(cfg))));
        if (activeParticles >= CAP[l]) return false;
        return l == 0 || random.nextFloat() < SCALE[l];
    }

    public static void particleSpawned() {
        activeParticles++;
    }

    public static void particleRemoved() {
        if (activeParticles > 0) activeParticles--;
    }

    /** Called when leaving a world: forget the measurements and give the next world time to load. */
    public static void reset() {
        windowStart = 0;
        frames = 0;
        spikes = 0;
        slow = 0;
        fast = 0;
        activeParticles = 0;
        level = 0;
    }

    /** Call once per rendered frame. */
    public static void frame() {
        VisualsConfig cfg = VisualsConfig.get();
        long now = System.nanoTime() / 1_000_000L;
        if (windowStart == 0) {
            windowStart = now;
            lastFrame = now;
            warmupUntil = now + 8000;
            frames = 0;
            spikes = 0;
            return;
        }
        long dt = now - lastFrame;
        lastFrame = now;
        // Gaps over a second are loading screens or a hidden HUD, not gameplay.
        if (dt > 1000) {
            windowStart = now;
            frames = 0;
            spikes = 0;
            return;
        }
        frames++;
        if (dt > 100) spikes++;

        long elapsed = now - windowStart;
        if (elapsed < 1000) return;
        fps = frames * 1000f / elapsed;
        boolean spiky = spikes >= 2;
        frames = 0;
        spikes = 0;
        windowStart = now;
        evaluate(cfg, now, spiky);
    }

    private static void evaluate(VisualsConfig cfg, long now, boolean spiky) {
        if (!active(cfg)) {
            level = 0;
            return;
        }
        int floor = floorLevel(cfg);
        if (level < floor) level = floor;
        if (now < warmupUntil || Minecraft.getInstance().isPaused()) return;

        float target = cfg.targetFps;
        boolean low = fps < target * 0.9f || spiky;
        boolean high = fps > target * 1.08f && !spiky;
        if (low) {
            fast = 0;
            slow++;
            if (slow >= 2 && level < MAX_LEVEL) {
                level++;
                slow = 0;
                blockUpUntil = now + 45_000L;
            }
        } else {
            slow = 0;
            if (high) {
                fast++;
                if (fast >= 8 && level > floor && now > blockUpUntil) {
                    level--;
                    fast = 0;
                }
            } else {
                fast = 0;
            }
        }
    }
}

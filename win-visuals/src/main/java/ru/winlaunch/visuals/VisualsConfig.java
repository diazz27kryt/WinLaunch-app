package ru.winlaunch.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Config stored in .minecraft/config/winvisuals.json. Edit it and restart, or toggle with the keybind. */
public class VisualsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("winvisuals.json");

    public enum ColorMode { RAINBOW, SOLID, PULSE }

    public boolean enabled = true;

    // Sky and menu options exposed by the WinVisuals interface.
    public boolean auroraSky = true;
    public boolean skyStars = true;
    public boolean skyNebula = true;
    public boolean skyFog = false;
    public float skyIntensity = 1.0f;

    public boolean showHud = true;
    public boolean uiAnimations = true;
    public boolean compactUi = false;
    public float uiScale = 1.0f;
    public boolean reducedParticles = false;
    public float backgroundDim = 0.55f;
    /** Also show effects for other players, not only for you. */
    public boolean showOthers = false;

    public ColorMode colorMode = ColorMode.RAINBOW;
    /** Used by SOLID and PULSE modes, hex RRGGBB. */
    public String color = "55AAFF";
    /** Rainbow / pulse speed. */
    public float colorSpeed = 1.0f;
    public float particleSize = 1.0f;

    public boolean trails = true;
    /** Particles per tick while moving. */
    public int trailDensity = 2;
    /** Only show the trail while sprinting. */
    public boolean trailOnlySprint = false;
    /** Also show the trail while flying with elytra. */
    public boolean trailElytra = true;

    public boolean hitEffects = true;
    public int hitParticles = 18;

    public boolean jumpEffects = true;
    public int jumpParticles = 14;

    public boolean landingEffects = true;
    public int landingParticles = 18;

    private static VisualsConfig instance;

    public static VisualsConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static VisualsConfig load() {
        try {
            if (Files.exists(FILE)) {
                VisualsConfig cfg = GSON.fromJson(Files.readString(FILE), VisualsConfig.class);
                if (cfg != null) {
                    cfg.sanitize();
                    cfg.save();
                    return cfg;
                }
            }
        } catch (Exception e) {
            WinVisuals.LOGGER.warn("Could not read winvisuals.json, using defaults", e);
        }
        VisualsConfig cfg = new VisualsConfig();
        cfg.save();
        return cfg;
    }

    private void sanitize() {
        if (colorMode == null) colorMode = ColorMode.RAINBOW;
        if (color == null || !color.matches("(?i)#?[0-9a-f]{6}")) color = "55AAFF";
        trailDensity = Math.max(0, Math.min(trailDensity, 10));
        hitParticles = Math.max(0, Math.min(hitParticles, 80));
        jumpParticles = Math.max(0, Math.min(jumpParticles, 60));
        landingParticles = Math.max(0, Math.min(landingParticles, 60));
        colorSpeed = Math.max(0.05f, Math.min(colorSpeed, 10f));
        particleSize = Math.max(0.3f, Math.min(particleSize, 3f));
        skyIntensity = Math.max(0.1f, Math.min(skyIntensity, 2f));
        uiScale = Math.max(0.75f, Math.min(uiScale, 1.5f));
        backgroundDim = Math.max(0.2f, Math.min(backgroundDim, 0.9f));
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            WinVisuals.LOGGER.warn("Could not save winvisuals.json", e);
        }
    }

    /** RGB of the configured solid colour. */
    public int solidRgb() {
        String hex = color.startsWith("#") ? color.substring(1) : color;
        return Integer.parseInt(hex, 16);
    }
}

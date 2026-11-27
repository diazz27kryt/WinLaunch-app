package ru.winlaunch.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * Win Visuals own particle: full-bright, tinted, with smooth fade, twinkle and spin.
 * It is added straight to the particle engine, so no particle type has to be registered
 * and nothing is sent to or required from the server.
 */
public final class GlowParticle extends SingleQuadParticle {
    public enum Shape {
        STAR("star"), SPARKLE("sparkle"), GLOW("glow"), RING("ring");

        private final Identifier id;

        Shape(String name) {
            id = Identifier.fromNamespaceAndPath("winvisuals", name);
        }
    }

    private static final int FULL_BRIGHT = 0xF000F0;
    /** Turns the camera-facing quad so that it lies on the ground, front side up. */
    private static final FacingCameraMode FLAT =
        (rotation, camera, partialTick) -> rotation.rotationX((float) (-Math.PI / 2));

    private final Shape shape;
    private final float baseSize;
    private final float baseAlpha;
    private final float spin;
    private final float phase;

    private GlowParticle(ClientLevel level, Shape shape, TextureAtlasSprite sprite, double x, double y, double z,
                         int rgb, float size, int life, float alpha) {
        super(level, x, y, z, sprite);
        this.shape = shape;
        this.baseSize = size;
        this.baseAlpha = alpha;
        this.lifetime = Math.max(2, life);
        this.hasPhysics = false;
        this.gravity = 0f;
        this.friction = 0.88f;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.quadSize = size;
        this.phase = level.random.nextFloat() * 6.2832f;
        boolean spins = shape == Shape.STAR || shape == Shape.SPARKLE;
        this.spin = spins ? (level.random.nextFloat() - 0.5f) * 0.25f : 0f;
        this.roll = spins ? phase : 0f;
        this.oRoll = this.roll;
        setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        setAlpha(alpha);
    }

    /** Spawns one particle. {@code size} is half of the quad width in blocks. */
    public static void spawn(ClientLevel level, Shape shape, double x, double y, double z,
                             double vx, double vy, double vz, int rgb, float size, int life, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        TextureAtlasSprite sprite = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(shape.id);
        GlowParticle particle = new GlowParticle(level, shape, sprite, x, y, z, rgb, size, life, alpha);
        particle.xd = vx;
        particle.yd = vy;
        particle.zd = vz;
        mc.particleEngine.add(particle);
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        roll += spin;
        float left = 1f - (float) age / lifetime;
        // Hold the brightness, then fade out smoothly.
        setAlpha(baseAlpha * Math.max(0f, Math.min(1f, left * 1.8f)));
    }

    @Override
    public float getQuadSize(float partialTick) {
        float k = Math.min(1f, (age + partialTick) / lifetime);
        switch (shape) {
            case RING: {
                float out = 1f - (1f - k) * (1f - k) * (1f - k); // fast start, soft stop
                return baseSize * (0.2f + 0.8f * out);
            }
            case GLOW:
                return baseSize * (1f - 0.75f * k);
            default: {
                float pop = Math.min(1f, (age + partialTick) / 3f);
                float twinkle = 0.85f + 0.15f * (float) Math.sin((age + partialTick) * 0.9f + phase);
                return baseSize * pop * twinkle * (1f - 0.5f * k);
            }
        }
    }

    @Override
    public FacingCameraMode getFacingCameraMode() {
        return shape == Shape.RING ? FLAT : super.getFacingCameraMode();
    }

    @Override
    protected int getLightColor(float partialTick) {
        return FULL_BRIGHT;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }
}

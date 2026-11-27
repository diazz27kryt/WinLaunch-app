package ru.winlaunch.visuals.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import ru.winlaunch.visuals.Palette;
import ru.winlaunch.visuals.VisualsConfig;

/**
 * Win Visuals cosmetics drawn on the player model: glowing halo, translucent cone hat and feathered wings.
 * Purely client-side. All coordinates are in model space: blocks, Y points down, -Z is the front.
 */
public final class CosmeticsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier WHITE = texture("white");
    private static final Identifier WING = texture("wing");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int SEGMENTS = 64;

    /** Top of the head, measured from the neck pivot. */
    private static final float HEAD_TOP = -0.5f;

    /** Where the wing root sits inside wing.png, and the size of the whole textured quad in blocks. */
    private static final float ROOT_U = 0.039f;
    private static final float ROOT_V = 0.547f;
    private static final float WING_SIZE = 1.25f;

    public CosmeticsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("winvisuals", "textures/cosmetic/" + name + ".png");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state,
                       float yRot, float xRot) {
        VisualsConfig cfg = VisualsConfig.get();
        if (!cfg.enabled || state.isInvisible) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self = mc.player != null && state.id == mc.player.getId();
        if (!self && !cfg.showOthers) return;

        long time = mc.level == null ? 0L : mc.level.getGameTime();
        int rgb = Palette.rgb(time, 0f);

        if (cfg.cosmeticWings && !state.isFallFlying) {
            wings(poseStack, collector, state);
        }
        if (cfg.cosmeticHat || cfg.cosmeticHalo) {
            poseStack.pushPose();
            getParentModel().head.translateAndRotate(poseStack);
            if (cfg.cosmeticHat) hat(poseStack, collector, rgb);
            if (cfg.cosmeticHalo) {
                float bob = (float) Math.sin(state.ageInTicks * 0.09f) * 0.025f;
                // The halo floats higher when the hat is worn too.
                halo(poseStack, collector, rgb, HEAD_TOP - (cfg.cosmeticHat ? 0.42f : 0.2f) + bob);
            }
            poseStack.popPose();
        }
    }

    // ---------------------------------------------------------------- halo and hat

    private static void halo(PoseStack poseStack, SubmitNodeCollector collector, int rgb, float y) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(WHITE), (pose, out) -> {
            torus(pose, out, y, 0.40f, 0.016f, argb(255, mix(rgb, 0.45f)));
            torus(pose, out, y, 0.40f, 0.036f, argb(80, rgb));
            torus(pose, out, y, 0.40f, 0.062f, argb(34, rgb));
        });
    }

    /** A wide see-through cone with a bright rim. */
    private static void hat(PoseStack poseStack, SubmitNodeCollector collector, int rgb) {
        float tipY = HEAD_TOP - 0.25f;
        float rimY = HEAD_TOP + 0.04f;
        float radius = 0.58f;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(WHITE), (pose, out) -> {
            int tip = argb(175, mix(rgb, 0.35f));
            int edge = argb(85, rgb);
            for (int i = 0; i < SEGMENTS; i++) {
                double a0 = Math.PI * 2 * i / SEGMENTS;
                double a1 = Math.PI * 2 * (i + 1) / SEGMENTS;
                float x0 = (float) Math.cos(a0) * radius, z0 = (float) Math.sin(a0) * radius;
                float x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;
                vertex(pose, out, 0, tipY, 0, 0.5f, 0.5f, tip);
                vertex(pose, out, x0, rimY, z0, 0.5f, 0.5f, edge);
                vertex(pose, out, x1, rimY, z1, 0.5f, 0.5f, edge);
                vertex(pose, out, 0, tipY, 0, 0.5f, 0.5f, tip);
            }
            torus(pose, out, rimY, radius, 0.011f, argb(255, mix(rgb, 0.4f)));
            torus(pose, out, rimY, radius, 0.032f, argb(60, rgb));
        });
    }

    /** A ring with a round cross-section, so it stays visible from every angle. */
    private static void torus(PoseStack.Pose pose, VertexConsumer out, float y, float radius, float tube, int color) {
        int sides = 8;
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = Math.PI * 2 * i / SEGMENTS;
            double a1 = Math.PI * 2 * (i + 1) / SEGMENTS;
            for (int j = 0; j < sides; j++) {
                double b0 = Math.PI * 2 * j / sides;
                double b1 = Math.PI * 2 * (j + 1) / sides;
                torusVertex(pose, out, y, radius, tube, a0, b0, color);
                torusVertex(pose, out, y, radius, tube, a1, b0, color);
                torusVertex(pose, out, y, radius, tube, a1, b1, color);
                torusVertex(pose, out, y, radius, tube, a0, b1, color);
            }
        }
    }

    private static void torusVertex(PoseStack.Pose pose, VertexConsumer out, float y, float radius, float tube,
                                    double a, double b, int color) {
        double r = radius + Math.cos(b) * tube;
        vertex(pose, out, (float) (Math.cos(a) * r), y + (float) (Math.sin(b) * tube), (float) (Math.sin(a) * r),
            0.5f, 0.5f, color);
    }

    // ---------------------------------------------------------------- wings

    private void wings(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state) {
        // Slow idle flapping that gets quicker and wider while walking.
        float walk = Math.min(1f, state.walkAnimationSpeed);
        float flap = (float) Math.sin(state.ageInTicks * 0.11f) * (7f + 5f * walk);
        float sweep = 28f + flap;

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0f, 0.24f, 0.155f);
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * 0.07f, 0f, 0f);
            // Both wings are swept backwards (towards +Z).
            poseStack.mulPose(Axis.YP.rotationDegrees(-side * sweep));
            final int s = side;
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(WING), (pose, out) -> {
                float inner = -s * ROOT_U * WING_SIZE;
                float outer = s * (1f - ROOT_U) * WING_SIZE;
                float top = -ROOT_V * WING_SIZE;
                float bottom = (1f - ROOT_V) * WING_SIZE;
                int color = argb(240, 0xFFFFFF);
                vertex(pose, out, inner, top, 0f, 0f, 0f, color);
                vertex(pose, out, inner, bottom, 0f, 0f, 1f, color);
                vertex(pose, out, outer, bottom, 0f, 1f, 1f, color);
                vertex(pose, out, outer, top, 0f, 1f, 0f, color);
                // Back face too: the mirrored wing would otherwise be culled and only one wing would show.
                vertex(pose, out, inner, top, 0f, 0f, 0f, color);
                vertex(pose, out, outer, top, 0f, 1f, 0f, color);
                vertex(pose, out, outer, bottom, 0f, 1f, 1f, color);
                vertex(pose, out, inner, bottom, 0f, 0f, 1f, color);
            });
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    // ---------------------------------------------------------------- helpers

    private static void vertex(PoseStack.Pose pose, VertexConsumer out, float x, float y, float z, float u, float v,
                               int color) {
        out.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT)
            .setNormal(pose, 0f, -1f, 0f);
    }

    private static int argb(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** Mixes the colour with white by {@code k}. */
    private static int mix(int rgb, float k) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r += (int) ((255 - r) * k);
        g += (int) ((255 - g) * k);
        b += (int) ((255 - b) * k);
        return (r << 16) | (g << 8) | b;
    }
}

package ru.winlaunch.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.winlaunch.visuals.AuroraSkyRenderer;
import ru.winlaunch.visuals.VisualsConfig;

/** Draws the Win Visuals night sky inside the 1.21.11 sky render pass. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    /** Runs before the vanilla sun, moon and stars so they stay visible on top of our sky. */
    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void winvisuals$drawSky(PoseStack poseStack, float sunAngle, float moonAngle,
                                    float starAngle, MoonPhase moonPhase, float rainBrightness,
                                    float starBrightness, CallbackInfo ci) {
        VisualsConfig cfg = VisualsConfig.get();
        if (!cfg.enabled) return;
        AuroraSkyRenderer.render(poseStack.last().pose(), starAngle);
    }

    /** Our shader draws its own stars, so skip the vanilla star field to avoid doubling them up. */
    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void winvisuals$skipVanillaStars(float brightness, PoseStack poseStack, CallbackInfo ci) {
        VisualsConfig cfg = VisualsConfig.get();
        if (cfg.enabled && cfg.skyStars) ci.cancel();
    }
}

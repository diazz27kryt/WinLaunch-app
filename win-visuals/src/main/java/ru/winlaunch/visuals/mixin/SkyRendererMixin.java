package ru.winlaunch.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.winlaunch.visuals.AuroraSkyRenderer;
import ru.winlaunch.visuals.VisualsConfig;

/** Draws the Win Visuals night sky inside the 1.21.11 sky render pass. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void winvisuals$drawAurora(PoseStack poseStack, float sunAngle, float moonAngle,
                                       float starAngle, MoonPhase moonPhase, float rainBrightness,
                                       float starBrightness, CallbackInfo ci) {
        VisualsConfig cfg = VisualsConfig.get();
        if (!cfg.enabled) return;
        AuroraSkyRenderer.render(poseStack.last().pose(), starAngle);
    }
}

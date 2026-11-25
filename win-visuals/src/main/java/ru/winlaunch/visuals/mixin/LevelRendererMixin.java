package ru.winlaunch.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.winlaunch.visuals.AuroraSkyRenderer;
import ru.winlaunch.visuals.VisualsConfig;

/** Replaces the vanilla sky pass with the procedural Win Visuals sky. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(
        method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void winvisuals$renderCustomSky(Matrix4f poseMatrix, Matrix4f projectionMatrix, float tickDelta,
                                             Camera camera, boolean thickFog, Runnable fogCallback,
                                             CallbackInfo ci) {
        if (!VisualsConfig.get().enabled) return;
        fogCallback.run();\n        AuroraSkyRenderer.render(poseMatrix, tickDelta);
        ci.cancel();
    }
}

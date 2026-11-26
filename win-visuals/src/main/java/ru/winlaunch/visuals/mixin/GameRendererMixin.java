package ru.winlaunch.visuals.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.winlaunch.visuals.Zoom;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** Zoom narrows the world field of view only (the hand keeps its own). */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void winvisuals$zoom(Camera camera, float partialTick, boolean useFovSetting,
                                 CallbackInfoReturnable<Float> cir) {
        if (!useFovSetting) return;
        float f = Zoom.factor();
        if (f > 1.001f) {
            cir.setReturnValue(cir.getReturnValueF() / f);
        }
    }
}

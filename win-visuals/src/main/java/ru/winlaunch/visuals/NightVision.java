package ru.winlaunch.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Night vision for caves and the night. It gives the local player the vanilla Night Vision effect, but only
 * on the client: the server never hears about it, and the effect has no icon or particles. The game then
 * lights the world exactly like it does with a potion.
 */
public final class NightVision {
    private NightVision() {}

    public static void tick(Minecraft mc, VisualsConfig cfg) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        boolean ours = current != null && current.isInfiniteDuration() && !current.isVisible();

        if (cfg.enabled && cfg.nightVision) {
            if (!ours) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                    MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
            }
        } else if (ours) {
            // Only our own hidden effect is removed; a real potion is left alone.
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }
}

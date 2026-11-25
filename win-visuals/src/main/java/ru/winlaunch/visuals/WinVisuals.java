package ru.winlaunch.visuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import ru.winlaunch.visuals.gui.WinVisualsScreen;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WinVisuals implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("winvisuals");

    private static KeyMapping openKey;

    public static KeyMapping openKey() {
        return openKey;
    }

    /** Previous-tick state per player, used to detect jumps and landings. */
    private static final class State {
        boolean wasOnGround = true;
        double prevDy = 0;
        double prevX, prevZ;
        boolean hasPos = false;
    }

    private final Map<UUID, State> states = new HashMap<>();

    @Override
    public void onInitializeClient() {
        VisualsConfig.get();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.winvisuals.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("winvisuals", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        HudRenderCallback.EVENT.register((gui, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            VisualsConfig cfg = VisualsConfig.get();
            if (!cfg.enabled || !cfg.showHud || client.player == null || client.screen != null) return;
            int accent = 0xFF000000 | Palette.rgb(client.level == null ? 0L : client.level.getGameTime(), 0f);
            gui.fill(8, 8, 112, 26, 0xB20B0E18);
            gui.fill(8, 8, 10, 26, accent);
            gui.drawString(client.font, "WIN VISUALS", 16, 14, 0xFFE8ECF6, false);
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            VisualsConfig cfg = VisualsConfig.get();
            Minecraft mc = Minecraft.getInstance();
            if (cfg.enabled && cfg.hitEffects && world.isClientSide() && player == mc.player
                && world instanceof ClientLevel level) {
                Effects.hit(level, entity);
            }
            return InteractionResult.PASS;
        });
    }

    private void onTick(Minecraft mc) {
        while (openKey.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new WinVisualsScreen());
            }
        }

        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            states.clear();
            return;
        }
        VisualsConfig cfg = VisualsConfig.get();
        if (!cfg.enabled || mc.isPaused()) return;

        for (Player p : level.players()) {
            if (p != mc.player && !cfg.showOthers) continue;
            tickPlayer(level, p, cfg);
        }
        // Drop state of players that left the world.
        states.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
    }

    private void tickPlayer(ClientLevel level, Player p, VisualsConfig cfg) {
        State s = states.computeIfAbsent(p.getUUID(), k -> new State());

        double dx = s.hasPos ? p.getX() - s.prevX : 0;
        double dz = s.hasPos ? p.getZ() - s.prevZ : 0;
        s.prevX = p.getX();
        s.prevZ = p.getZ();
        s.hasPos = true;

        boolean onGround = p.onGround();
        double dy = p.getDeltaMovement().y;

        if (cfg.jumpEffects && s.wasOnGround && !onGround && dy > 0.1 && !p.isInWater() && !p.isFallFlying()) {
            Effects.jump(level, p);
        }
        if (cfg.landingEffects && !s.wasOnGround && onGround && s.prevDy < -0.55) {
            Effects.landing(level, p);
        }

        if (cfg.trails && !p.isSpectator() && !p.isInvisible()) {
            boolean moving = dx * dx + dz * dz > 0.0004;
            boolean elytra = cfg.trailElytra && p.isFallFlying();
            boolean sprintOk = !cfg.trailOnlySprint || p.isSprinting() || elytra;
            if ((moving && sprintOk) || elytra) {
                Effects.trail(level, p, dx, dz);
            }
        }

        s.wasOnGround = onGround;
        s.prevDy = dy;
    }
}

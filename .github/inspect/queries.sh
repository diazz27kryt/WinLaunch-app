#!/usr/bin/env bash
# usage: queries.sh MC_SRC FAPI_DIR
S=$1; F=$2
decl() { # print member declarations of a class
  f=$(find "$S" -path "*$1" | head -n 1)
  echo "=============== $1  ($f)"
  [ -z "$f" ] && { echo "NOT FOUND"; return; }
  grep -nE '^\s{4}[A-Za-z@<].*[({]\s*$|^\s{4}[A-Za-z].*\);$|^\s{4}(public|protected|static).*;$' "$f" | head -${2:-120}
}
full() { f=$(find "$S" -path "*$1" | head -n 1); echo "=============== FULL $1"; cat "$f" | head -${2:-400}; }

full net/minecraft/client/renderer/SkyRenderer.java 500
decl net/minecraft/resources/Identifier.java 40
decl net/minecraft/client/KeyMapping.java 60
decl net/minecraft/client/gui/GuiGraphics.java 150
decl net/minecraft/client/gui/screens/Screen.java 150
decl net/minecraft/client/input/MouseButtonEvent.java 30
decl net/minecraft/client/input/KeyEvent.java 30
decl net/minecraft/client/renderer/RenderType.java 80
decl net/minecraft/client/renderer/RenderPipelines.java 10
grep -n 'DEBUG_QUADS\|DEBUG_FILLED\|POSITION_COLOR\|STARS\|SKY\b\|LIGHTNING' $(find "$S" -path '*renderer/RenderPipelines.java') | head -30
decl net/minecraft/core/particles/DustParticleOptions.java 20
decl net/minecraft/client/multiplayer/ClientLevel.java 80
decl net/minecraft/world/entity/player/Player.java 5
grep -rn 'isFallFlying\|isSprinting' $(find "$S" -path '*world/entity/LivingEntity.java') | head -5
echo "=============== FABRIC API classes (rendering/keybinding/hud/events)"
find "$F" -name '*.jar' | head -80; for j in $(find "$F" -name '*.jar'); do unzip -l "$j" | awk '{print $4}'; done | grep -iE 'HudRenderCallback|HudElementRegistry|HudLayerRegistration|KeyBindingHelper|LevelRenderEvents|WorldRenderEvents|AttackEntityCallback|ClientTickEvents|FabricKeyMapping' | head -40

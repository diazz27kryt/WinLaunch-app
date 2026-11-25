# Win Visuals

Клиентский Fabric-мод для Minecraft 1.21.1 (только косметика, ничего не влияет на геймплей):

- **Трейлы**: цветной шлейф за игроком при движении (радуга / один цвет / пульс), в том числе при полёте на элитрах.
- **Эффекты ударов**: взрыв цветных частиц и крит-искр в точке попадания.
- **Эффекты прыжка**: расширяющееся кольцо под ногами.
- **Эффекты приземления**: кольцо и облачко пыли после падения с высоты.

## Сборка

Нужны JDK 21 и доступ к maven.fabricmc.net.

```
gradle wrapper --gradle-version 8.10
./gradlew build
```

Готовый jar: `build/libs/win-visuals-1.0.0.jar`. Положить в папку `mods` вместе с Fabric API.

## Настройка

Меню настроек открывается на **Right Shift** (клавиша меняется в настройках управления; повторное нажатие закрывает). Вкладки: Общее, Цвета, Трейлы, Удары, Прыжки. Всё сохраняется в `config/winvisuals.json`:
`colorMode` (RAINBOW / SOLID / PULSE), `color`, `colorSpeed`, `particleSize`, `trails`, `trailDensity`,
`trailOnlySprint`, `hitEffects`, `hitParticles`, `jumpEffects`, `landingEffects`, `showOthers` и т.д.

## Вшивание в лаунчер

После сборки положить jar в `ZalithLauncher/src/main/assets/winvisuals/win-visuals.jar`. Паки WinLaunch (Optimization и PvP) для Minecraft 1.21.1 будут включать его автоматически вместе с Fabric API.

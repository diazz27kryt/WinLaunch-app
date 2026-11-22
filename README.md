# WinLaunch

**WinLaunch** is an Android launcher for [Minecraft: Java Edition](https://www.minecraft.net/), an
**unofficial modified version of [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2)** by
MovTery and contributors (GPL-3.0). See [NOTICE.md](NOTICE.md) for what was changed and for the license terms.

- Android 8.0+ (API 26), Jetpack Compose UI, PojavLauncher-based game engine (inherited from Zalith Launcher 2).
- Plays Minecraft you own: sign in with a Microsoft account (or use the upstream's local/offline account where a
  server allows it). WinLaunch does not include Minecraft and does not grant a license for it.
- Fabric / Forge / NeoForge / Quilt support, so mods such as [GazJava](https://github.com/diazz27kryt/Gazjava)
  can be used on a phone.

## Download

Every push is built by GitHub Actions: open the **Actions** tab, pick the latest successful *Build APK* run and
download the artifact `WinLaunch-debug-apk-arm64` (most phones) or `WinLaunch-debug-apk-all` (all CPU types).
These are *debug* builds (signed with the public WinLaunch debug key).

## Build

Requirements: JDK 21, Android SDK (platform 37), NDK, Rust with `cargo-ndk`. See
[`.github/workflows/build.yml`](.github/workflows/build.yml) for the exact steps.

```
./gradlew ZalithLauncher:assembleDebug -Darch=arm64
```

## Release signing

Release builds are signed with `ZalithLauncher/winlaunch_release.jks` (not included in the repository). Provide the
keystore and the `STORE_PASSWORD` / `KEY_PASSWORD` environment variables to build `assembleRelease`.

## License

GPL-3.0, see [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md).

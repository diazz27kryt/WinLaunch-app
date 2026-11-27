package ru.winlaunch.visuals.gametest;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Enables the WinLaunch resource pack (path in $WL_PACK_ZIP) on the title screen and screenshots the panorama
 * and logo. Skipped when the variable is not set.
 */
public class WinLaunchPackClientTest implements FabricClientGameTest {
    private static final Path SHOTS = Path.of(System.getProperty("java.io.tmpdir"), "wvshots");

    private static void shot(ClientGameTestContext context, String name) {
        context.runOnClient(client -> Screenshot.takeScreenshot(client.getMainRenderTarget(), image -> {
            try (NativeImage img = image) {
                Files.createDirectories(SHOTS);
                img.writeToFile(SHOTS.resolve(name + ".png"));
                System.out.println("WVSHOT saved " + name);
            } catch (IOException e) {
                System.out.println("WVSHOT failed " + name + " " + e);
            }
        }));
        context.waitTicks(6);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        String zip = System.getenv("WL_PACK_ZIP");
        if (zip == null || zip.isBlank()) {
            System.out.println("WLPACK skipped (WL_PACK_ZIP not set)");
            return;
        }
        context.runOnClient(client -> {
            client.options.menuBackgroundBlurriness().set(0);
            client.options.framerateLimit().set(20);
            try {
                Path dir = client.getResourcePackDirectory();
                Files.createDirectories(dir);
                Files.copy(Path.of(zip), dir.resolve("WinLaunch.zip"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            PackRepository repo = client.getResourcePackRepository();
            repo.reload();
            boolean added = repo.addPack("file/WinLaunch.zip");
            Pack pack = repo.getPack("file/WinLaunch.zip");
            System.out.println("WLPACK added=" + added + " compatible=" + (pack != null && pack.getCompatibility().isCompatible())
                + " selected=" + repo.getSelectedIds());
            client.options.updateResourcePacks(repo);
        });
        context.waitTicks(20);
        context.waitFor(client -> client.getOverlay() == null);
        context.waitForScreen(TitleScreen.class);
        context.waitTicks(20);
        shot(context, "wl_title_a");
        context.waitTicks(120);
        shot(context, "wl_title_b");
    }
}

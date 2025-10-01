package net.covers1624.wstool.minecraft;

import com.google.common.hash.Hashing;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.net.HttpEngineDownloadAction;
import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.wstool.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by covers1624 on 6/2/25.
 */
public class AssetDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssetDownloader.class);

    public static AssetDownloadResult downloadAssets(Environment env, HttpEngine http, String mcVersion) {
        LOGGER.info("Updating assets for Minecraft {}", mcVersion);
        try {

            var versionsDir = env.systemFolder().resolve("versions");
            var assetsDir = env.systemFolder().resolve("assets");

            var versionList = VersionListManifest.update(http, versionsDir);
            var versionInList = versionList.versionsMap().get(mcVersion);
            if (versionInList == null) {
                throw new RuntimeException("Version doesn't exist? " + mcVersion);
            }

            var versionManifest = VersionManifest.update(http, versionsDir, versionInList);
            var assetIndex = AssetIndexManifest.update(http, assetsDir, versionManifest);

            try (var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)) {
                var futures = FastStream.of(assetIndex.objects().values())
                        .distinct()
                        .map(e -> updateAsset(executor, http, assetsDir, e))
                        .toList()
                        .toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(futures)
                        .join();
            }

            return new AssetDownloadResult(assetsDir, versionManifest.assetIndex());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to update assets.", ex);
        }
    }

    private static CompletableFuture<Void> updateAsset(ExecutorService executor, HttpEngine http, Path assetsDir, AssetIndexManifest.Asset asset) {
        return CompletableFuture.runAsync(() -> {
            Path dest = assetsDir.resolve("objects").resolve(asset.path());
            try {
                if (validate(dest, asset)) return;
            } catch (IOException ignored) {
            }

            if (copyFromExisting(dest, asset)) return;
            downloadAsset(http, dest, asset);
        }, executor);
    }

    private static boolean copyFromExisting(Path dest, AssetIndexManifest.Asset asset) {
        for (Launcher launcher : Launcher.LAUNCHERS) {
            for (Path dir : launcher.paths) {
                try {
                    var launcherFile = dir.resolve("objects").resolve(asset.path());
                    if (!validate(launcherFile, asset)) continue;

                    LOGGER.info("Copying asset from {} asset cache {}", launcher.name, asset.hash());
                    Files.deleteIfExists(dest);
                    Files.copy(launcherFile, IOUtils.makeParents(dest));
                    return true;
                } catch (IOException ex) {
                    LOGGER.error("Failed to check/use asset {} from {}", asset.path(), launcher.name, ex);
                }
            }
        }
        return false;
    }

    private static void downloadAsset(HttpEngine http, Path dest, AssetIndexManifest.Asset asset) {
        for (int i = 0; i < 3; i++) {
            try {
                if (!validate(dest, asset)) {
                    Files.deleteIfExists(dest);
                }

                LOGGER.info("Downloading asset {}", asset.hash());
                new HttpEngineDownloadAction(http)
                        .setUrl("https://resources.download.minecraft.net/" + asset.path())
                        .setDest(dest)
                        .setQuiet(false)
                        .execute();
                if (validate(dest, asset)) {
                    return;
                }
            } catch (IOException ex) {
                LOGGER.error("Error downloading asset. Try {}/{}", i + 1, 3, ex);
            }
        }
    }

    private static boolean validate(Path file, AssetIndexManifest.Asset asset) throws IOException {
        if (Files.notExists(file)) return false;

        if (Files.size(file) != asset.size()) return false;

        return sha1File(file).equals(asset.hash());
    }

    private static String sha1File(Path file) throws IOException {
        return HashUtils.hash(Hashing.sha1(), file).toString();
    }

    private static final Optional<Path> APPDATA = Optional.ofNullable(System.getenv("APPDATA")).map(Path::of);
    private static final Optional<Path> LOCALAPPDATA = Optional.ofNullable(System.getenv("LOCALAPPDATA")).map(Path::of);
    private static final Optional<Path> USER_HOME = Optional.ofNullable(System.getProperty("user.home")).map(Path::of);
    private static final Optional<Path> GRADLE_HOME = Optional.ofNullable(System.getProperty("user.home"))
            .map(Path::of)
            .map(e -> e.resolve(".gradle"));

    private enum Launcher {
        VANILLA(
                "Vanilla Launcher",
                APPDATA.map(e -> e.resolve(".minecraft")), // Windows
                USER_HOME.map(e -> e.resolve(".minecraft")), // Linux
                USER_HOME.map(e -> e.resolve("Library/Application Support/minecraft")) // macOS
        ),
        CURSE_FORGE(
                "CurseForge Launcher",
                USER_HOME.map(e -> e.resolve("curseforge/minecraft/Install")) // Windows
        ),
        MODRINTH(
                "Modrinth Launcher",
                APPDATA.map(e -> e.resolve("com.modrinth.theseus/meta/")) // Windows
        ),
        FTB_APP(
                "FTBApp",
                LOCALAPPDATA.map(e -> e.resolve(".ftba/bin")), // Windows
                USER_HOME.map(e -> e.resolve(".ftba/bin")), // Linux
                USER_HOME.map(e -> e.resolve("Library/Application Support/.ftba/bin")) // macOS
        ),
        MULTI_MC(
                "MultiMC",
                USER_HOME.map(e -> e.resolve("scoop/persist/multimc")), // Windows via Scoop
                USER_HOME.map(e -> e.resolve(".local/share/multimc")) // Linux
        ),
        PRISM(
                "Prism Launcher",
                APPDATA.map(e -> e.resolve("PrismLauncher")), // Windows
                USER_HOME.map(e -> e.resolve(".local/share/PrismLauncher")), // Linux
                USER_HOME.map(e -> e.resolve("Library/Application Support/PrismLauncher")) // macOS
        ),
        NFRT(
                "NeoFormRuntime/MDG",
                GRADLE_HOME.map(e -> e.resolve("caches/neoformruntime"))
        ),
        FABRIC_LOOM(
                "Fabric Loom",
                GRADLE_HOME.map(e -> e.resolve("caches/fabric-loom"))
        ),
        FORGE_GRADLE(
                "Forge Gradle",
                GRADLE_HOME.map(e -> e.resolve("caches/forge_gradle")), // FG 3+
                GRADLE_HOME.map(e -> e.resolve("caches/minecraft")) // FG 2
        ),
        ;

        public final String name;
        public final List<Path> paths;

        public static final List<Launcher> LAUNCHERS = List.of(Launcher.values());

        @SafeVarargs
        Launcher(String name, Optional<Path>... paths) {
            this.name = name;
            this.paths = FastStream.of(paths)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(e -> e.resolve("assets"))
                    .filter(Files::exists)
                    .toImmutableList();
        }
    }

    public record AssetDownloadResult(Path assetsDir, VersionManifest.AssetIndex assetIndex) { }
}

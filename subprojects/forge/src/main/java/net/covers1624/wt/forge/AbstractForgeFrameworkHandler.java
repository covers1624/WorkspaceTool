/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.net.download.DownloadAction;
import net.covers1624.quack.net.download.DownloadProgressTail;
import net.covers1624.tconsole.ConsumingOutputStream;
import net.covers1624.tconsole.api.TailConsole;
import net.covers1624.tconsole.api.TailGroup;
import net.covers1624.tconsole.tails.TextTail;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.framework.FrameworkHandler;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.forge.api.script.ForgeFramework;
import net.covers1624.wt.gradle.GradleProgressListener;
import net.covers1624.wt.mc.data.AssetIndexJson;
import net.covers1624.wt.mc.data.VersionInfoJson;
import net.covers1624.wt.mc.data.VersionManifestJson;
import net.covers1624.wt.util.GitHelper;
import net.covers1624.wt.util.HashContainer;
import net.covers1624.wt.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.text.MessageFormat.format;
import static net.covers1624.quack.util.SneakyUtils.sneaky;

/**
 * Created by covers1624 on 7/8/19.
 */
public abstract class AbstractForgeFrameworkHandler<T extends ForgeFramework> implements FrameworkHandler<T> {

    private static final String DEV_LOGIN_VERSION = "0.1.0.3";

    protected static final HashCode MARKER_HASH = HashCode.fromString("ff");
    protected static final Logger LOGGER = LogManager.getLogger("ForgeFrameworkHandler");
    protected static final String HASH_MERGED_AT = "merged-at";
    protected static final String HASH_MARKER_SETUP = "marker-setup";
    protected static final String HASH_GSTART_LOGIN = "gstart-login";
    protected static final HashFunction SHA_256 = Hashing.sha256();

    protected static final String LOCAL_BRANCH_SUFFIX = "-wt-local";

    protected final WorkspaceToolContext context;
    protected final HashContainer hashContainer;
    protected final GitHelper gitHelper;

    protected Path forgeDir;
    protected boolean needsSetup;
    protected boolean wasCloned;

    public AbstractForgeFrameworkHandler(WorkspaceToolContext context) {
        this.context = context;
        hashContainer = new HashContainer(context.cacheDir.resolve("forge_framework_cache.json"));
        gitHelper = new GitHelper(hashContainer);
        needsSetup = hashContainer.get(HASH_MARKER_SETUP) != null;
    }

    @Override
    public void constructFrameworkModules(T frameworkImpl) {
        forgeDir = context.projectDir.resolve(frameworkImpl.getPath());

        gitHelper.setRepoUrl(frameworkImpl.getUrl());
        gitHelper.setPath(forgeDir);
        gitHelper.setBranch(frameworkImpl.getBranch());
        gitHelper.setCommit(frameworkImpl.getCommit());
        gitHelper.setBranchSuffix(LOCAL_BRANCH_SUFFIX);

        if (sneaky(gitHelper::validate)) {
            needsSetup = true;
            wasCloned = true;
            hashContainer.set(HASH_MARKER_SETUP, MARKER_HASH);
        }
    }

    protected void runForgeSetup(Map<String, String> env, String... tasks) {
        String gradleVersion = context.gradleManager.getGradleVersionForProject(forgeDir);
        GradleConnector connector = GradleConnector.newConnector()
                .useGradleVersion(gradleVersion)
                .forProjectDirectory(forgeDir.toFile());
        Path javaHome;
        try {
            javaHome = context.getJavaInstall(context.gradleManager.getJavaVersionForGradle(gradleVersion)).javaHome;
        } catch (IOException e) {
            throw new RuntimeException("Unable to get Java install.", e);
        }
        LOGGER.info("Using JDK {}.", javaHome);
        try (ProjectConnection connection = connector.connect()) {
            TailGroup tailGroup = context.console.newGroupFirst();
            Map<String, String> realEnv = new HashMap<>(System.getenv());
            realEnv.putAll(env);
            connection.newBuild()
                    .setJavaHome(javaHome.toAbsolutePath().toFile())
                    .forTasks(tasks)
                    .withArguments("-si")
                    .setJvmArguments("-Xmx3G", "-Dorg.gradle.daemon=false", "-Didea.active=true", "-Didea.sync.active")
                    .setEnvironmentVariables(realEnv)
                    .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                    .setStandardError(new ConsumingOutputStream(LOGGER::error))
                    .addProgressListener(new GradleProgressListener(context, tailGroup))
                    .run();
            context.console.removeGroup(tailGroup);
        }
    }

    protected void downloadAssets(String mcVersion) {
        try {
            downloadAssets(context.cacheDir.resolve("minecraft"), mcVersion);
        } catch (Exception e) {
            throw new RuntimeException("An error occured whilst downloading Minecraft assets.", e);
        }
    }

    protected Dependency getDevLoginDependency() {
        try {
            Path devLoginFile = context.cacheDir.resolve("libs/DevLogin-" + DEV_LOGIN_VERSION + ".jar");
            DownloadAction action = new DownloadAction();
            action.setSrc("https://maven.covers1624.net/net/covers1624/DevLogin/" + DEV_LOGIN_VERSION + "/DevLogin-" + DEV_LOGIN_VERSION + ".jar");
            action.setDest(devLoginFile);
            action.setUseETag(true);
            action.setOnlyIfModified(true);
            action.execute();

            return new MavenDependencyImpl()
                    .setNotation(MavenNotation.parse("net.covers1624:DevLogin:" + DEV_LOGIN_VERSION))
                    .setClasses(devLoginFile)
                    .setExport(false);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to download DevLogin.", ex);
        }
    }

    private void downloadAssets(Path mcDir, String mcVersion) throws Exception {
        String RESOURCES_URL = "https://resources.download.minecraft.net/";
        //Mojang uses sha1 for their assets. This is safe to ignore.
        @SuppressWarnings ("deprecation")
        HashFunction sha1 = Hashing.sha1();

        Path assetsDir = mcDir.resolve("assets");
        context.blackboard.put(ForgeExtension.ASSETS_PATH, assetsDir);
        TailGroup dlGroup = context.console.newGroupFirst();
        DownloadProgressTail.Pool tailPool = new DownloadProgressTail.Pool(dlGroup);
        TextTail totalProgressTail = dlGroup.add(new TextTail(1));
        totalProgressTail.setLine(0, "Downloading assests..");

        Path vManifest = mcDir.resolve("version_manifest.json");
        {
            DownloadAction action = new DownloadAction();
            action.setSrc("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            action.setDest(vManifest);
            action.setUseETag(true);
            action.setOnlyIfModified(true);
            action.execute();
        }
        VersionManifestJson.Version mv = Utils.fromJson(vManifest, VersionManifestJson.class)
                .findVersion(mcVersion)
                .orElseThrow(() -> new RuntimeException("Failed to find minecraft version: " + mcVersion));

        Path versionFile = mcDir.resolve(mcVersion + ".json");
        {
            DownloadAction action = new DownloadAction();
            action.setSrc(mv.url);
            action.setDest(versionFile);
            action.setUseETag(true);
            action.setOnlyIfModified(true);
            action.execute();
        }
        VersionInfoJson versionInfo = Utils.fromJson(versionFile, VersionInfoJson.class);
        VersionInfoJson.AssetIndex assetIndex = versionInfo.assetIndex;

        context.blackboard.put(ForgeExtension.VERSION_INFO, versionInfo);

        Path assetIndexFile = mcDir.resolve("assets/indexes/" + assetIndex.id + ".json");
        {
            DownloadAction action = new DownloadAction();
            action.setSrc(assetIndex.url);
            action.setDest(assetIndexFile);
            action.setUseETag(true);
            action.setOnlyIfModified(true);
            action.execute();
        }
        AssetIndexJson indexJson = Utils.fromJson(assetIndexFile, AssetIndexJson.class);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        indexJson.objects.forEach((name, object) -> {

            String loc = object.hash.substring(0, 2) + "/" + object.hash;
            Path out;
            if (!indexJson.virtual) {
                out = assetsDir.resolve("objects").resolve(loc);
            } else {
                out = assetsDir.resolve("virtual").resolve(assetIndex.id).resolve(name);
            }

            if (Files.exists(out)) {
                Hasher hasher = sha1.newHasher();
                Utils.addToHasher(hasher, out);
                if (hasher.hash().toString().equals(object.hash)) {
                    return;//Continue from lambda.
                }
            }
            DownloadAction action = new DownloadAction();
            action.setSrc(RESOURCES_URL + loc);
            action.setDest(out);
            action.setQuiet(true);

            executor.submit(() -> {
                DownloadProgressTail tail = tailPool.pop();
                tail.setFileName(name);
                action.setListener(tail);
                sneaky(action::execute);
                if (!context.console.isSupported(TailConsole.Output.STDOUT)) {
                    LOGGER.info("Downloaded: '{}' to '{}'", action.getSrc(), action.getDest());
                }
                action.setListener(null);
                tailPool.push(tail);
            });
        });

        executor.shutdown();
        int max = (int) executor.getTaskCount();

        while (!executor.awaitTermination(200, TimeUnit.MILLISECONDS)) {
            int done = (int) executor.getCompletedTaskCount();
            totalProgressTail.setLine(0, format("Completed: {0}/{1}   {2}%", done, max, (int) ((double) done / max * 100)));
        }
        context.console.removeGroup(dlGroup);
    }
}

package net.covers1624.wt.forge;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import net.covers1624.tconsole.api.TailConsole;
import net.covers1624.tconsole.api.TailGroup;
import net.covers1624.tconsole.tails.TextTail;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.SourceSetDependency;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.module.ModuleImpl;
import net.covers1624.wt.api.impl.module.SourceSetImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.util.download.DownloadProgressTail;
import net.covers1624.wt.forge.api.script.Forge114;
import net.covers1624.wt.forge.util.AccessExtractor;
import net.covers1624.wt.forge.util.AtFile;
import net.covers1624.wt.mc.data.AssetIndexJson;
import net.covers1624.wt.mc.data.VersionInfoJson;
import net.covers1624.wt.mc.data.VersionManifestJson;
import net.covers1624.wt.util.MavenNotation;
import net.covers1624.wt.util.ProjectDataHelper;
import net.covers1624.wt.util.Utils;
import net.covers1624.wt.util.download.DownloadAction;
import net.rubygrapefruit.platform.terminal.Terminals;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.GradleVersion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;
import static java.util.Collections.emptySet;

/**
 * Created by covers1624 on 7/8/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class Forge114FrameworkHandler extends AbstractForgeFrameworkHandler<Forge114> {

    public Forge114FrameworkHandler(WorkspaceToolContext context) {
        super(context);
    }

    @Override
    public void constructFrameworkModules(Forge114 frameworkImpl) {
        super.constructFrameworkModules(frameworkImpl);

        Path cachedForgeAt = context.cacheDir.resolve("forge_accesstransformer.cfg");
        Path forgeAt = forgeDir.resolve("src/main/resources/META-INF/accesstransformer.cfg");
        Path mergedAt = context.cacheDir.resolve("merged_at.cfg");
        if (wasCloned || !Files.exists(cachedForgeAt)) {
            Utils.sneaky(() -> Files.copy(forgeAt, cachedForgeAt, StandardCopyOption.REPLACE_EXISTING));
        }
        {//AccessTransformers
            Hasher mergedHasher = sha256.newHasher();
            List<Path> atFiles = context.modules.parallelStream()//
                    .flatMap(e -> e.getSourceSets().values().stream())//
                    .flatMap(e -> e.getResources().stream())//
                    .filter(Files::exists)//
                    .flatMap(Utils.sneak(e -> Files.walk(e).filter(f -> f.getFileName().toString().equals("accesstransformer.cfg"))))//
                    .collect(Collectors.toList());
            atFiles.forEach(e -> logger.info("Found AccessTransformer: {}", e));
            atFiles.forEach(e -> Utils.addToHasher(mergedHasher, e));
            Utils.addToHasher(mergedHasher, cachedForgeAt);
            HashCode mergedHash = mergedHasher.hash();
            if (hashContainer.check(HASH_MERGED_AT, mergedHash) || Files.notExists(mergedAt)) {
                needsSetup = true;
                hashContainer.set(HASH_MARKER_SETUP, MARKER_HASH);
                AtFile atFile = new AtFile().useDot();
                atFiles.stream().map(AtFile::new).forEach(atFile::merge);
                atFile.merge(new AtFile(cachedForgeAt));
                atFile.write(mergedAt);
                hashContainer.set(HASH_MERGED_AT, mergedHash);
            }
            Utils.sneaky(() -> Files.copy(mergedAt, forgeAt, StandardCopyOption.REPLACE_EXISTING));
        }

        Path formsRt = context.cacheDir.resolve("libs/forms_rt.jar");
        Dependency formsRtDep = new MavenDependencyImpl()//
                .setNotation(MavenNotation.parse("org.jetbrains:forms_rt:1.0.0"))//
                .setClasses(formsRt).setExport(false);
        { //GStart Login.
            Path r1 = forgeDir.resolve("src/userdev/java/net/minecraftforge/userdev/WTLaunchLogin.java");
            Path r2 = forgeDir.resolve("src/userdev/java/net/minecraftforge/userdev/WTCredentialsDialog.java");

            Hasher gStartLoginResourcesHasher = sha256.newHasher();
            Utils.addToHasher(gStartLoginResourcesHasher, "/wt_login/forms_rt.jar");
            Utils.addToHasher(gStartLoginResourcesHasher, "/wt_login/114/src/net/minecraftforge/userdev/WTLaunchLogin.java");
            Utils.addToHasher(gStartLoginResourcesHasher, "/wt_login/114/src/net/minecraftforge/userdev/WTCredentialsDialog.java");
            HashCode hash1 = gStartLoginResourcesHasher.hash();

            Hasher gStartLoginFilesHasher = sha256.newHasher();
            Utils.addToHasher(gStartLoginFilesHasher, formsRt);
            Utils.addToHasher(gStartLoginFilesHasher, r1);
            Utils.addToHasher(gStartLoginFilesHasher, r2);
            HashCode hash2 = gStartLoginFilesHasher.hash();

            if (hashContainer.check(HASH_GSTART_LOGIN, hash1) || !hash2.equals(hash1)) {
                Utils.extractResource("/wt_login/forms_rt.jar", formsRt);
                Utils.extractResource("/wt_login/114/src/net/minecraftforge/userdev/WTLaunchLogin.java", r1);
                Utils.extractResource("/wt_login/114/src/net/minecraftforge/userdev/WTCredentialsDialog.java", r2);
                hashContainer.set(HASH_GSTART_LOGIN, hash1);
            }
        }

        WorkspaceToolModel model = context.modelCache.getModel(forgeDir, emptySet(), Collections.singleton("prepareRuns"));
        Module forgeModule = new ModuleImpl.GradleModule("Forge", forgeDir, model.getProjectData());
        forgeModule.addExclude(forgeDir.resolve("build"));
        forgeModule.addExclude(forgeDir.resolve(".gradle"));
        forgeModule.addExclude(forgeDir.resolve("projects/clean"));
        forgeModule.addExclude(forgeDir.resolve("projects/forge/build"));
        forgeModule.addExclude(forgeDir.resolve("projects/mcp/build"));
        ProjectData forgeSubModuleData = model.getProjectData().subProjects.stream()//
                .filter(e -> e.name.equals("forge"))//
                .findFirst()//
                .orElseThrow(() -> new RuntimeException("'forge' submodule not found on Forge project."));

        //I have no idea why Gradle doesnt do this already..
        //TODO, Investigate why gradle model builder stuff doesnt extract this properly.
        forgeSubModuleData.configurations.get("userdevCompile").extendsFrom.add("userdevImplementation");
        forgeSubModuleData.configurations.get("testCompile").extendsFrom.add("testImplementation");
        forgeSubModuleData.configurations.get("fmllauncherCompile").extendsFrom.add("fmllauncherImplementation");
        forgeSubModuleData.configurations.get("fmllauncherCompile").extendsFrom.add("compile");
        forgeSubModuleData.configurations.get("testCompile").extendsFrom.add("compile");
        forgeSubModuleData.configurations.get("compile").extendsFrom.add("implementation");
        forgeSubModuleData.configurations.get("installer").transitive = true;

        Map<String, Configuration> configurations = ProjectDataHelper.buildConfigurations(forgeModule, forgeSubModuleData);
        Map<String, SourceSet> sourceSets = ProjectDataHelper.buildSourceSets(forgeSubModuleData, configurations);
        //TODO, Test SourceSet is disabled, testImplementation is not being resolved.
        sourceSets.remove("test");
        forgeModule.setConfigurations(configurations);
        forgeModule.setSourceSets(sourceSets);

        SourceSet mainSS = sourceSets.get("main");

        //SourceSet testSS = sourceSets.get("test");
        SourceSet userdevSS = sourceSets.get("userdev");

        Configuration mainCompile = mainSS.getCompileConfiguration();
        //Configuration testCompile = testSS.getCompileConfiguration();
        Configuration userdevCompile = userdevSS.getCompileConfiguration();
        Configuration userdevRuntume = userdevSS.getRuntimeConfiguration();

        SourceSetDependency fmllauncherDep = new SourceSetDependencyImpl(forgeModule, "fmllauncher");
        SourceSetDependency mainSSDep = new SourceSetDependencyImpl(forgeModule, "main");

        mainCompile.addDependency(fmllauncherDep.copy());

        //testCompile.addDependency(mainSSDep);
        //testCompile.addDependency(fmllauncherDep);

        userdevCompile.addDependency(mainSSDep);
        userdevCompile.addDependency(fmllauncherDep);
        userdevCompile.addDependency(formsRtDep);

        context.frameworkModules.add(forgeModule);

        Dependency forgeDep = new SourceSetDependencyImpl(forgeModule, "userdev");
        context.modules.forEach(m -> {
            m.getSourceSets().values().forEach(ss -> {
                ss.getCompileConfiguration().addDependency(forgeDep);
            });
            userdevRuntume.addDependency(new SourceSetDependencyImpl()//
                    .setModule(m)//
                    .setSourceSet("main")//
                    .setExport(false)//
            );
        });

        if (needsSetup) {
            boolean useProjectGradle = false;
            try (ProjectConnection connection = GradleConnector.newConnector()
                    .forProjectDirectory(forgeDir.toFile())
                    .connect()) {
                BuildEnvironment environment = connection.getModel(BuildEnvironment.class);
                GradleVersion installed = GradleVersion.version(environment.getGradle().getGradleVersion());
                GradleVersion requiredVersion = GradleVersion.version(GRADLE_VERSION);

                logger.info("Detected Gradle version: {}", installed);
                if (installed.compareTo(requiredVersion) >= 0) {
                    logger.info("Using project gradle version.");
                    useProjectGradle = true;
                } else {
                    logger.info("Forcing gradle {}.", GRADLE_VERSION);
                }
            }

            GradleConnector connector = GradleConnector.newConnector();
            connector.forProjectDirectory(forgeDir.toFile());
            if (!useProjectGradle) {
                connector.useGradleVersion(GRADLE_VERSION);
            }
            try (ProjectConnection connection = connector.connect()) {
                connection.newBuild()//
                        .forTasks("clean", "setup", ":forge:compileJava")//
                        .withArguments("-si")//
                        .setStandardOutput(System.out)//
                        .setStandardError(System.err)//
                        .run();
            }
            Path accessList = context.cacheDir.resolve("forge_access_list.cfg.xz");
            AtFile atFile = AccessExtractor.extractAccess(Collections.singleton(forgeDir.resolve("projects/forge/build/classes/java/main")));
            atFile.write(accessList, AtFile.CompressionMethod.XZ);
            hashContainer.remove(HASH_MARKER_SETUP);//clear the marker.
        }
        try {
            downloadAssets(context.cacheDir.resolve("minecraft"), model.getProjectData().extraProperties.get("MC_VERSION"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SourceSet addSourceSet(Module module, String name, Consumer<SourceSet> func) {
        SourceSet sourceSet = new SourceSetImpl(name);
        module.addSourceSet(name, sourceSet);
        func.accept(sourceSet);
        return sourceSet;
    }

    private void downloadAssets(Path mcDir, String mcVersion) throws Exception {
        String RESOURCES_URL = "http://resources.download.minecraft.net/";
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
        VersionManifestJson.Version mv = Utils.fromJson(vManifest, VersionManifestJson.class)//
                .findVersion(mcVersion)//
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
                action.setProgressTail(tail);
                Utils.sneaky(action::execute);
                if (!context.console.isSupported(TailConsole.Output.STDOUT)) {
                    logger.info("Downloaded: '{}' to '{}'", action.getSrc(), action.getDest());
                }
                action.setProgressTail(null);
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

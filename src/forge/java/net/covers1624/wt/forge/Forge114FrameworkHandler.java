package net.covers1624.wt.forge;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.data.ProjectData;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.SourceSetDependency;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.module.ModuleImpl;
import net.covers1624.wt.api.impl.module.SourceSetImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.api.tail.DownloadProgressTail;
import net.covers1624.wt.api.tail.TailGroup;
import net.covers1624.wt.api.tail.TextTail;
import net.covers1624.wt.forge.api.script.Forge114;
import net.covers1624.wt.mc.data.AssetIndexJson;
import net.covers1624.wt.mc.data.VersionInfoJson;
import net.covers1624.wt.mc.data.VersionManifestJson;
import net.covers1624.wt.util.MavenNotation;
import net.covers1624.wt.util.ProjectDataHelper;
import net.covers1624.wt.util.Utils;
import net.covers1624.wt.util.download.DownloadAction;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
            try (ProjectConnection connection = GradleConnector.newConnector()//
                    .useGradleVersion(GRADLE_VERSION)//
                    .forProjectDirectory(forgeDir.toFile())//
                    .connect()) {
                connection.newBuild()//
                        .forTasks("clean", "setup")//
                        .withArguments("-si")//
                        .setStandardOutput(System.out)//
                        .setStandardError(System.err)//
                        .run();
            }
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
        TailGroup dlGroup = context.console.newGroup();
        DownloadProgressTail.Pool tailPool = new DownloadProgressTail.Pool(dlGroup);
        TextTail totalProgressTail = dlGroup.add(new TextTail());

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
                if (!context.console.isSupported()) {
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
            totalProgressTail.setText(format("Completed: {0}/{1}   {2}%", done, max, (int) ((double) done / max * 100)));
        }
        context.console.removeGroup(dlGroup);
    }
}

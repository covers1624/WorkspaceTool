/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.SourceSetDependency;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.module.ModuleImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.forge.api.script.Forge114;
import net.covers1624.wt.util.ProjectDataHelper;
import net.covers1624.wt.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 7/8/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class Forge114FrameworkHandler extends AbstractForge113PlusFrameworkHandler<Forge114> {

    public Forge114FrameworkHandler(WorkspaceToolContext context) {
        super(context);
    }

    @Override
    public void constructFrameworkModules(Forge114 frameworkImpl) {
        super.constructFrameworkModules(frameworkImpl);

        handleAts();

        Path formsRt = context.cacheDir.resolve("libs/forms_rt.jar");
        Dependency formsRtDep = new MavenDependencyImpl()//
                .setNotation(MavenNotation.parse("org.jetbrains:forms_rt:1.0.0"))//
                .setClasses(formsRt).setExport(false);
        { //GStart Login.
            Path r1 = forgeDir.resolve("src/userdev/java/net/minecraftforge/userdev/WTLaunchLogin.java");
            Path r2 = forgeDir.resolve("src/userdev/java/net/minecraftforge/userdev/WTCredentialsDialog.java");

            Hasher gStartLoginResourcesHasher = SHA_256.newHasher();
            Utils.addToHasher(gStartLoginResourcesHasher, "/wt_login/forms_rt.jar");
            Utils.addToHasher(gStartLoginResourcesHasher, "/wt_login/114/src/net/minecraftforge/userdev/WTLaunchLogin.java");
            Utils.addToHasher(gStartLoginResourcesHasher, "/wt_login/114/src/net/minecraftforge/userdev/WTCredentialsDialog.java");
            HashCode hash1 = gStartLoginResourcesHasher.hash();

            Hasher gStartLoginFilesHasher = SHA_256.newHasher();
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
        ProjectData forgeSubModuleData = requireNonNull(model.getProjectData().subProjects.get("forge"), "'forge' submodule not found on Forge project.");

        Map<String, Configuration> configurations = ProjectDataHelper.buildConfigurations(forgeModule, forgeSubModuleData, emptyMap());
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
                for (Path resourceDir : ss.getResources()) {
                    //If the SS has a mods.toml file, assume there is _some_ mod there
                    if (Files.exists(resourceDir.resolve("META-INF/mods.toml"))) {
                        userdevRuntume.addDependency(new SourceSetDependencyImpl()
                                .setModule(m)
                                .setSourceSet(ss.getName())
                                .setExport(false)
                        );
                        break;
                    }
                }
            });
        });

        if (needsSetup) {
            runForgeSetup(of(), "clean", "setup", ":forge:compileJava");
//            Path accessList = context.cacheDir.resolve("forge_access_list.cfg.xz");
//            AtFile atFile = AccessExtractor.extractAccess(Collections.singleton(forgeDir.resolve("projects/forge/build/classes/java/main")));
//            atFile.write(accessList, AtFile.CompressionMethod.XZ);
            hashContainer.remove(HASH_MARKER_SETUP);//clear the marker.
        }
        downloadAssets(model.getProjectData().extraProperties.get("MC_VERSION"));
    }
}

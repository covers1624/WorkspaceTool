/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.gradle.data.ConfigurationData;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.gradle.data.SourceSetData;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.module.ModuleImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.forge.api.script.Forge117;
import net.covers1624.wt.util.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.collection.ColUtils.iterable;

/**
 * Created by covers1624 on 25/10/21.
 */
@SuppressWarnings ("UnstableApiUsage")
public class Forge117FrameworkHandler extends AbstractForge113PlusFrameworkHandler<Forge117> {

    private static final Attributes.Name fmlModType = new Attributes.Name("FMLModType");

    public Forge117FrameworkHandler(WorkspaceToolContext context) {
        super(context);
    }

    @Override
    public void constructFrameworkModules(Forge117 frameworkImpl) {
        super.constructFrameworkModules(frameworkImpl);

        // Handle any Access Transformers.
        handleAts();

        extractLaunchResources();

        fixBullshitVersionRange();

        WorkspaceToolModel model = context.modelCache.getModel(forgeDir, emptySet(), Collections.singleton("prepareRuns"));
        ProjectData projectData = model.getProjectData();
        projectData.subProjects.remove("clean"); // Useless for WorkspaceTool.
        projectData.subProjects.remove("fmlonly"); // Also, useless for WorkspaceTool.
        for (ProjectData project : iterable(projectData.streamAllProjects())) {
            // All Test sourcesets seem to be missing a dependency on main.
            // TODO, rework dependency visitor system to not rely on SourceSet dependencies.
            //  We should be able to detect the dependencies more manually.
            SourceSetData ssData = project.sourceSets.get("test");
            if (ssData == null) continue;
            ConfigurationData configData = project.configurations.get(ssData.compileConfiguration);
            if (configData == null) continue;
            configData.dependencies.add(new ConfigurationData.SourceSetDependency("main"));
        }

        // Write out some manifests, these are required for FML to load these modules properly.
        writeManifest(forgeDir.resolve("javafmllanguage/src/main/resources/META-INF/MANIFEST.MF"), "LANGPROVIDER");
        writeManifest(forgeDir.resolve("mclanguage/src/main/resources/META-INF/MANIFEST.MF"), "LANGPROVIDER");
        writeManifest(forgeDir.resolve("fmlcore/src/main/resources/META-INF/MANIFEST.MF"), "LIBRARY");
        if (Files.exists(forgeDir.resolve("lowcodelanguage"))) {
            writeManifest(forgeDir.resolve("lowcodelanguage/src/main/resources/META-INF/MANIFEST.MF"), "LANGPROVIDER");
        }

        // Build our Forge modules.
        List<Module> modules = ModuleImpl.makeGradleModules("", model.getProjectData(), context);
        context.frameworkModules.addAll(modules);
        GradleBackedModule forgeSubModule = ForgeExtension.findForgeSubModule(context);
        Configuration forgeRuntimeConfig = requireNonNull(forgeSubModule.getConfigurations().get("runtimeOnly"), "'runtimeOnly' Configuration is missing.");

        Dependency forgeDep = new SourceSetDependencyImpl(forgeSubModule, "main");
        context.modules.forEach(m -> {
            m.getSourceSets().values().forEach(ss -> {
                ss.getCompileConfiguration().addDependency(forgeDep.copy());
                for (Path resourceDir : ss.getResources()) {
                    //If the SS has a mods.toml file, assume there is _some_ mod there
                    if (Files.exists(resourceDir.resolve("META-INF/mods.toml"))) {
                        forgeRuntimeConfig.addDependency(new SourceSetDependencyImpl()
                                .setModule(m)
                                .setSourceSet(ss.getName())
                                .setExport(false)
                        );
                        break;
                    }
                }
            });
        });

        // Run forge setup if required.
        if (needsSetup) {
            runForgeSetup(of(), "clean", "setup", ":forge:compileJava");
            hashContainer.remove(HASH_MARKER_SETUP);
        }
        downloadAssets(projectData.extraProperties.get("MC_VERSION"));
    }

    private void writeManifest(Path manifestPath, String modType) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0 ");
        manifest.getMainAttributes().put(fmlModType, modType);
        try (OutputStream os = Files.newOutputStream(Utils.makeFile(manifestPath))) {
            manifest.write(os);
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void extractLaunchResources() {
        Path offlineLaunch = forgeDir.resolve("fmlloader/src/main/java/net/covers1624/wt/OfflineLaunch.java");

        Hasher loginResourcesHasher = SHA_256.newHasher();
        Utils.addToHasher(loginResourcesHasher, "/wt_login/117/net/covers1624/wt/OfflineLaunch.java");
        HashCode hash1 = loginResourcesHasher.hash();

        Hasher loginFilesHasher = SHA_256.newHasher();
        Utils.addToHasher(loginFilesHasher, offlineLaunch);
        HashCode hash2 = loginFilesHasher.hash();

        if (hashContainer.check(HASH_GSTART_LOGIN, hash1) || !hash2.equals(hash1)) {
            Utils.extractResource("/wt_login/117/net/covers1624/wt/OfflineLaunch.java", offlineLaunch);
            hashContainer.set(HASH_GSTART_LOGIN, hash1);
        }
    }

    private void fixBullshitVersionRange() {
        try {
            List<String> lines = Files.readAllLines(forgeDir.resolve("build.gradle"), StandardCharsets.UTF_8);
            boolean unFucked = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("classpath 'net.minecraftforge.gradle:ForgeGradle:")) {
                    lines.set(i, line.replace(")", "]"));
                    unFucked = true;
                    break;
                }
            }
            if (unFucked) {
                Files.write(forgeDir.resolve("build.gradle"), lines, StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to un-fuck ForgeGradle dependency range.");
        }
    }
}

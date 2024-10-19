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
import net.covers1624.wt.forge.api.script.NeoForge120;
import net.covers1624.wt.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.collection.ColUtils.iterable;

/**
 * Created by covers1624 on 2/9/23.
 */
public class NeoForge120FrameworkHandler extends AbstractForge113PlusFrameworkHandler<NeoForge120> {

    public NeoForge120FrameworkHandler(WorkspaceToolContext context) {
        super(context);
    }

    @Override
    public void constructFrameworkModules(NeoForge120 frameworkImpl) {
        super.constructFrameworkModules(frameworkImpl);

        handleAts();

        extractLaunchResources();

        WorkspaceToolModel model = context.modelCache.getModel(forgeDir, emptySet(), Set.of("prepareRuns"));
        ProjectData projectData = model.getProjectData();
        boolean isNeo202Plus = projectData.subProjects.containsKey("neoforge");

        if (isNeo202Plus) {
            projectData.subProjects.remove("base"); // Useless for WorkspaceTool.
            projectData.subProjects.remove("tests"); // TODO make this configurable?
        } else {
            projectData.subProjects.remove("clean"); // Useless for WorkspaceTool.
        }
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

        List<Module> modules = ModuleImpl.makeGradleModules("", model.getProjectData(), context);
        context.frameworkModules.addAll(modules);

        GradleBackedModule forgeSubModule = ForgeExtension.findForgeSubModule(context);
        Configuration forgeRuntimeConfig = requireNonNull(forgeSubModule.getConfigurations().get("runtimeOnly"), "'runtimeOnly' Configuration is missing.");
        forgeRuntimeConfig.addDependency(getDevLoginDependency());

        Dependency forgeDep = new SourceSetDependencyImpl(forgeSubModule, "main");
        context.modules.forEach(m -> {
            m.getSourceSets().values().forEach(ss -> {
                ss.getCompileConfiguration().addDependency(forgeDep.copy());
                for (Path resourceDir : ss.getResources()) {
                    //If the SS has a mods.toml file, assume there is _some_ mod there
                    if (Files.exists(resourceDir.resolve("META-INF/mods.toml")) || Files.exists(resourceDir.resolve("META-INF/neoforge.mods.toml"))) {
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

        String mcVersion;
        if (isNeo202Plus) {
            mcVersion = projectData.extraProperties.get("minecraft_version");
        } else {
            mcVersion = projectData.extraProperties.get("MC_VERSION");
        }

        // Run forge setup if required.
        if (needsSetup) {
            String prefix = forgeSubModule.getName().replace("NeoForge/", "");
            runForgeSetup(of(), "clean");
            runForgeSetup(of(), "setup");
            if (isNeo202Plus) {
                runForgeSetup(of(), "idePostSync");
            }
            runForgeSetup(of(), ":" + prefix + ":compileJava");
            hashContainer.remove(HASH_MARKER_SETUP);
        }
        downloadAssets(mcVersion);
    }

    private void extractLaunchResources() {
        Path offlineLaunch = forgeDir.resolve("src/main/java/net/covers1624/wt/OfflineLaunch.java");

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
}

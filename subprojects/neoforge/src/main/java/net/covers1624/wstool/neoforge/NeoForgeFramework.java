package net.covers1624.wstool.neoforge;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.GitRepoManager;
import net.covers1624.wstool.api.HashContainer;
import net.covers1624.wstool.api.extension.Framework;
import net.covers1624.wstool.api.module.Dependency;
import net.covers1624.wstool.api.module.Module;
import net.covers1624.wstool.api.module.SourceSet;
import net.covers1624.wstool.api.module.WorkspaceBuilder;
import net.covers1624.wstool.gradle.api.data.JavaToolchainData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SubProjectList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Created by covers1624 on 5/19/25.
 */
public interface NeoForgeFramework extends Framework {

    String path();

    String url();

    String branch();

    String commit();

    @Override
    default void buildFrameworks(
            Environment env,
            BiFunction<Path, Set<String>, ProjectData> dataExtractor,
            BiFunction<WorkspaceBuilder, ProjectData, Module> moduleFactory,
            WorkspaceBuilder builder
    ) {
        Path rootDir = env.projectRoot().resolve(path());

        HashContainer hashContainer = new HashContainer(env.projectCache(), "neoforge");
        GitRepoManager gitManager = new GitRepoManager(rootDir);
        gitManager.setConfig(url(), branch(), commit());

        var requiresSetupProp = hashContainer.getProperty("requires_setup")
                .withDefault(true);

        try {
            if (gitManager.checkout()) {
                requiresSetupProp.setValue(true);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to update NeoForge clone.", ex);
        }

        var projectData = dataExtractor.apply(rootDir, Set.of());
        applyNeoForgeToolchain(projectData, builder);

        var nfModule = moduleFactory.apply(builder, projectData);
        var nfSubModule = nfModule.subModules().get("neoforge");
        var nfMain = nfSubModule.sourceSets().get("main");

        // TODO we should be able to extract this data from Gradle in some way.
        for (Module module : builder.modules().values()) {
            for (SourceSet ss : module.sourceSets().values()) {
                if (isNeoForgeModPresent(ss)) {
                    nfMain.runtimeDependencies().add(new Dependency.SourceSetDependency(ss));
                    ss.compileDependencies().add(new Dependency.SourceSetDependency(nfMain));
                }
            }
        }
    }

    private void applyNeoForgeToolchain(ProjectData projectData, WorkspaceBuilder builder) {
        var subProjectData = projectData.getData(SubProjectList.class);
        if (subProjectData == null) return;

        var nfSubProject = subProjectData.get("neoforge");
        if (nfSubProject == null) return;

        JavaToolchainData toolchainData = nfSubProject.getData(JavaToolchainData.class);
        if (toolchainData == null) return;

        builder.setJavaVersion(toolchainData.langVersion);
    }

    private static boolean isNeoForgeModPresent(SourceSet sourceSet) {
        for (Path resourcesDir : sourceSet.sourcePaths().getOrDefault("resources", List.of())) {
            if (Files.exists(resourcesDir.resolve("META-INF/mods.toml")) || Files.exists(resourcesDir.resolve("META-INF/neoforge.mods.toml"))) {
                return true;
            }
        }
        return false;
    }
}

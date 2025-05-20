package net.covers1624.wstool.neoforge;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.GitRepoManager;
import net.covers1624.wstool.api.HashContainer;
import net.covers1624.wstool.api.extension.Framework;
import net.covers1624.wstool.api.module.Module;
import net.covers1624.wstool.api.module.WorkspaceBuilder;
import net.covers1624.wstool.gradle.api.data.JavaToolchainData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SubProjectList;

import java.io.IOException;
import java.nio.file.Path;
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

        var module = moduleFactory.apply(builder, projectData);
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
}

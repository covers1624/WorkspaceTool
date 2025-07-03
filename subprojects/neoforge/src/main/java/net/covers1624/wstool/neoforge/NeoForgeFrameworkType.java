package net.covers1624.wstool.neoforge;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.GitRepoManager;
import net.covers1624.wstool.api.HashContainer;
import net.covers1624.wstool.api.ModuleProcessor;
import net.covers1624.wstool.api.extension.FrameworkType;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.SourceSet;
import net.covers1624.wstool.api.workspace.Workspace;
import net.covers1624.wstool.gradle.api.data.JavaToolchainData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SubProjectList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;


/**
 * Created by covers1624 on 5/19/25.
 */
public interface NeoForgeFrameworkType extends FrameworkType {

    String path();

    String url();

    String branch();

    String commit();

    @Override
    default void buildFrameworks(Environment env, ModuleProcessor moduleProcessor, Workspace workspace) {
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

        var nfModule = moduleProcessor.buildModule(workspace, rootDir, Set.of());
        applyNeoForgeToolchain(projectData, workspace);

        var nfSubModule = nfModule.subModules().get("neoforge");
        var nfMain = nfSubModule.sourceSets().get("main");

        // TODO we should be able to extract this data from Gradle in some way.
        for (Module module : workspace.modules().values()) {
            for (SourceSet ss : module.sourceSets().values()) {
                if (isNeoForgeModPresent(ss)) {
                    nfMain.runtimeDependencies().add(new Dependency.SourceSetDependency(ss));
                    ss.compileDependencies().add(new Dependency.SourceSetDependency(nfMain));
                }
            }
        }
    }

    private void applyNeoForgeToolchain(ProjectData projectData, Workspace workspace) {
        var subProjectData = projectData.getData(SubProjectList.class);
        if (subProjectData == null) return;

        var nfSubProject = subProjectData.get("neoforge");
        if (nfSubProject == null) return;

        JavaToolchainData toolchainData = nfSubProject.getData(JavaToolchainData.class);
        if (toolchainData == null) return;

        workspace.setJavaVersion(toolchainData.langVersion);
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

package net.covers1624.wstool.gradle.databuild;

import net.covers1624.wstool.gradle.LookupCache;
import net.covers1624.wstool.gradle.ProjectBuilder;
import net.covers1624.wstool.gradle.api.data.JavaToolchainData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.Nullable;

/**
 * Created by covers1624 on 5/20/25.
 */
public class ProjectToolchainDataBuilder implements ProjectBuilder {

    @Override
    public void buildProjectData(Project project, ProjectData projectData, LookupCache lookupCache) {
        JavaToolchainData data = null;
        if (GradleVersion.current().compareTo(GradleVersion.version("6.7")) >= 0) {
            data = extractViaPluginToolchainSpec(project);
        }

        if (data != null) {
            projectData.putData(JavaToolchainData.class, data);
        }
    }

    private static @Nullable JavaToolchainData extractViaPluginToolchainSpec(Project project) {
        JavaPluginExtension java = project.getExtensions().findByType(JavaPluginExtension.class);
        if (java == null) return null;

        JavaToolchainSpec toolchainSpec = java.getToolchain();

        if (!toolchainSpec.getLanguageVersion().isPresent()) {
            return null;
        }

        return new JavaToolchainData(
                toolchainSpec.getLanguageVersion().get().asInt()
        );
    }
}

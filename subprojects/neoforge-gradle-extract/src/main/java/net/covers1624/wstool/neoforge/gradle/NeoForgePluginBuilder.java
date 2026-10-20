package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.PluginBuilder;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.neoforge.gradle.api.NeoForgeGradleVersion;
import net.covers1624.wstool.neoforge.gradle.api.NeoForgeGradleVersion.Variant;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by covers1624 on 1/11/25.
 */
public class NeoForgePluginBuilder implements PluginBuilder {

    @Override
    public void buildPluginData(Project project, PluginData pluginData, List<String> additionalConfigurations) {
        NeoForgeGradleVersion version = detect(project, pluginData);
        if (version != null) {
            pluginData.putData(NeoForgeGradleVersion.class, version);
        }
    }

    private static @Nullable NeoForgeGradleVersion detect(Project project, PluginData pluginData) {
        if (pluginData.plugins.containsKey("net.neoforged.gradle")) {
            return new NeoForgeGradleVersion(
                    Variant.NEO_GRADLE_USER_DEV,
                    findViaClasspath(project, "net.neoforged", "NeoGradle")
            );
        }
        if (pluginData.plugins.containsKey("net.neoforged.gradle.userdev")) {
            return new NeoForgeGradleVersion(
                    Variant.NEO_GRADLE_USER_DEV,
                    findViaClasspath(project, "net.neoforged.gradle", "userdev")
            );
        }
        if (pluginData.plugins.containsKey("net.neoforged.gradle.neoform")) {
            return new NeoForgeGradleVersion(
                    Variant.NEO_GRADLE_NEO_FORM,
                    findViaClasspath(project, "net.neoforged.gradle", "neoform")
            );
        }
        if (pluginData.plugins.containsKey("net.neoforged.moddev")) {
            return new NeoForgeGradleVersion(
                    Variant.MOD_DEV_GRADLE,
                    findViaClasspath(project, "net.neoforged", "moddev-gradle")
            );
        }
        return null;
    }

    private static @Nullable String findViaClasspath(@Nullable Project project, String group, String name) {
        while (project != null) {
            String version = findViaProjectClasspath(project, group, name);
            if (version != null) {
                return version;
            }
            project = project.getParent();
        }

        return null;
    }

    private static @Nullable String findViaProjectClasspath(Project project, String group, String name) {
        ConfigurationContainer configurations = project.getBuildscript().getConfigurations();
        Configuration configuration = configurations.findByName("classpath");
        if (configuration == null || !configuration.isCanBeResolved()) {
            project.getLogger().error("Unable to probe 'classpath' configuration for buildscript of project: {}", project.getDisplayName());
            return null;
        }
        ResolvedArtifact artifact = FastStream.of(configuration.getResolvedConfiguration().getResolvedArtifacts())
                .filter(e -> e.getModuleVersion().getId().getGroup().equals(group))
                .filter(e -> e.getModuleVersion().getId().getName().equals(name))
                .firstOrDefault();
        if (artifact == null) return null;

        return artifact.getModuleVersion()
                .getId()
                .getVersion();
    }
}

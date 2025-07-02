package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.PluginBuilder;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.neoforge.gradle.api.NeoForgeGradleVersion;
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
        String ngVersion = findViaClasspath(project, "net.neoforged", "NeoGradle");
        if (ngVersion == null) {
            ngVersion = findViaClasspath(project, "net.neoforged.gradle", "userdev");
        }
        if (ngVersion != null) {
            pluginData.putData(NeoForgeGradleVersion.class, new NeoForgeGradleVersion(NeoForgeGradleVersion.Variant.NEO_GRADLE, ngVersion));
            return;
        }

        String mdgVersion = findViaClasspath(project, "net.neoforged", "moddev-gradle");
        if (mdgVersion != null) {
            pluginData.putData(NeoForgeGradleVersion.class, new NeoForgeGradleVersion(NeoForgeGradleVersion.Variant.MOD_DEV_GRADLE, mdgVersion));
        }
    }

    private static @Nullable String findViaClasspath(Project project, String group, String name) {
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

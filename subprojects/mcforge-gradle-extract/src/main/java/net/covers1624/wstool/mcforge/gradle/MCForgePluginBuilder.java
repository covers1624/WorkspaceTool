package net.covers1624.wstool.mcforge.gradle;

import groovy.lang.MetaProperty;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.PluginBuilder;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.mcforge.gradle.api.MCForgeGradleVersion;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.jetbrains.annotations.Nullable;

/**
 * Created by covers1624 on 1/11/25.
 */
public class MCForgePluginBuilder implements PluginBuilder {

    @Override
    public void buildPluginData(Project project, PluginData pluginData) {
        String version = findFGVersion(project);
        if (version != null) {
            pluginData.putData(MCForgeGradleVersion.class, new MCForgeGradleVersion(version));
        }
    }

    private static @Nullable String findFGVersion(Project project) {
        String version = findFG2Version(project);
        if (version != null) return version;

        return findFG3PlusVersion(project);
    }

    private static @Nullable String findFG2Version(Project project) {
        Object mcExtension = project.getExtensions().findByName("minecraft");
        if (mcExtension == null) return null;

        MetaProperty prop = DefaultGroovyMethods.hasProperty(mcExtension, "forgeGradleVersion");
        if (prop == null) return null;

        Object version = prop.getProperty(mcExtension);
        if (version == null) return null;

        return version.toString();
    }

    private static @Nullable String findFG3PlusVersion(Project project) {
        ConfigurationContainer configurations = project.getBuildscript().getConfigurations();
        Configuration configuration = configurations.findByName("classpath");
        if (configuration == null || !configuration.isCanBeResolved()) {
            project.getLogger().error("Unable to probe 'classpath' configuration for buildscript of project: {}", project.getDisplayName());
            return null;
        }
        ResolvedArtifact artifact = FastStream.of(configuration.getResolvedConfiguration().getResolvedArtifacts())
                .filter(e -> e.getModuleVersion().getId().getGroup().equals("net.minecraftforge.gradle"))
                .filter(e -> e.getModuleVersion().getId().getName().equals("ForgeGradle"))
                .firstOrDefault();
        if (artifact == null) return null;

        return artifact.getModuleVersion()
                .getId()
                .getVersion();
    }
}

package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.wstool.gradle.PluginBuilder;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.databuild.SourceSetDataBuilder;
import net.covers1624.wstool.neoforge.gradle.api.NeoDevData;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.List;

/**
 * Created by covers1624 on 5/25/25.
 */
public class NeoDevPluginBuilder implements PluginBuilder {

    @Override
    public void buildPluginData(Project project, PluginData pluginData, List<String> additionalConfigurations) {
        if (pluginData.plugins.containsKey("net.neoforged.gradle.platform.PlatformDevProjectPlugin")) {
            additionalConfigurations.add("moduleOnly");
            pluginData.putData(NeoDevData.class, new NeoDevData("moduleOnly"));
        } else if (pluginData.plugins.containsKey("net.neoforged.neodev.NeoDevPlugin")) {
            ConfigurationContainer configurations = project.getConfigurations();
            Configuration moduleLibraries = configurations.findByName("moduleLibraries");
            if (moduleLibraries != null) {
                SourceSetContainer sourceSets = SourceSetDataBuilder.getSourceSetContainer(project);
                SourceSet main = sourceSets.getByName("main");
                configurations.create("wstool_moduleOnly", c -> {
                    c.setCanBeResolved(true);
                    c.setCanBeConsumed(false);
                    c.shouldResolveConsistentlyWith(configurations.getByName(main.getRuntimeClasspathConfigurationName()));
                    c.extendsFrom(moduleLibraries);
                });
                additionalConfigurations.add("wstool_moduleOnly");
                pluginData.putData(NeoDevData.class, new NeoDevData("wstool_moduleOnly"));
            } else {
                pluginData.putData(NeoDevData.class, new NeoDevData(null));
            }
        }
    }
}

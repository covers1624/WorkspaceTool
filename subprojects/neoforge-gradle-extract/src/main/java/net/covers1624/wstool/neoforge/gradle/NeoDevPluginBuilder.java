package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.wstool.gradle.PluginBuilder;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.neoforge.gradle.api.NeoDevData;
import org.gradle.api.Project;

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
        }
    }
}

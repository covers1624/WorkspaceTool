package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.PluginData;
import org.gradle.api.Project;

/**
 * Created by covers1624 on 9/9/23.
 */
public interface PluginBuilder {

    void buildPluginData(Project project, PluginData pluginData);
}

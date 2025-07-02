package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.gradle.api.Project;

import java.util.List;

/**
 * Plugin builders are used to extract any plugin information, or static information
 * specific to the plugin, which may be used by transformers, project builders, or
 * another non-gradle side operation.
 * <p>
 * Plugin builders are run interleaved with other Plugin builders whilst the {@link ProjectData}
 * tree is being built.
 * <p>
 * Created by covers1624 on 9/9/23.
 */
public interface PluginBuilder {

    /**
     * Parse any additional plugin related data from the project and emit them
     * into the provided {@link PluginData}.
     *
     * @param project    The project to extract from.
     * @param pluginData The plugin data to store into.
     */
    void buildPluginData(Project project, PluginData pluginData, List<String> additionalConfigurations);
}

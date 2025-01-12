package net.covers1624.wstool.mcforge.gradle;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.ProjectTransformer;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.mcforge.gradle.api.MCForgeGradleVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.util.List;

/**
 * Created by covers1624 on 1/12/25.
 */
public class MCForgeProjectTransformer implements ProjectTransformer {

    @Override
    public void transformProject(Project project, ProjectData projectData) {
        PluginData pluginData = projectData.pluginData();
        MCForgeGradleVersion fgVersion = pluginData.getData(MCForgeGradleVersion.class);
        if (fgVersion == null) return;

        for (Configuration configuration : project.getConfigurations()) {
            if (ColUtils.anyMatch(configuration.getExtendsFrom(), e -> e.getName().equals("minecraft"))) {
                List<Configuration> newExtendsFrom = FastStream.of(configuration.getExtendsFrom())
                        .filter(e-> !e.getName().equals("minecraft"))
                        .toList();
                configuration.setExtendsFrom(newExtendsFrom);
            }
        }
    }
}

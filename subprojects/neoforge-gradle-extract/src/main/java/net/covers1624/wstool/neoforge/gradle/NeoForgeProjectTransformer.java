package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.ProjectTransformer;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.neoforge.gradle.api.NeoForgeGradleVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by covers1624 on 1/12/25.
 */
public class NeoForgeProjectTransformer implements ProjectTransformer {

    @Override
    public void transformProject(Project project, ProjectData projectData) {
        PluginData pluginData = projectData.pluginData();
        NeoForgeGradleVersion version = pluginData.getData(NeoForgeGradleVersion.class);
        if (version == null) return;

        if (version.variant == NeoForgeGradleVersion.Variant.NEO_GRADLE) {
            filterExtendsFrom(project, e -> e.equals("minecraft"));
            filterExtendsFrom(project, e -> e.startsWith("ng_dummy_ng_"));
        } else if (version.variant == NeoForgeGradleVersion.Variant.MOD_DEV_GRADLE) {
            filterExtendsFrom(project, e -> e.startsWith("modDev"));
        }
    }

    private static void filterExtendsFrom(Project project, Predicate<String> toRemove) {
        for (Configuration configuration : project.getConfigurations()) {
            if (ColUtils.anyMatch(configuration.getExtendsFrom(), e -> toRemove.test(e.getName()))) {
                List<Configuration> newExtendsFrom = FastStream.of(configuration.getExtendsFrom())
                        .filterNot(e -> toRemove.test(e.getName()))
                        .toList();
                configuration.setExtendsFrom(newExtendsFrom);
            }
        }
    }
}

package net.covers1624.wstool.mcforge.gradle;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.LookupCache;
import net.covers1624.wstool.gradle.ProjectBuilder;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetAttribute;
import net.covers1624.wstool.gradle.databuild.SourceSetDataBuilder;
import net.covers1624.wstool.mcforge.gradle.api.MCForgeGradleVersion;
import net.covers1624.wstool.minecraft.ModLoaderAttribute;
import net.covers1624.wstool.minecraft.SideAttribute;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.Arrays;
import java.util.List;

/**
 * Created by covers1624 on 1/12/25.
 */
public class MCForgeProjectBuilder implements ProjectBuilder {

    private static final List<SourceSetAttribute> FORGE_JOINED = Arrays.asList(ModLoaderAttribute.MC_FORGE, SideAttribute.JOINED);

    @Override
    public void preTransformProject(Project project, ProjectData projectData, LookupCache lookup) {
        PluginData pluginData = projectData.pluginData();
        MCForgeGradleVersion fgVersion = pluginData.getData(MCForgeGradleVersion.class);
        if (fgVersion == null) return;

        if (fgVersion.version.startsWith("2.3.")) {
            addLegacyAttributes(project, lookup);
        }

        for (Configuration configuration : project.getConfigurations()) {
            if (ColUtils.anyMatch(configuration.getExtendsFrom(), e -> e.getName().equals("minecraft"))) {
                List<Configuration> newExtendsFrom = FastStream.of(configuration.getExtendsFrom())
                        .filter(e -> !e.getName().equals("minecraft"))
                        .toList();
                configuration.setExtendsFrom(newExtendsFrom);
                lookup.addAttributes(configuration, FORGE_JOINED);
            }
        }
    }

    private static void addLegacyAttributes(Project project, LookupCache lookup) {
        SourceSetContainer sourceSets = SourceSetDataBuilder.getSourceSetContainer(project);
        if (sourceSets == null) return;

        SourceSet ss = sourceSets.findByName("main");
        if (ss == null) return;

        lookup.addAttributes(ss, FORGE_JOINED);
    }
}

package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.LookupCache;
import net.covers1624.wstool.gradle.ProjectBuilder;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetAttribute;
import net.covers1624.wstool.gradle.databuild.SourceSetDataBuilder;
import net.covers1624.wstool.minecraft.ModLoaderAttribute;
import net.covers1624.wstool.minecraft.SideAttribute;
import net.covers1624.wstool.neoforge.gradle.api.NeoForgeGradleVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by covers1624 on 1/12/25.
 */
public class NeoForgeProjectBuilder implements ProjectBuilder {

    private static final List<SourceSetAttribute> NEO_JOINED = Arrays.asList(ModLoaderAttribute.NEO_FORGE, SideAttribute.JOINED);
    private static final List<SourceSetAttribute> VANILLA_JOINED = Arrays.asList(ModLoaderAttribute.VANILLA, SideAttribute.JOINED);
    private static final List<SourceSetAttribute> VANILLA_CLIENT = Arrays.asList(ModLoaderAttribute.VANILLA, SideAttribute.CLIENT);
    private static final List<SourceSetAttribute> VANILLA_SERVER = Arrays.asList(ModLoaderAttribute.VANILLA, SideAttribute.SERVER);

    @Override
    public void preTransformProject(Project project, ProjectData projectData, LookupCache lookup) {
        PluginData pluginData = projectData.pluginData();
        NeoForgeGradleVersion version = pluginData.getData(NeoForgeGradleVersion.class);
        if (version == null) return;

        if (version.variant == NeoForgeGradleVersion.Variant.NEO_GRADLE_USER_DEV) {
            filterExtendsFrom(project, e -> e.equals("minecraft"), (config, rem) -> lookup.addAttributes(config, NEO_JOINED));
            filterExtendsFrom(project, e -> e.startsWith("ng_dummy_ng_"), (config, rem) -> lookup.addAttributes(config, NEO_JOINED));
            filterClasspaths(project, e -> "net.neoforged".equals(e.getGroup()) && "neoforge".equals(e.getName()), config -> lookup.addAttributes(config, NEO_JOINED));
        } else if (version.variant == NeoForgeGradleVersion.Variant.NEO_GRADLE_NEO_FORM) {
            filterClasspaths(project, e -> endsWithSafe("net.minecraft", e.getGroup()) && "neoform_joined".equals(e.getName()), config -> lookup.addAttributes(config, VANILLA_JOINED));
            filterClasspaths(project, e -> endsWithSafe("net.minecraft", e.getGroup()) && "neoform_client".equals(e.getName()), config -> lookup.addAttributes(config, VANILLA_CLIENT));
            filterClasspaths(project, e -> endsWithSafe("net.minecraft", e.getGroup()) && "neoform_server".equals(e.getName()), config -> lookup.addAttributes(config, VANILLA_SERVER));
        } else if (version.variant == NeoForgeGradleVersion.Variant.MOD_DEV_GRADLE) {
            filterExtendsFrom(project, e -> e.startsWith("modDev"), (configuration, rem) -> {
                List<Dependency> removedDeps = FastStream.of(rem)
                        .flatMap(Configuration::getDependencies)
                        .toList();
                if (ColUtils.anyMatch(removedDeps, e -> "net.neoforged".equals(e.getGroup()) && "neoform".equals(e.getName()))) {
                    lookup.addAttributes(configuration, VANILLA_JOINED);
                }

                if (ColUtils.anyMatch(removedDeps, e -> "net.neoforged".equals(e.getGroup()) && "neoforge".equals(e.getName()))) {
                    lookup.addAttributes(configuration, NEO_JOINED);
                }
            });
        }
    }

    private void filterExtendsFrom(Project project, Predicate<String> toRemove, BiConsumer<Configuration, Set<Configuration>> onRemoved) {
        for (Configuration configuration : project.getConfigurations()) {
            Set<Configuration> removed = FastStream.of(configuration.getExtendsFrom())
                    .filter(e -> toRemove.test(e.getName()))
                    .toLinkedHashSet();
            if (removed.isEmpty()) continue;

            List<Configuration> newExtendsFrom = FastStream.of(configuration.getExtendsFrom())
                    .filterNot(removed::contains)
                    .toList();
            configuration.setExtendsFrom(newExtendsFrom);

            onRemoved.accept(configuration, removed);
        }
    }

    private static void filterClasspaths(Project project, Predicate<Dependency> predicate, Consumer<Configuration> onTransformed) {
        ConfigurationContainer configurations = project.getConfigurations();
        SourceSetContainer sourceSets = SourceSetDataBuilder.getSourceSetContainer(project);
        if (sourceSets == null) return;

        for (SourceSet sourceSet : sourceSets) {
            filterClasspath(configurations.getByName(sourceSet.getCompileClasspathConfigurationName()), predicate, onTransformed);
            filterClasspath(configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()), predicate, onTransformed);
        }
    }

    private static void filterClasspath(Configuration configuration, Predicate<Dependency> predicate, Consumer<Configuration> onTransformed) {
        boolean removed = false;
        for (Configuration config : configuration.getHierarchy()) {
            removed |= config.getDependencies().removeIf(predicate);
        }
        if (removed) {
            onTransformed.accept(configuration);
        }
    }

    private static boolean endsWithSafe(@Nullable String string, @Nullable String suffix) {
        return string != null && suffix != null && string.endsWith(suffix);
    }
}

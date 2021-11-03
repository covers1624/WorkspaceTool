/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.gradle;

import groovy.lang.MetaProperty;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.event.VersionedClass;
import net.covers1624.wt.api.gradle.data.ConfigurationData;
import net.covers1624.wt.api.gradle.data.PluginData;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.forge.gradle.data.*;
import net.covers1624.wt.gradle.builder.ExtraDataBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.*;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Created by covers1624 on 18/6/19.
 */
@VersionedClass (5)
public class FGDataBuilder implements ExtraDataBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(FGDataBuilder.class);

    private static final String FG2_USER_PLUGIN = "net.minecraftforge.gradle.forge";
    private static final String FG2_USER_PLUGIN_CLASS = "net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin";
    private static final String FG2_PATCHER_PLUGIN = "net.minecraftforge.gradle.patcher";
    private static final String FG2_PATCHER_PLUGIN_Class = "net.minecraftforge.gradle.patcher.PatcherPlugin";

    private static final String FG3_USER_PLUGIN = "net.minecraftforge.gradle";
    private static final String FG3_USER_PLUGIN_CLASS = "net.minecraftforge.gradle";
    private static final String FG3_PATCHER_PLUGIN = "net.minecraftforge.gradle.patcher";
    private static final String FG3_PATCHER_PLUGIN_Class = "net.minecraftforge.gradle.patcher.PatcherPlugin";

    @Override
    @SuppressWarnings ("unchecked")
    public void preBuild(Project project, PluginData pluginData) throws Exception {
        //Detect ForgeGradle version.
        FGPluginData fgPluginData = new FGPluginData();
        fgPluginData.version = FGVersion.UNKNOWN;
        String fg2VString = tryGetFG2Version(project);
        if (fg2VString != null) {
            fgPluginData.versionString = fg2VString;
            if (fg2VString.startsWith("2.2")) {
                fgPluginData.version = FGVersion.FG22;
            } else if (fg2VString.startsWith("2.3")) {
                fgPluginData.version = FGVersion.FG23;
            } else {
                LOGGER.error("Failed to parse FG2 Version: {}", fg2VString);
                throw new RuntimeException("Failed to parse FG2Version.");
            }
        } else {
            //For now we have to parse classpath stuffs.
            Configuration classpath = project.getBuildscript().getConfigurations().findByName("classpath");
            if (!classpath.isCanBeResolved()) {
                throw new RuntimeException("Unable to resolve classpath configuration.");
            }
            Set<ResolvedArtifact> artifacts = classpath.getResolvedConfiguration().getResolvedArtifacts();
            Optional<ResolvedArtifact> fgArtifact = artifacts.parallelStream()
                    .map(e -> Pair.of(e, e.getModuleVersion().getId()))
                    .filter(e -> e.getRight().getGroup().equals("net.minecraftforge.gradle"))
                    .filter(e -> e.getRight().getName().equals("ForgeGradle"))
                    .map(Pair::getLeft)
                    .findFirst();
            if (!fgArtifact.isPresent()) {
                LOGGER.error("Unable to find ForgeGradle artifact in buildscript classpath configuration.");
                LOGGER.debug("Found artifacts: '{}'", artifacts.parallelStream()
                        .map(ResolvedArtifact::getModuleVersion)
                        .map(ResolvedModuleVersion::getId)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "))
                );
                return;
            }
            ModuleVersionIdentifier ident = fgArtifact.get().getModuleVersion().getId();
            String version = ident.getVersion();
            fgPluginData.versionString = version;
            if (version.startsWith("2.2")) {
                fgPluginData.version = FGVersion.FG22;
            } else if (version.startsWith("2.3")) {
                fgPluginData.version = FGVersion.FG23;
            } else if (version.startsWith("3.0")) {
                fgPluginData.version = FGVersion.FG30;
            } else if (version.startsWith("4.0")) {
                fgPluginData.version = FGVersion.FG40;
            } else if (version.startsWith("4.1")) {
                fgPluginData.version = FGVersion.FG41;
            } else if (version.startsWith("5.0")) {
                fgPluginData.version = FGVersion.FG50;
            } else if (version.startsWith("5.1")) {
                fgPluginData.version = FGVersion.FG51;
            } else {
                LOGGER.error("Unknown FG version: '{}', From: '{}' in project {}({})", version, ident, project.getPath(), project.getDisplayName());
            }
        }
        if (fgPluginData.version != FGVersion.UNKNOWN) {
            LOGGER.info("Found ForgeGradle! Version: {} in project {}({})", fgPluginData.version, project.getPath(), project.getDisplayName());
            pluginData.extraData.put(FGPluginData.class, fgPluginData);
        } else {
            LOGGER.warn("Failed to find ForgeGradle in project {}({}). :(", project.getPath(), project.getDisplayName());
            return;
        }

        if (fgPluginData.version.isFG2() && pluginData.pluginIds.contains(FG2_PATCHER_PLUGIN)) {
            Task genGradleProjects = project.getTasks().findByName("genGradleProjects");
            if (genGradleProjects != null) {
                Field field = genGradleProjects.getClass().getSuperclass().getDeclaredField("dependencies");
                field.setAccessible(true);
                List<String> dependencies = (List<String>) field.get(genGradleProjects);
                dependencies.forEach(dep -> {
                    if (dep.startsWith("testCompile")) {
                        String trimmed = dep.replace("testCompile", "").replace("'", "").trim();
                        project.getDependencies().add("testCompile", trimmed);
                    }
                });
            }
        }
    }

    @Override
    public void build(Project project, ProjectData projectData, ProjectData rootData) throws Exception {
        PluginData pluginData = projectData.pluginData;
        FGPluginData fgPluginData = rootData.pluginData.getData(FGPluginData.class);

        if (fgPluginData == null) {
            LOGGER.info("Null plugin data.");
            return;
        }
        FGVersion version = fgPluginData.version;
        if (version.isFG2()) {
            FG2.build(project, pluginData, projectData);
        } else if (version.isFG3Compatible()) {
            FG3Plus.build(project, pluginData, projectData);
        }
    }

    public static class FG2 {

        public static void build(Project project, PluginData pluginData, ProjectData projectData) {
            projectData.extraData.put(FG2Data.class, buildForgeProjectData(project, pluginData));
            projectData.extraData.put(FG2McpMappingData.class, buildMappingData(project));
        }

        private static FG2Data buildForgeProjectData(Project project, PluginData pluginData) {
            FG2Data data = new FG2Data();

            Object baseExtension = project.getExtensions().getByName("minecraft");
            data.mcpMappings = getProperty(baseExtension, "mappings");
            data.mcVersion = getProperty(baseExtension, "version");
            if (!pluginData.pluginIds.contains(FG2_PATCHER_PLUGIN)) {
                data.forgeVersion = getProperty(baseExtension, "forgeVersion");
            } else {
                data.forgeVersion = String.valueOf(project.getVersion());
            }

            for (Jar task : project.getTasks().withType(Jar.class)) {
                Attributes attribs = task.getManifest().getAttributes();
                String cm = (String) attribs.get("FMLCorePlugin");
                String tweaker = (String) attribs.get("TweakClass");
                if (cm != null) {
                    data.fmlCoreMods.add(cm);
                }
                if (tweaker != null) {
                    data.tweakClasses.add(tweaker);
                }
            }
            return data;
        }

        private static FG2McpMappingData buildMappingData(Project project) {
            FG2McpMappingData data = new FG2McpMappingData();
            ConfigurationContainer configurations = project.getConfigurations();
            TaskContainer tasks = project.getTasks();
            Task genSrgs = tasks.getByName("genSrgs");
            Task mergeJars = tasks.getByName("mergeJars");

            if (!genSrgs.getState().getExecuted() || !mergeJars.getState().getExecuted()) {
                throw new RuntimeException("GenSrgs or MergeJars did not run.");
            }
            Configuration configMappings = configurations.getByName("forgeGradleMcpMappings");
            Configuration configMcpData = configurations.getByName("forgeGradleMcpData");

            Dependency mappingsArtifact = ColUtils.headOption(configMappings.getDependencies())
                    .orElseThrow(() -> new RuntimeException("Empty."));
            Dependency mcpDataArtifact = ColUtils.headOption(configMcpData.getDependencies())
                    .orElseThrow(() -> new RuntimeException("Empty."));

            data.mappingsArtifact = zipNotationOf(mappingsArtifact);
            data.mcpDataArtifact = zipNotationOf(mcpDataArtifact);

            data.mappings = ColUtils.headOption(configMappings.getResolvedConfiguration().getFiles())
                    .orElseThrow(() -> new RuntimeException("Empty"));
            data.data = ColUtils.headOption(configMcpData.getResolvedConfiguration().getFiles())
                    .orElseThrow(() -> new RuntimeException("Empty"));

            data.mergedJar = getProperty(mergeJars, "outJar");

            data.notchToSrg = FGDataBuilder.<File>getProperty(genSrgs, "notchToSrg").getAbsoluteFile();
            data.notchToMcp = FGDataBuilder.<File>getProperty(genSrgs, "notchToMcp").getAbsoluteFile();
            data.mcpToNotch = FGDataBuilder.<File>getProperty(genSrgs, "mcpToNotch").getAbsoluteFile();
            data.srgToMcp = FGDataBuilder.<File>getProperty(genSrgs, "srgToMcp").getAbsoluteFile();
            data.mcpToSrg = FGDataBuilder.<File>getProperty(genSrgs, "mcpToSrg").getAbsoluteFile();

            return data;
        }
    }

    public static class FG3Plus {

        public static void build(Project project, PluginData pluginData, ProjectData projectData) {
            FG3Data data = new FG3Data();
            projectData.extraData.put(FG3Data.class, data);
            Object mcExt = project.getExtensions().findByName("minecraft");
            Object patcherExt = project.getExtensions().findByName("patcher");
            Object extension = mcExt != null ? mcExt : patcherExt;

            if (extension != null) {
                data.accessTransformers = coerceToList(tryGetProperty(extension, "accessTransformers"));
                data.sideAnnotationStrippers = coerceToList(tryGetProperty(extension, "sideAnnotationStrippers"));
            }
            Optional<ConfigurationData.MavenDependency> mappings = projectData.configurations.values().stream()
                    .flatMap(e -> e.dependencies.stream())
                    .filter(e -> e instanceof ConfigurationData.MavenDependency)
                    .map(e -> ((ConfigurationData.MavenDependency) e))
                    .filter(e ->
                            e.mavenNotation.group.equals("net.minecraft")
                                    && (e.mavenNotation.module.startsWith("mappings_")
                                    || e.mavenNotation.module.startsWith("official_"))
                    )
                    .findFirst();
            mappings.ifPresent(e -> {
                LOGGER.info("Found MCP mappings.");
                FG3McpMappingData fg3MappingData = new FG3McpMappingData();
                fg3MappingData.mappingsArtifact = e.mavenNotation;
                fg3MappingData.mappingsZip = e.classes;
                projectData.extraData.put(FG3McpMappingData.class, fg3MappingData);
            });
            if (!mappings.isPresent()) {
                LOGGER.warn("MCP Mappings not found in project!");
            }
        }
    }

    private static String tryGetFG2Version(Project project) {
        Object baseExtension = project.getExtensions().findByName("minecraft");
        if (baseExtension != null) {
            return tryGetProperty(baseExtension, "forgeGradleVersion");
        }
        return null;
    }

    public static <T> T tryGetProperty(Object object, String name) {
        MetaProperty prop = DefaultGroovyMethods.hasProperty(object, name);
        if (prop == null) {
            return null;
        }
        return unsafeCast(prop.getProperty(object));
    }

    private static <T> T getProperty(Object object, String name) {
        T thing = tryGetProperty(object, name);
        if (thing == null) {
            throw new RuntimeException("Property not found: " + name);
        }
        return thing;
    }

    private static <T> List<T> coerceToList(Object o) {
        if (o instanceof List) return unsafeCast(o);
        if (o instanceof ConfigurableFileCollection) {
            return unsafeCast(new ArrayList<>(((ConfigurableFileCollection) o).getFiles()));
        }
        throw new IllegalArgumentException("Unable to coerce '" + o.getClass().getName() + "' to a List.");
    }

    private static MavenNotation zipNotationOf(Dependency dep) {
        return new MavenNotation(dep.getGroup(), dep.getName(), dep.getVersion(), null, "zip");
    }

}

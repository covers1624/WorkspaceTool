package net.covers1624.wt.forge.gradle;

import com.google.common.collect.Sets;
import groovy.lang.MetaProperty;
import net.covers1624.wt.api.data.GradleData;
import net.covers1624.wt.api.data.PluginData;
import net.covers1624.wt.event.VersionedClass;
import net.covers1624.wt.forge.gradle.data.ForgeGradleData;
import net.covers1624.wt.forge.gradle.data.ForgeGradlePluginData;
import net.covers1624.wt.forge.gradle.data.McpMappingData;
import net.covers1624.wt.gradle.builder.ExtraDataBuilder;
import net.covers1624.wt.util.ColUtils;
import net.covers1624.wt.util.MavenNotation;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Created by covers1624 on 18/6/19.
 */
@VersionedClass (2)
public class ForgeGradleDataBuilder implements ExtraDataBuilder {

    private static Logger logger = (Logger) LoggerFactory.getLogger("ForgeGradleDataBuilder");

    private static final String FG_USER_PLUGIN = "net.minecraftforge.gradle.forge";
    private static final String FG_PATCHER_PLUGIN = "net.minecraftforge.gradle.patcher";

    private static final Set<String> FG2_PLUGINS = Sets.newHashSet(FG_USER_PLUGIN, FG_PATCHER_PLUGIN);

    @Override
    @SuppressWarnings ("unchecked")
    public void preGradleData(Project project, PluginData pluginData) throws Exception {
        if (pluginData.pluginIds.contains(FG_PATCHER_PLUGIN)) {
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
    public void build(Project project, PluginData pluginData, GradleData gradleData) throws Exception {
        if (pluginData.pluginIds.parallelStream().anyMatch(FG2_PLUGINS::contains)) {
            pluginData.extraData.put(ForgeGradlePluginData.class, buildFG2PluginData(project));
            gradleData.extraData.put(ForgeGradleData.class, buildForgeGradleData(project, pluginData));
            gradleData.extraData.put(McpMappingData.class, buildMappingData(project));
        }

    }

    private ForgeGradlePluginData buildFG2PluginData(Project project) throws Exception {
        ForgeGradlePluginData fgPluginData = new ForgeGradlePluginData();
        Object baseExtension = project.getExtensions().getByName("minecraft");
        fgPluginData.versionString = getProperty(baseExtension, "forgeGradleVersion");
        if (fgPluginData.versionString.startsWith("2.")) {
            fgPluginData.version = ForgeGradleVersion.FORGE_GRADLE_2;
        } else {
            logger.error("Failed to parse FG version. '{}'", fgPluginData.versionString);
            fgPluginData.version = ForgeGradleVersion.UNKNOWN;
        }
        return fgPluginData;
    }

    private ForgeGradleData buildForgeGradleData(Project project, PluginData pluginData) {
        ForgeGradleData data = new ForgeGradleData();

        Object baseExtension = project.getExtensions().getByName("minecraft");
        data.mcpMappings = getProperty(baseExtension, "mappings");
        data.mcVersion = getProperty(baseExtension, "version");
        if (!pluginData.pluginIds.contains(FG_PATCHER_PLUGIN)) {
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

    private McpMappingData buildMappingData(Project project) {
        McpMappingData data = new McpMappingData();
        ConfigurationContainer configurations = project.getConfigurations();
        TaskContainer tasks = project.getTasks();
        Task genSrgs = tasks.getByName("genSrgs");
        Task mergeJars = tasks.getByName("mergeJars");

        if (!genSrgs.getState().getExecuted() || !mergeJars.getState().getExecuted()) {
            throw new RuntimeException("GenSrgs or MergeJars did not run.");
        }
        Configuration configMappings = configurations.getByName("forgeGradleMcpMappings");
        Configuration configMcpData = configurations.getByName("forgeGradleMcpData");

        Dependency mappingsArtifact = ColUtils.head(configMappings.getDependencies());
        Dependency mcpDataArtifact = ColUtils.head(configMcpData.getDependencies());

        data.mappingsArtifact = zipNotationOf(mappingsArtifact);
        data.mcpDataArtifact = zipNotationOf(mcpDataArtifact);

        data.mappings = ColUtils.head(configMappings.getResolvedConfiguration().getFiles());
        data.data = ColUtils.head(configMcpData.getResolvedConfiguration().getFiles());

        data.mergedJar = getProperty(mergeJars, "outJar");

        data.notchToSrg = ForgeGradleDataBuilder.<File>getProperty(genSrgs, "notchToSrg").getAbsoluteFile();
        data.notchToMcp = ForgeGradleDataBuilder.<File>getProperty(genSrgs, "notchToMcp").getAbsoluteFile();
        data.mcpToNotch = ForgeGradleDataBuilder.<File>getProperty(genSrgs, "mcpToNotch").getAbsoluteFile();
        data.srgToMcp = ForgeGradleDataBuilder.<File>getProperty(genSrgs, "srgToMcp").getAbsoluteFile();
        data.mcpToSrg = ForgeGradleDataBuilder.<File>getProperty(genSrgs, "mcpToSrg").getAbsoluteFile();

        return data;
    }

    @SuppressWarnings ("unchecked")
    private static <T> T getProperty(Object object, String name) {
        MetaProperty prop = DefaultGroovyMethods.hasProperty(object, name);
        if (prop == null) {
            throw new RuntimeException("Property not found: " + name);
        }
        return (T) prop.getProperty(object);
    }

    private static MavenNotation zipNotationOf(Dependency dep) {
        return new MavenNotation(dep.getGroup(), dep.getName(), dep.getVersion(), null, "zip");
    }

}

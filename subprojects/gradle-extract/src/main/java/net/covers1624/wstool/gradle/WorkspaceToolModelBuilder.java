package net.covers1624.wstool.gradle;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.wstool.gradle.api.ModelProperties;
import net.covers1624.wstool.gradle.api.WorkspaceToolModel;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SubProjectList;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by covers1624 on 14/5/23.
 */
public class WorkspaceToolModelBuilder implements ParameterizedToolingModelBuilder<ModelProperties> {

    @Override
    public Object buildAll(String modelName, ModelProperties properties, Project project) {
        try {
            ClassLoader cl = WorkspaceToolModelBuilder.class.getClassLoader();
            List<PluginBuilder> pluginBuilders = FastStream.of(ServiceLoader.load(PluginBuilder.class, cl))
                    .toList();
            List<ProjectBuilder> projectBuilders = FastStream.of(ServiceLoader.load(ProjectBuilder.class, cl))
                    .toList();
            List<ProjectTransformer> projectTransformers = FastStream.of(ServiceLoader.load(ProjectTransformer.class, cl))
                    .toList();

            LookupCache lookupCache = new LookupCache();
            ProjectData projectData = buildProjectTree(project, null, pluginBuilders, lookupCache);

            for (ProjectTransformer transformer : projectTransformers) {
                lookupCache.projects.forEach((proj, data) -> transformer.transformProject(project.findProject(proj), data));
            }

            for (ProjectBuilder builder : projectBuilders) {
                lookupCache.projects.forEach((proj, data) -> {
                    builder.buildProjectData(project.findProject(proj), data, lookupCache);
                });
            }

            try (ObjectOutputStream os = new ObjectOutputStream(Files.newOutputStream(IOUtils.makeParents(properties.getOutputFile().toPath())))) {
                os.writeObject(projectData);
            }
        } catch (Throwable ex) {
            project.getLogger().error("Fatal error building model.");
            project.getLogger().error("If this is a Java 8 Gradle instance, this may crash WorkspaceTool with a serialization error.");
            project.getLogger().error("", ex);
            throw new RuntimeException("Fatal exception building model.", ex);
        }

        return new WorkspaceToolModel.Dummy();
    }

    // @formatter:off
    @Override public Class<ModelProperties> getParameterType() { return ModelProperties.class; }
    @Override public boolean canBuild(String modelName) { return modelName.equals(WorkspaceToolModel.class.getName()); }
    @Override public Object buildAll(String modelName, Project project) { throw new UnsupportedOperationException(); }
    // @formatter:on

    // Build the project tree lookup map and subProject data.
    private static ProjectData buildProjectTree(Project project, @Nullable ProjectData parent, List<PluginBuilder> pluginBuilders, LookupCache lookupCache) {
        ProjectData projectData = new ProjectData(
                project.getName(),
                project.getProjectDir(),
                parent,
                String.valueOf(project.getVersion()),
                String.valueOf(project.getGroup()),
                String.valueOf(project.findProperty("archivesBaseName"))
        );
        projectData.putData(PluginData.class, buildPlugins(project, pluginBuilders, lookupCache));

        if (lookupCache.projects.containsKey(project.getPath())) {
            throw new IllegalStateException("Already visited project: " + project);
        }
        lookupCache.projects.put(project.getPath(), projectData);

        SubProjectList subProjectData = new SubProjectList();
        for (Project subProj : project.getSubprojects()) {
            subProjectData.put(subProj.getName(), buildProjectTree(subProj, projectData, pluginBuilders, lookupCache));
        }
        projectData.putData(SubProjectList.class, subProjectData);

        return projectData;
    }

    // Build all plugin specific data for the project.
    private static PluginData buildPlugins(Project project, List<PluginBuilder> dataBuilders, LookupCache lookupCache) {
        PluginContainer plugins = project.getPlugins();

        Map<String, String> pluginMap = getPluginMap(plugins);
        PluginData data = new PluginData();
        FastStream.of(plugins)
                .map(Plugin::getClass)
                .map(Class::getName)
                .forEach(cName -> {
                    data.plugins.put(pluginMap.getOrDefault(cName, cName), cName);
                });

        List<String> additional = lookupCache.additionalConfigurations.computeIfAbsent(project, e -> new ArrayList<>());
        for (PluginBuilder builder : dataBuilders) {
            builder.buildPluginData(project, data, additional);
        }
        return data;
    }

    private static Map<String, String> getPluginMap(PluginContainer plugins) {
        Map<String, String> classToName = new HashMap<>();
        Set<String> parsed = new HashSet<>();
        for (Plugin<?> plugin : plugins) {
            try {
                for (URL url : ColUtils.iterable(plugin.getClass().getClassLoader().getResources("META-INF/gradle-plugins"))) {
                    String f = url.getFile();
                    if (!parsed.add(f)) continue;

                    FileSystem fs;
                    Path resourcePath;
                    switch (url.getProtocol()) {
                        case "jar": {
                            int sep = f.indexOf("!/");
                            fs = IOUtils.getJarFileSystem(URI.create(f.substring(0, sep)), false);
                            resourcePath = fs.getPath(f.substring(sep + 1)); // + 1 to strip the ! and leave the / from !/
                            break;
                        }
                        case "file": {
                            fs = null;
                            resourcePath = Paths.get(f);
                            break;
                        }
                        default:
                            continue; // Ignore the file.
                    }

                    if (Files.notExists(resourcePath)) continue;

                    try (Stream<Path> s = Files.walk(resourcePath)) {
                        for (Path path : (Iterable<Path>) s::iterator) {
                            if (!Files.isRegularFile(path)) continue;
                            if (!path.getFileName().toString().endsWith(".properties")) continue;
                            String pName = path.getFileName().toString().replace(".properties", "");
                            try (InputStream is = Files.newInputStream(path)) {
                                Properties properties = new Properties();
                                properties.load(is);
                                String cName = properties.getProperty("implementation-class");
                                if (!classToName.containsKey(cName)) {
                                    classToName.put(cName, pName);
                                }
                            }
                        }
                    }
                    if (fs != null) {
                        fs.close();
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException("Failed to read plugin metadata.", ex);
            }
        }
        return classToName;
    }
}

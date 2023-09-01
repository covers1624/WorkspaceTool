package net.covers1624.wstool.gradle;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.wstool.gradle.api.ModelProperties;
import net.covers1624.wstool.gradle.api.WorkspaceToolModel;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
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
    public Class<ModelProperties> getParameterType() {
        return ModelProperties.class;
    }

    @Override
    public Object buildAll(String modelName, ModelProperties properties, Project project) {
        try {
            List<DataBuilder> dataBuilders = new LinkedList<>();
            dataBuilders.add(new ProjectExtDataBuilder());
            dataBuilders.add(new SourceSetDataListBuilder());

            for (String builderClazz : properties.getDataBuilders()) {
                try {
                    dataBuilders.add((DataBuilder) Class.forName(builderClazz).getConstructor().newInstance());
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to instantiate data builder class. " + builderClazz);
                }
            }
            ProjectData projectData = buildProject(project, null, dataBuilders);
            try (ObjectOutputStream os = new ObjectOutputStream(Files.newOutputStream(properties.getOutputFile().toPath()))) {
                os.writeObject(projectData);
            }
        } catch (Throwable ex) {
            throw new RuntimeException("Fatal exception building model.", ex);
        }

        return new WorkspaceToolModel.Dummy();
    }

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(WorkspaceToolModel.class.getName());
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        throw new UnsupportedOperationException();
    }

    private static ProjectData buildProject(Project project, @Nullable ProjectData parent, List<DataBuilder> dataBuilders) {
        ProjectData projectData = new ProjectData(
                project.getName(),
                project.getProjectDir(),
                parent,
                String.valueOf(project.getVersion()),
                String.valueOf(project.getGroup()),
                String.valueOf(project.findProperty("archivesBaseName"))
        );
        projectData.data.put(ProjectData.class, buildPlugins(project, dataBuilders));

        for (DataBuilder builder : dataBuilders) {
            builder.buildProjectData(project, projectData);
        }

        for (Project subProj : project.getSubprojects()) {
            projectData.subprojects.put(subProj.getName(), buildProject(subProj, projectData, dataBuilders));
        }

        return projectData;
    }

    private static PluginData buildPlugins(Project project, List<DataBuilder> dataBuilders) {
        PluginContainer plugins = project.getPlugins();

        Map<String, String> pluginMap = getPluginMap(plugins);
        PluginData data = new PluginData();
        FastStream.of(plugins)
                .map(Plugin::getClass)
                .map(Class::getName)
                .forEach(cName -> {
                    data.plugins.put(pluginMap.getOrDefault(cName, cName), cName);
                });

        for (DataBuilder builder : dataBuilders) {
            builder.buildPluginData(project, data);
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

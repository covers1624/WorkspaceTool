package net.covers1624.wstool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.JdkProvider;
import net.covers1624.wstool.api.config.Config;
import net.covers1624.wstool.api.extension.Extension;
import net.covers1624.wstool.api.extension.Framework;
import net.covers1624.wstool.api.extension.Workspace;
import net.covers1624.wstool.api.module.Dependency;
import net.covers1624.wstool.api.module.Module;
import net.covers1624.wstool.api.module.SourceSet;
import net.covers1624.wstool.api.module.WorkspaceBuilder;
import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.api.data.*;
import net.covers1624.wstool.json.TypeFieldDeserializer;
import net.covers1624.wstool.module.ModuleUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 19/9/24.
 */
public class WorkspaceTool {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VERSION;

    static {
        String v = WorkspaceTool.class.getPackage().getImplementationVersion();
        if (v == null) {
            v = "dev";
        }
        VERSION = v;
    }

    public static void main(String[] args) throws IOException {
        Environment env = Environment.of();
        LOGGER.info("Starting WorkspaceTool v@{}", VERSION);
        LOGGER.info("  Project Directory: {}", env.projectRoot());
        List<Extension> extensions = loadExtensions();

        Path configFile = env.projectRoot().resolve("workspace.yml");
        if (Files.notExists(configFile)) {
            configFile = env.projectRoot().resolve("workspace.yaml");
        }
        if (Files.notExists(configFile)) {
            LOGGER.error("Expected workspace.yml or workspace.yaml in project directory.");
            return;
        }

        Config config = deserializeConfig(extensions, configFile);
        Workspace workspace = config.workspace();

        // TODO, currently only one framework supported.
        //       Ideally, Fabric will be a framework.
        if (config.frameworks().size() != 1) {
            throw new RuntimeException("Expected one framework, got: " + config.frameworks().size());
        }
        Framework framework = config.frameworks().get(0);

        List<Path> modulePaths = FastStream.of(config.modules())
                .flatMap(e -> {
                    try {
                        return ModuleUtils.expandModuleReference(env.projectRoot(), e);
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to expand module reference.", ex);
                    }
                })
                .filterNot(e -> e.getFileName().toString().equals("buildSrc"))
                .filter(e -> Files.exists(e.resolve("build.gradle")))
                .toList();
        LOGGER.info("Found {} modules.", modulePaths.size());

        JdkProvider jdkProvider = new JdkProvider(env);
        GradleModelExtractor modelExtractor = new GradleModelExtractor(env, jdkProvider, config.gradleHashables());

        WorkspaceBuilder builder = workspace.builder(env);
        for (Path modulePath : modulePaths) {
            LOGGER.info("Processing module {}", env.projectRoot().relativize(modulePath));
            ProjectData projectData = modelExtractor.extractProjectData(modulePath, Set.of());
            buildModule(builder, projectData);
        }

        framework.buildFrameworks(
                env,
                modelExtractor::extractProjectData,
                WorkspaceTool::buildModule,
                builder
        );

        LOGGER.info("Writing workspace..");
        builder.writeWorkspace();

        LOGGER.info("Done!");
    }

    private static Module buildModule(WorkspaceBuilder builder, ProjectData project) {
        Module module = builder.newModule(project.projectDir.toPath(), project.name);
        module.setProjectData(project);
        module.excludes().add(module.rootDir().resolve(".gradle"));

        // First build the tree of modules.
        Map<ProjectData, Module> moduleMap = new LinkedHashMap<>();
        Map<SourceSetData, SourceSet> sourceSetMap = new LinkedHashMap<>();
        buildModuleTree(moduleMap, sourceSetMap, project, module);

        // Then process them and insert dependencies. This must be done in a 2-pass system, as we need the
        // module tree to exist in order to make SourceSet/Module dependencies.
        moduleMap.forEach((p, m) -> {
            var sourceSets = p.getData(SourceSetList.class);
            if (sourceSets == null) return;

            var configurationData = p.getData(ConfigurationList.class);
            if (configurationData == null) return;
            var configurations = configurationData.asMap();

            for (SourceSetData data : sourceSets.asMap().values()) {
                SourceSet sourceSet = requireNonNull(sourceSetMap.get(data));
                // TODO, we can model the gradle data a bit different, perhaps with an enum key in a map,
                //       and we could collapse both of these.
                sourceSet.compileDependencies().addAll(
                        buildDependencies(
                                moduleMap,
                                sourceSetMap,
                                configurations.get(data.compileClasspathConfiguration).dependencies,
                                sourceSet
                        )
                );

                sourceSet.runtimeDependencies().addAll(
                        buildDependencies(
                                moduleMap,
                                sourceSetMap,
                                configurations.get(data.runtimeClasspathConfiguration).dependencies,
                                sourceSet
                        )
                );
            }

            // TODO, this is a workaround for Gradle extraction not properly extracting some sourceset deps.
            //       Specifically the `test` source set has its 'compileClasspath' and `runtimeClasspath` file collections
            //       appended with the main source set output, instead of it being placed on the associated configuration.
            //       Currently, we only pull the configuration data. We should instead pull the classpath data and match the
            //       source set output dirs to find the right links. This should more match what Intellij does.
            SourceSet mainSourceSet = m.sourceSets().get("main");
            SourceSet testSourceSet = m.sourceSets().get("test");
            if (mainSourceSet != null && testSourceSet != null) {
                testSourceSet.compileDependencies().add(new Dependency.SourceSetDependency(mainSourceSet));
                testSourceSet.runtimeDependencies().add(new Dependency.SourceSetDependency(mainSourceSet));
            }
        });
        return module;
    }

    private static Set<Dependency> buildDependencies(Map<ProjectData, Module> moduleMap, Map<SourceSetData, SourceSet> sourceSetMap, List<? extends ConfigurationData.Dependency> deps, SourceSet sourceSet) {
        // TODO this can likely be converted back to a List when/if we remove the recursive-ness of extracted Gradle dependencies.
        Set<Dependency> dependencies = new HashSet<>();
        for (ConfigurationData.Dependency dep : deps) {
            if (dep instanceof ConfigurationData.MavenDependency maven) {
                dependencies.add(new Dependency.MavenDependency(
                        maven.mavenNotation,
                        FastStream.of(maven.files.entrySet())
                                .toMap(
                                        Map.Entry::getKey,
                                        e -> e.getValue().toPath()
                                )
                ));
                dependencies.addAll(buildDependencies(moduleMap, sourceSetMap, maven.children, sourceSet));
            } else if (dep instanceof ConfigurationData.SourceSetDependency ss) {
                dependencies.add(new Dependency.SourceSetDependency(sourceSetMap.get(ss.sourceSet)));
            } else if (dep instanceof ConfigurationData.ProjectDependency proj) {
                dependencies.add(new Dependency.SourceSetDependency(moduleMap.get(proj.project).sourceSets().get("main")));
            } else {
                throw new RuntimeException("Unhandled dependency type: " + dep.getClass());
            }
        }

        return dependencies;
    }

    private static void buildModuleTree(Map<ProjectData, Module> moduleMap, Map<SourceSetData, SourceSet> sourceSetMap, ProjectData project, Module module) {
        module.excludes().add(module.rootDir().resolve("build"));
        module.excludes().add(module.rootDir().resolve("out"));

        moduleMap.put(project, module);
        var sourceSets = project.getData(SourceSetList.class);
        if (sourceSets != null) {
            for (SourceSetData data : sourceSets.asMap().values()) {
                SourceSet sourceSet = module.newSourceSet(data.name);
                sourceSetMap.put(data, sourceSet);

                sourceSet.sourcePaths().putAll(
                        FastStream.of(data.sourceMap.entrySet())
                                .toMap(
                                        Map.Entry::getKey,
                                        e -> FastStream.of(e.getValue()).map(File::toPath).toList()
                                )
                );
            }
        }

        var subProjects = project.getData(SubProjectList.class);
        if (subProjects != null) {
            for (ProjectData subProject : subProjects.asMap().values()) {
                buildModuleTree(
                        moduleMap,
                        sourceSetMap,
                        subProject,
                        module.newSubModule(subProject.projectDir.toPath(), subProject.name)
                );
            }
        }
    }

    private static List<Extension> loadExtensions() {
        LOGGER.info("Loading extensions...");
        List<Extension> extensions = new ArrayList<>();
        for (Extension extension : ServiceLoader.load(Extension.class)) {
            Extension.Details details = extension.getClass().getAnnotation(Extension.Details.class);
            if (details == null) {
                throw new RuntimeException("Extension " + extension.getClass().getName() + " requires @Extension.Details annotation.");
            }
            extensions.add(extension);
            LOGGER.info("  Loaded extension {}, {}", details.id(), details.desc());
        }
        LOGGER.info("Loaded {} extensions.", extensions.size());
        return extensions;
    }

    private static Config deserializeConfig(List<Extension> extensions, Path config) throws IOException {
        LOGGER.info("Loading config...");
        Gson gson = createDeserializer(extensions).create();
        try (Reader reader = new InputStreamReader(Files.newInputStream(config))) {
            return gson.fromJson(gson.toJson((Object) new Yaml().load(reader)), Config.class);
        }
    }

    private static @NotNull GsonBuilder createDeserializer(List<Extension> extensions) {
        GsonBuilder builder = new GsonBuilder();

        Map<String, Class<? extends Framework>> frameworkTypes = new HashMap<>();
        Map<String, Class<? extends Workspace>> workspaceTypes = new HashMap<>();
        builder.registerTypeAdapter(Framework.class, new TypeFieldDeserializer<>("framework", frameworkTypes));
        builder.registerTypeAdapter(Workspace.class, new TypeFieldDeserializer<>("workspace", workspaceTypes));

        Extension.ConfigTypeRegistry registry = new Extension.ConfigTypeRegistry() {

            @Override
            public void registerFramework(String key, Class<? extends Framework> type) {
                if (frameworkTypes.put(key, type) != null) throw new IllegalArgumentException("Type " + key + " already exists.");
            }

            @Override
            public void registerWorkspace(String key, Class<? extends Workspace> type) {
                if (workspaceTypes.put(key, type) != null) throw new IllegalArgumentException("Type " + key + " already exists.");
            }
        };

        for (Extension extension : extensions) {
            extension.registerConfigTypes(registry);
        }
        return builder;
    }
}

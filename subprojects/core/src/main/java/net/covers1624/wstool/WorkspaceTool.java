package net.covers1624.wstool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.quack.net.httpapi.okhttp.OkHttpEngine;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.JdkProvider;
import net.covers1624.wstool.api.ModuleProcessor;
import net.covers1624.wstool.api.config.Config;
import net.covers1624.wstool.api.config.RunConfigTemplate;
import net.covers1624.wstool.api.extension.Extension;
import net.covers1624.wstool.api.extension.FrameworkType;
import net.covers1624.wstool.api.extension.WorkspaceType;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.SourceSet;
import net.covers1624.wstool.api.workspace.Workspace;
import net.covers1624.wstool.api.workspace.runs.RunConfig;
import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.GradleTaskExecutor;
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
import java.util.function.Function;

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
        run(Environment.of());
    }

    public static void run(Environment env) throws IOException {
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
        WorkspaceType workspaceType = config.workspace();

        // TODO, currently only one framework supported.
        //       Ideally, Fabric will be a framework.
        if (config.frameworks().size() != 1) {
            throw new RuntimeException("Expected one framework, got: " + config.frameworks().size());
        }
        FrameworkType frameworkType = config.frameworks().getFirst();

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

        var http = OkHttpEngine.create();
        env.putService(HttpEngine.class, http);
        JdkProvider jdkProvider = new JdkProvider(env, http);
        env.putService(JdkProvider.class, jdkProvider);

        GradleModelExtractor modelExtractor = new GradleModelExtractor(env, jdkProvider, config.gradleHashables());
        GradleTaskExecutor gradleTaskExecutor = new GradleTaskExecutor(jdkProvider);

        env.putService(GradleTaskExecutor.class, gradleTaskExecutor);

        Workspace workspace = workspaceType.newWorkspace(env);
        ModuleProcessor moduleProcessor = new ModuleProcessor() {
            @Override
            public Module buildModule(Workspace workspace, Path project, Set<String> extraTasks) {
                var data = modelExtractor.extractProjectData(project, extraTasks);
                return WorkspaceTool.buildModule(workspace, data);
            }

            @Override
            public Set<Dependency> processConfiguration(Module rootModule, ConfigurationData configuration) {
                // TODO, we should probably recover the ProjectData -> Module and SourceSetData -> SourceSet links for this
                //       but it's not required for now.
                return buildDependencies(Map.of(), Map.of(), configuration.dependencies);
            }
        };
        env.putService(ModuleProcessor.class, moduleProcessor);

        for (Extension extension : extensions) {
            extension.prepareEnvironment(env);
        }

        LOGGER.info("Processing modules.");
        for (Path modulePath : modulePaths) {
            LOGGER.info("Processing module {}", env.projectRoot().relativize(modulePath));
            moduleProcessor.buildModule(workspace, modulePath, Set.of());
        }

        LOGGER.info("Discovering cross-project links.");
        insertCrossModuleLinks(workspace);

        LOGGER.info("Processing run templates.");
        processRunConfigTemplates(env, workspaceType, workspace);

        LOGGER.info("Setting up frameworks.");
        frameworkType.buildFrameworks(
                env,
                workspace
        );

        LOGGER.info("Writing workspace..");
        workspace.writeWorkspace();

        LOGGER.info("Done!");
    }

    private static Module buildModule(Workspace workspace, ProjectData project) {
        Module module = workspace.newModule(project.projectDir.toPath(), project.name);
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
                                configurations.get(data.compileClasspathConfiguration).dependencies
                        )
                );

                sourceSet.runtimeDependencies().addAll(
                        buildDependencies(
                                moduleMap,
                                sourceSetMap,
                                configurations.get(data.runtimeClasspathConfiguration).dependencies
                        )
                );
            }
        });
        return module;
    }

    private static Set<Dependency> buildDependencies(Map<ProjectData, Module> moduleMap, Map<SourceSetData, SourceSet> sourceSetMap, Set<? extends ConfigurationData.Dependency> deps) {
        // TODO this can likely be converted back to a List when/if we remove the recursive-ness of extracted Gradle dependencies.
        Set<Dependency> dependencies = new LinkedHashSet<>();
        for (ConfigurationData.Dependency dep : deps) {
            switch (dep) {
                case ConfigurationData.MavenDependency maven -> {
                    dependencies.add(new Dependency.MavenDependency(
                            maven.mavenNotation,
                            FastStream.of(maven.files.entrySet())
                                    .toMap(
                                            Map.Entry::getKey,
                                            e -> e.getValue().toPath()
                                    )
                    ));
                    dependencies.addAll(buildDependencies(moduleMap, sourceSetMap, maven.children));
                }
                case ConfigurationData.SourceSetDependency ss -> {
                    var sourceSet = sourceSetMap.get(ss.sourceSet);
                    if (sourceSet == null) {
                        throw new RuntimeException("Unknown SourceSet whilst processing dependencies. " + ss.sourceSet.name);
                    }
                    dependencies.add(new Dependency.SourceSetDependency(sourceSet));
                }
                case ConfigurationData.ProjectDependency proj -> {
                    var project = moduleMap.get(proj.project);
                    if (project == null) {
                        throw new RuntimeException("Unknown Project whilst processing dependencies. " + proj.project.name);
                    }
                    dependencies.add(new Dependency.SourceSetDependency(project.sourceSets().get("main")));
                }
                default -> throw new RuntimeException("Unhandled dependency type: " + dep.getClass());
            }
        }

        return dependencies;
    }

    private static void buildModuleTree(Map<ProjectData, Module> moduleMap, Map<SourceSetData, SourceSet> sourceSetMap, ProjectData project, Module module) {
        module.setProjectData(project);
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

    // TODO We should pull Gradle publishing data to provide information here, archivesBaseName may be wrong.
    // TODO, We may want to do this _somehow_ when building the module tree (perhaps extract all then process?),
    //       as we currently don't trim transitive dependencies from the removed dependency.
    //       For example: in the case of CCL, we don't trim the transitive Quack dependency.
    private static void insertCrossModuleLinks(Workspace workspace) {
        Map<MavenNotation, SourceSet> moduleLookup = new HashMap<>();
        workspace.allProjectModules().forEach(module -> {
            var projData = module.projectData();
            if (projData == null) return;

            // TODO, I'm not sure if we can properly trace which source set a jar is composed from in Gradle. It would be a lot
            //       of heuristics for tracing various Gradle objects around through task I/O. So for now we just pick main.
            moduleLookup.put(MavenNotation.parse(projData.group + ":" + projData.archivesBaseName), module.sourceSets().get("main"));
        });

        workspace.allProjectModules().forEach(module -> {
            for (SourceSet sourceSet : module.sourceSets().values()) {
                insertCrossModuleLinks(sourceSet.compileDependencies(), moduleLookup);
                insertCrossModuleLinks(sourceSet.runtimeDependencies(), moduleLookup);
            }
        });
    }

    private static void insertCrossModuleLinks(List<Dependency> dependencies, Map<MavenNotation, SourceSet> depLookup) {
        for (int i = 0; i < dependencies.size(); i++) {
            if (dependencies.get(i) instanceof Dependency.MavenDependency(MavenNotation notation, Map<String, Path> files)) {
                // TODO, ideally if we pull publishing metadata, we only need to nuke version.
                var found = depLookup.get(notation.withVersion("").withClassifier("").withExtension("jar"));
                if (found != null) {
                    dependencies.set(i, new Dependency.SourceSetDependency(found));
                }
            }
        }
    }

    private static void processRunConfigTemplates(Environment env, WorkspaceType workspaceType, Workspace workspace) {
        var runTemplates = workspaceType.runs();
        Map<String, RunConfigTemplate> templates = FastStream.of(runTemplates)
                .filter(e -> e.templateName() != null)
                .toMap(RunConfigTemplate::templateName, Function.identity());
        for (RunConfigTemplate config : runTemplates) {
            if (config.name() == null) continue;

            var runConfig = workspace.newRunConfig(config.name());
            for (String from : config.from()) {
                RunConfigTemplate template = templates.get(from);
                if (template == null) {
                    throw new RuntimeException("Failed to process run config " + config.name() + " template " + from + " does not exist.");
                }

                applyRun(env, template, runConfig);
            }
            applyRun(env, config, runConfig);
        }
    }

    private static void applyRun(Environment env, RunConfigTemplate config, RunConfig run) {
        run.config().putAll(config.config());

        var runDir = config.runDir();
        if (runDir != null) {
            run.runDir(env.projectRoot().resolve(runDir));
        }

        var mainClass = config.mainClass();
        if (mainClass != null) {
            run.mainClass(mainClass);
        }

        run.args().addAll(config.args());
        run.vmArgs().addAll(config.vmArgs());
        run.sysProps().putAll(config.sysProps());
        run.envVars().putAll(config.env());
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

        Map<String, Class<? extends FrameworkType>> frameworkTypes = new HashMap<>();
        Map<String, Class<? extends WorkspaceType>> workspaceTypes = new HashMap<>();
        builder.registerTypeAdapter(FrameworkType.class, new TypeFieldDeserializer<>("framework", frameworkTypes));
        builder.registerTypeAdapter(WorkspaceType.class, new TypeFieldDeserializer<>("workspace", workspaceTypes));

        Extension.ConfigTypeRegistry registry = new Extension.ConfigTypeRegistry() {

            @Override
            public void registerFrameworkType(String key, Class<? extends FrameworkType> type) {
                if (frameworkTypes.put(key, type) != null) throw new IllegalArgumentException("Type " + key + " already exists.");
            }

            @Override
            public void registerWorkspaceType(String key, Class<? extends WorkspaceType> type) {
                if (workspaceTypes.put(key, type) != null) throw new IllegalArgumentException("Type " + key + " already exists.");
            }
        };

        for (Extension extension : extensions) {
            extension.registerConfigTypes(registry);
        }
        return builder;
    }
}

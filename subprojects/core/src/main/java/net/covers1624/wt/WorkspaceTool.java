/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt;

import com.google.common.collect.ImmutableMap;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.covers1624.jdkutils.AdoptiumProvisioner;
import net.covers1624.jdkutils.JavaLocator;
import net.covers1624.jdkutils.JdkInstallationManager;
import net.covers1624.quack.logging.log4j2.Log4jUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.net.apache.ApacheHttpClientDownloadAction;
import net.covers1624.quack.util.SneakyUtils;
import net.covers1624.tconsole.api.TailGroup;
import net.covers1624.tconsole.log4j.TailConsoleAppender;
import net.covers1624.wt.api.Extension;
import net.covers1624.wt.api.ExtensionDetails;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.dependency.ScalaSdkDependency;
import net.covers1624.wt.api.framework.FrameworkHandler;
import net.covers1624.wt.api.gradle.data.*;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.gradle.model.impl.WorkspaceToolModelImpl;
import net.covers1624.wt.api.impl.dependency.DependencyLibraryImpl;
import net.covers1624.wt.api.impl.dependency.ScalaSdkDependencyImpl;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.mixin.DefaultMixinInstantiator;
import net.covers1624.wt.api.impl.module.ModuleImpl;
import net.covers1624.wt.api.impl.script.AbstractWorkspaceScript;
import net.covers1624.wt.api.impl.script.FrameworkRegistryImpl;
import net.covers1624.wt.api.impl.script.runconfig.DefaultRunConfig;
import net.covers1624.wt.api.impl.workspace.WorkspaceRegistryImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.api.script.ModdingFramework;
import net.covers1624.wt.api.script.NullFramework;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.script.runconfig.RunConfig;
import net.covers1624.wt.api.workspace.WorkspaceHandler;
import net.covers1624.wt.api.workspace.WorkspaceWriter;
import net.covers1624.wt.event.*;
import net.covers1624.wt.gradle.GradleManagerImpl;
import net.covers1624.wt.gradle.GradleModelCacheImpl;
import net.covers1624.wt.util.DependencyAggregator;
import net.covers1624.wt.util.OverallProgressTail;
import net.covers1624.wt.util.ScalaVersion;
import net.covers1624.wt.util.SimpleServiceLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.stream.StreamSupport.stream;
import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Created by covers1624 on 13/05/19.
 */
public class WorkspaceTool {

    public static final String VERSION = "dev";
    private static final Logger LOGGER = LogManager.getLogger("WorkspaceTool");

    public static final Path SYSTEM_WT_FOLDER = Paths.get(System.getProperty("user.home"), ".workspace_tool")
            .normalize().toAbsolutePath();
    public static final Path WT_JDKS = SYSTEM_WT_FOLDER.resolve("jdks");

    private final List<Extension> extensions = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        WorkspaceTool instance = new WorkspaceTool();
        instance.run(args);
    }

    private void run(String[] args) throws Exception {
        WorkspaceToolContext context = new WorkspaceToolContext();
        context.console = TailConsoleAppender.getTailConsole();
        Runtime.getRuntime().addShutdownHook(new Thread(context.console::shutdown));
        Log4jUtils.redirectStreams();

        context.projectDir = Paths.get(".").normalize().toAbsolutePath();
        context.cacheDir = context.projectDir.resolve(".workspace_tool");

        Path workspaceScript = context.projectDir.resolve("workspace.groovy");
        if (Files.notExists(workspaceScript)) {
            LOGGER.error("'workspace.groovy' does not exist in the project directory. {}", context.projectDir);
            System.exit(1);
        }

        context.console.setRefreshRate(15, TimeUnit.MILLISECONDS);
        TailGroup mainGroup = context.console.newGroup();
        mainGroup.add(new OverallProgressTail());

        LOGGER.info("WorkspaceTool@{}", VERSION);
        LOGGER.info(" Project Dir:      {}", context.projectDir.toAbsolutePath());
        LOGGER.info(" Workspace Script: {}", workspaceScript.toAbsolutePath());

        LOGGER.info("Loading Extensions..");
        SimpleServiceLoader<Extension> extensionLoader = new SimpleServiceLoader<>(Extension.class);
        extensionLoader.poll();
        for (Class<? extends Extension> clazz : extensionLoader.getNewServices()) {
            ExtensionDetails details = clazz.getAnnotation(ExtensionDetails.class);
            if (details == null) {
                throw new RuntimeException("Extension class '" + clazz.getName() + "' is missing @ExtensionDetails annotation.");
            }
            LOGGER.info(" Loading Extension: {}: {}", details.name(), details.desc());
            Extension extension = clazz.getConstructor().newInstance();
            extensions.add(extension);
            extension.load();
        }

        LOGGER.info("Initializing internal systems..");
        JavaLocator javaLocator = JavaLocator.builder()
                .ignoreOpenJ9()
                .findIntellijJdks()
                .findGradleJdks()
                .build();
        context.javaInstalls = javaLocator.findJavaVersions();
        context.jdkManager = new JdkInstallationManager(WT_JDKS, new AdoptiumProvisioner(ApacheHttpClientDownloadAction::new), false);
        context.frameworkRegistry = new FrameworkRegistryImpl();
        context.gradleManager = new GradleManagerImpl();
        context.modelCache = new GradleModelCacheImpl(context);
        context.workspaceRegistry = new WorkspaceRegistryImpl();
        InitializationEvent.REGISTRY.fireEvent(new InitializationEvent(context));

        ModuleHashCheckEvent.REGISTRY.register(this::onModuleHashCheck);
        ProcessDependencyEvent.REGISTRY.register(Event.Priority.FIRST, this::onProcessDependency);

        context.mixinInstantiator = new DefaultMixinInstantiator();
        context.mixinInstantiator.addMixinTarget(RunConfig.class, DefaultRunConfig.class);

        // WT-Gradle module
        context.gradleManager.includeClassMarker("net.covers1624.wt.gradle.WorkspaceToolGradlePlugin");
        // API
        context.gradleManager.includeClassMarker(ProjectData.class);
        // Guava
        context.gradleManager.includeClassMarker(ImmutableMap.class);
        // Lang3
        context.gradleManager.includeClassMarker(StringUtils.class);
        // Commons text
        context.gradleManager.includeClassMarker(StringSubstitutor.class);
        // Quack
        context.gradleManager.includeClassMarker(SneakyUtils.class);

        context.gradleManager.includeResourceMarker("gradle_plugin.marker");

        context.frameworkRegistry.registerScriptImpl(NullFramework.class, NullFramework::new);
        context.frameworkRegistry.registerFrameworkHandler(NullFramework.class, FrameworkHandler.NullFrameworkHandler::new);

        LOGGER.info("Preparing script..");
        Binding binding = new Binding();
        binding.setProperty(AbstractWorkspaceScript.FR_PROP, context.frameworkRegistry);
        binding.setProperty(AbstractWorkspaceScript.WR_PROP, context.workspaceRegistry);
        binding.setProperty(AbstractWorkspaceScript.MI_PROP, context.mixinInstantiator);
        context.workspaceScript = runScript(binding, workspaceScript);

        if (context.workspaceScript.getFramework() == null) {
            LOGGER.error("No framework specified in script.");
        }
        if (context.workspaceScript.getWorkspace() == null) {
            LOGGER.error("No workspace specified in script.");
        }

        DependencyAggregator dependencyAggregator = new DependencyAggregator(context);
        context.dependencyLibrary = new DependencyLibraryImpl();

        LOGGER.info("Constructing module representation..");
        ModuleContainerSpec moduleContainer = context.workspaceScript.getModuleContainer();
        List<Path> includes = moduleContainer.getIncludes().stream()
                .flatMap(e -> expandInclude(context.projectDir, e))
                .filter(e -> !e.getFileName().toString().equals("buildSrc"))
                .filter(e -> Files.exists(e.resolve("build.gradle")))
                .collect(Collectors.toList());
        for (Path candidate : includes) {
            Path rel = context.projectDir.relativize(candidate);
            // TODO, split on last slash.
            String group = rel.toString().replace("\\", "/").replace(candidate.getFileName().toString(), "")
                    .replaceAll("//", "");
            WorkspaceToolModel model = context.modelCache.getModel(candidate, emptySet(), emptySet());
            context.modules.addAll(ModuleImpl.makeGradleModules(group, model.getProjectData(), context));
        }

        LOGGER.info("Constructing Framework modules..");
        FrameworkHandler<?> frameworkHandler = context.frameworkRegistry.getFrameworkHandler(context.workspaceScript.getFrameworkClass(), context);
        ModdingFramework framework = context.workspaceScript.getFramework();
        frameworkHandler.constructFrameworkModules(unsafeCast(framework));

        LOGGER.info("Processing modules..");

        Iterable<Module> allModules = context.getAllModules();
        //Attempt to build a ScalaSdkDependency from the modules 'main' SourceSet.
        allModules.forEach(module -> {
            SourceSet sourceSet = module.getSourceSets().get("main");
            if (sourceSet == null) return;

            ScalaSdkDependency sdkCandidate = new ScalaSdkDependencyImpl();
            Configuration config = sourceSet.getCompileConfiguration();
            config.getAllDependencies()
                    .stream()
                    .filter(e -> e instanceof MavenDependency).map(e -> (MavenDependency) e)
                    .forEach(dep -> {
                        MavenNotation notation = dep.getNotation();
                        if (notation.group.startsWith("org.scala-lang")) {
                            if (notation.module.equals("scala-compiler")) {
                                sdkCandidate.setScalac(dep);
                            }
                            sdkCandidate.addLibrary(dep);

                        }
                    });
            if (sdkCandidate.getScalac() != null) {
                sdkCandidate.setVersion(sdkCandidate.getScalac().getNotation().version);
                ScalaVersion scalaVersion = ScalaVersion.findByVersion(sdkCandidate.getVersion())
                        .orElseThrow(() -> new RuntimeException("Unknown scala version: " + sdkCandidate.getVersion()));
                sdkCandidate.setScalaVersion(scalaVersion);
                config.addDependency(sdkCandidate);
                //Attempt to nuke.
                config.streamAll().forEach(e -> sdkCandidate.getClasspath().forEach(e.getDependencies()::remove));
            }
        });
        EarlyProcessModulesEvent.REGISTRY.fireEvent(new EarlyProcessModulesEvent(context));

        // Run a pass of dependency processing _before_ running dependency aggregation.
        context.modules.forEach(module -> {
            for (Configuration config : module.getConfigurations().values()) {
                config.setDependencies(config.getDependencies().stream()
                        .map(e -> {
                            EarlyProcessDependencyEvent event = new EarlyProcessDependencyEvent(context, module, config, config, e);
                            EarlyProcessDependencyEvent.REGISTRY.fireEvent(event);
                            return event.getResult();
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                );
            }
        });

        //Resolve all dependencies, and remove duplicates.
        allModules.forEach(dependencyAggregator::consume);
        allModules.forEach(module -> {
            module.getConfigurations().values()
                    .forEach(config -> {
                        config.setDependencies(config.getDependencies().stream()
                                .map(e -> {
                                    if (e instanceof MavenDependency dep) {
                                        return dependencyAggregator.resolve(dep.getNotation());
                                    }
                                    if (e instanceof ScalaSdkDependency dep) {
                                        return dependencyAggregator.resolveScala(dep.getScalaVersion());
                                    }
                                    return e;
                                })
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                        );
                    });
        });

        //Replace maven dependencies with module dependencies.
        context.modules.forEach(module -> {
            for (Configuration config : module.getConfigurations().values()) {
                config.setDependencies(config.getDependencies().stream()
                        .map(e -> {
                            ProcessDependencyEvent event = new ProcessDependencyEvent(context, module, config, config, e);
                            ProcessDependencyEvent.REGISTRY.fireEvent(event);
                            return event.getResult();
                        })
                        // Strip any dependencies which don't exist or are empty.
                        .filter(e -> {
                            if (e instanceof MavenDependency d) {
                                if (Files.notExists(d.getClasses())) return false;
                                try {
                                    if (Files.size(d.getClasses()) == 0) return false;
                                } catch (IOException ex) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                );
            }
            SourceSet testSS = module.getSourceSets().get("test");
            if (testSS != null) {
                testSS.getCompileConfiguration().addDependency(new SourceSetDependencyImpl()
                        .setModule(module)
                        .setSourceSet("main")
                );
            }
            SourceSet apiSS = module.getSourceSets().get("api");
            if (apiSS != null) {
                module.getSourceSets().get("main").getCompileConfiguration().addDependency(
                        new SourceSetDependencyImpl().setModule(module).setSourceSet("api"));
            }
        });

        // add run directories as excluded paths for every module
        context.workspaceScript.getWorkspace().getRunConfigContainer().getRunConfigs().values().stream()
                .map(RunConfig::getRunDir)
                .forEach(path ->
                        allModules.forEach(module ->
                                module.addExclude(path)
                        )
                );

        ProcessModulesEvent.REGISTRY.fireEvent(new ProcessModulesEvent(context));

        allModules.forEach(context.dependencyLibrary::consume);

        WorkspaceHandler<?> workspaceHandler = context.workspaceRegistry.constructWorkspaceHandlerImpl(context.workspaceScript.getWorkspaceType());
        workspaceHandler.buildWorkspaceModules(unsafeCast(context.workspaceScript.getWorkspace()), context);

        ProcessWorkspaceModulesEvent.REGISTRY.fireEvent(new ProcessWorkspaceModulesEvent(context));

        LOGGER.info("Writing workspace..");
        WorkspaceWriter<?> workspaceWriter = context.workspaceRegistry.getWorkspaceWriter(context.workspaceScript.getWorkspaceType(), context);
        workspaceWriter.write(unsafeCast(context.workspaceScript.getWorkspace()));

        for (RunConfig value : context.workspaceScript.getWorkspace().getRunConfigContainer().getRunConfigs().values()) {
            Files.createDirectories(value.getRunDir());
        }

        LOGGER.info("Done!");
        System.exit(0);
    }

    private static Stream<Path> expandInclude(Path base, String include) {
        if (include.startsWith("/")) {
            throw new IllegalArgumentException("Include must not start with a slash. ' " + include + "'");
        }
        if (include.endsWith("**")) {
            include = include.substring(0, include.length() - 2);
            int lastSlash = include.lastIndexOf("/");
            String prefix;
            Path expandFolder;
            // We have a 'Some**' include
            if (lastSlash == -1) {
                prefix = include;
                expandFolder = base;
            } else {
                String preSlash = include.substring(0, lastSlash);
                prefix = include.substring(lastSlash + 1);
                expandFolder = base.resolve(preSlash);
            }
            if (Files.notExists(expandFolder)) {
                return Stream.empty();
            }
            return SneakyUtils.sneaky(() -> Files.list(expandFolder))
                    .filter(e -> prefix.isEmpty() || e.getFileName().toString().startsWith(prefix));
        }
        return Stream.of(base.resolve(include));
    }

    private AbstractWorkspaceScript runScript(Binding binding, Path scriptFile) throws IOException {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.setScriptBaseClass(AbstractWorkspaceScript.class.getName());
        configuration.getOptimizationOptions().put(CompilerConfiguration.INVOKEDYNAMIC, true);
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addImports(
                "net.covers1624.wt.util.JavaVersion"
        );
        importCustomizer.addStarImports(
                "net.covers1624.wt.api.script",
                "net.covers1624.wt.api.script.module",
                "net.covers1624.wt.api.script.runconfig"
        );
        configuration.addCompilationCustomizers(importCustomizer);
        PrepareScriptEvent.REGISTRY.fireEvent(new PrepareScriptEvent(binding, scriptFile, configuration));
        GroovyShell shell = new GroovyShell(binding, configuration);
        LOGGER.info("Compiling script..");
        AbstractWorkspaceScript script = (AbstractWorkspaceScript) shell.parse(scriptFile.toFile());
        LOGGER.info("Executing script..");
        script.run();
        return script;
    }

    private void onModuleHashCheck(ModuleHashCheckEvent event) {
        event.putVersionedClass(ConfigurationData.class);
        event.putVersionedClass(ConfigurationData.Dependency.class);
        event.putVersionedClass(ConfigurationData.MavenDependency.class);
        event.putVersionedClass(ConfigurationData.SourceSetDependency.class);
        event.putVersionedClass(ExtraData.class);
        event.putVersionedClass(ExtraDataExtensible.class);
        event.putVersionedClass(ProjectData.class);
        event.putVersionedClass(PluginData.class);
        event.putVersionedClass(SourceSetData.class);
        event.putVersionedClass(WorkspaceToolModel.class);
        event.putVersionedClass(WorkspaceToolModelImpl.class);
        event.putClassBytes("net.covers1624.wt.gradle.WorkspaceToolGradlePlugin");
        event.putClassBytes("net.covers1624.wt.gradle.builder.AbstractModelBuilder");
        event.putClassBytes("net.covers1624.wt.gradle.builder.ExtraDataBuilder");
        event.putClassBytes("net.covers1624.wt.gradle.builder.WorkspaceToolModelBuilder");
    }

    private void onProcessDependency(ProcessDependencyEvent event) {
        Module module = event.getModule();
        Dependency dependency = event.getDependency();
        if (dependency instanceof MavenDependency mavenDep) {
            MavenNotation notation = mavenDep.getNotation();
            Optional<Module> matchingModule = event.getContext().modules.stream()
                    .filter(e -> e instanceof GradleBackedModule)
                    .map(e -> (GradleBackedModule) e)
                    .filter(e -> e != module)
                    .filter(e -> e.getProjectData().group.equals(notation.group))
                    .filter(e -> e.getProjectData().archivesBaseName.equals(notation.module))
                    .map(e -> (Module) e)
                    .findFirst();
            if (matchingModule.isPresent()) {
                Module otherModule = matchingModule.get();
                event.setResult(new SourceSetDependencyImpl(otherModule, "main"));
            }
        }
    }
}

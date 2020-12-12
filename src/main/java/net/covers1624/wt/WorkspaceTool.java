package net.covers1624.wt;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.covers1624.tconsole.api.TailGroup;
import net.covers1624.tconsole.log4j.Log4jUtils;
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
import net.covers1624.wt.api.impl.script.module.ModuleSpecImpl;
import net.covers1624.wt.api.impl.script.runconfig.DefaultRunConfig;
import net.covers1624.wt.api.impl.workspace.WorkspaceRegistryImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.api.script.ModdingFramework;
import net.covers1624.wt.api.script.NullFramework;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.script.module.ModuleSpec;
import net.covers1624.wt.api.script.runconfig.RunConfig;
import net.covers1624.wt.api.workspace.WorkspaceHandler;
import net.covers1624.wt.api.workspace.WorkspaceWriter;
import net.covers1624.wt.event.*;
import net.covers1624.wt.gradle.GradleManagerImpl;
import net.covers1624.wt.gradle.GradleModelCacheImpl;
import net.covers1624.wt.util.OverallProgressTail;
import net.covers1624.wt.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;
import static net.covers1624.wt.util.Utils.unsafeCast;

/**
 * Created by covers1624 on 13/05/19.
 */
public class WorkspaceTool {

    public static final String VERSION = "dev";
    private static final Logger logger = LogManager.getLogger("WorkspaceTool");

    //    private WTClassLoader classLoader;
    private List<Extension> extensions = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        //        ClassLoader classLoader = WorkspaceTool.class.getClassLoader();
        //        if (!(classLoader instanceof WTClassLoader)) {
        //            throw new RuntimeException("WorkspaceTool was loaded with the incorrect ClassLoader.");
        //        }
        WorkspaceTool instance = new WorkspaceTool();
        //instance.classLoader = (WTClassLoader) classLoader;
        instance.run(args);
    }

    private void run(String[] args) throws Exception {
        WorkspaceToolContext context = new WorkspaceToolContext();
        context.console = TailConsoleAppender.getTailConsole();
        Log4jUtils.redirectStreams();

        context.projectDir = new File("").toPath().normalize().toAbsolutePath();
        context.cacheDir = context.projectDir.resolve(".workspace_tool");

        Path workspaceScript = context.projectDir.resolve("workspace.groovy");
        if (Files.notExists(workspaceScript)) {
            logger.error("'workspace.groovy' does not exist in the project directory. {}", context.projectDir);
            System.exit(1);
        }

        context.console.setRefreshRate(15, TimeUnit.MILLISECONDS);
        TailGroup mainGroup = context.console.newGroup();
        mainGroup.add(new OverallProgressTail());

        logger.info("WorkspaceTool@{}", VERSION);
        logger.info(" Project Dir:      {}", context.projectDir.toAbsolutePath());
        logger.info(" Workspace Script: {}", workspaceScript.toAbsolutePath());

        //        logger.info("Evaluating script for scriptDeps() block..");
        //        try {
        //            Binding preBinding = new Binding();
        //            runScript(preBinding, workspaceScript, true);
        //        } catch (AbstractWorkspaceScript.AbortScriptException e) {
        //            logger.info("Found scriptDeps() block..");
        //        }

        logger.info("Loading Extensions..");
        SimpleServiceLoader<Extension> extensionLoader = new SimpleServiceLoader<>(Extension.class);
        extensionLoader.poll();
        for (Class<? extends Extension> clazz : extensionLoader.getNewServices()) {
            ExtensionDetails details = clazz.getAnnotation(ExtensionDetails.class);
            if (details == null) {
                throw new RuntimeException(ParameterFormatter.format("Missing {} is missing @ExtensionDetails.", clazz.getName()));
            }
            logger.info(" Loading Extension: {}: {}", details.name(), details.desc());
            Extension extension = clazz.newInstance();
            extensions.add(extension);
            extension.load();
        }

        logger.info("Initializing internal systems..");
        context.frameworkRegistry = new FrameworkRegistryImpl();
        context.gradleManager = new GradleManagerImpl();
        context.modelCache = new GradleModelCacheImpl(context);
        context.workspaceRegistry = new WorkspaceRegistryImpl();
        InitializationEvent.REGISTRY.fireEvent(new InitializationEvent(context));

        ModuleHashCheckEvent.REGISTRY.register(this::onModuleHashCheck);
        ProcessDependencyEvent.REGISTRY.register(Event.Priority.FIRST, this::onProcessDependency);

        context.mixinInstantiator = new DefaultMixinInstantiator();
        context.mixinInstantiator.addMixinTarget(RunConfig.class, DefaultRunConfig.class);
        context.mixinInstantiator.addMixinTarget(ModuleSpec.class, ModuleSpecImpl.class);

        context.gradleManager.includeClassMarker("net.covers1624.wt.gradle.WorkspaceToolGradlePlugin");
        context.gradleManager.includeClassMarker("net.covers1624.gradlestuff.sourceset.SourceSetDependencyPlugin");
        context.gradleManager.includeClassMarker(ProjectData.class);
        context.gradleManager.includeClassMarker(ImmutableMap.class);
        context.gradleManager.includeClassMarker(Gson.class);
        context.gradleManager.includeClassMarker(StringUtils.class);
        context.gradleManager.includeClassMarker(StringSubstitutor.class);
        context.gradleManager.includeClassMarker(MoreObjects.class);

        context.gradleManager.includeResourceMarker("gradle_plugin.marker");

        context.frameworkRegistry.registerScriptImpl(NullFramework.class, NullFramework::new);
        context.frameworkRegistry.registerFrameworkHandler(NullFramework.class, FrameworkHandler.NullFrameworkHandler::new);

        logger.info("Preparing script..");
        Binding binding = new Binding();
        binding.setProperty(AbstractWorkspaceScript.FR_PROP, context.frameworkRegistry);
        binding.setProperty(AbstractWorkspaceScript.WR_PROP, context.workspaceRegistry);
        binding.setProperty(AbstractWorkspaceScript.MI_PROP, context.mixinInstantiator);
        context.workspaceScript = runScript(binding, workspaceScript, false);

        if (context.workspaceScript.getFramework() == null) {
            logger.error("No framework specified in script.");
        }
        if (context.workspaceScript.getWorkspace() == null) {
            logger.error("No workspace specified in script.");
        }

        DependencyAggregator dependencyAggregator = new DependencyAggregator(context);
        context.dependencyLibrary = new DependencyLibraryImpl();

        logger.info("Constructing module representation..");
        List<Path> candidates = Files.walk(context.projectDir)//
                .parallel()//
                .filter(p -> {
                    if (Files.isDirectory(p) && !p.toString().endsWith("buildSrc")) {
                        Path buildGradle = p.resolve("build.gradle");
                        return Files.isRegularFile(buildGradle) && Files.exists(buildGradle);
                    }
                    return false;
                })//
                .collect(Collectors.toList());

        ModuleContainerSpec moduleContainer = context.workspaceScript.getModuleContainer();
        if (!moduleContainer.getIncludes().isEmpty()) {
            Predicate<Path> matcher = moduleContainer.createMatcher();
            for (Path candidate : candidates) {
                Path rel = context.projectDir.relativize(candidate);
                if (matcher.test(rel)) {
                    context.modules.add(ModuleImpl.makeGradleModule(rel.toString(), candidate, context));
                }
            }
        }

        logger.info("Constructing Framework modules..");
        FrameworkHandler<?> frameworkHandler = context.frameworkRegistry.getFrameworkHandler(context.workspaceScript.getFrameworkClass(), context);
        ModdingFramework framework = context.workspaceScript.getFramework();
        frameworkHandler.constructFrameworkModules(unsafeCast(framework));

        logger.info("Processing modules..");

        Iterable<Module> allModules = context.getAllModules();
        //Attempt to build a ScalaSdkDependency from the modules 'main' SourceSet.
        allModules.forEach(module -> {
            ScalaSdkDependency sdkCandidate = new ScalaSdkDependencyImpl();
            Configuration config = module.getSourceSets().get("main").getCompileConfiguration();
            config.getAllDependencies()//
                    .stream()//
                    .filter(e -> e instanceof MavenDependency).map(e -> (MavenDependency) e)//
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
                ScalaVersion scalaVersion = ScalaVersion.findByVersion(sdkCandidate.getVersion())//
                        .orElseThrow(() -> new RuntimeException("Unknown scala version: " + sdkCandidate.getVersion()));
                sdkCandidate.setScalaVersion(scalaVersion);
                config.addDependency(sdkCandidate);
                //Attempt to nuke. boom.exe
                config.walkHierarchy(e -> sdkCandidate.getClasspath().forEach(e.getDependencies()::remove));
            }
        });

        //Resolve all dependencies, and remove duplicates.
        allModules.forEach(dependencyAggregator::consume);
        stream(allModules.spliterator(), true)//
                .forEach(module -> {
                    module.getConfigurations().values()//
                            .parallelStream()//
                            .forEach(config -> {
                                config.setDependencies(config.getDependencies().stream()//
                                        .map(e -> {
                                            if (e instanceof MavenDependency) {
                                                return dependencyAggregator.resolve(((MavenDependency) e).getNotation());
                                            } else if (e instanceof ScalaSdkDependency) {
                                                return dependencyAggregator.resolveScala(((ScalaSdkDependency) e).getScalaVersion());
                                            }
                                            return e;
                                        })//
                                        .collect(Collectors.toSet())//
                                );
                            });
                });

        //Replace maven dependencies with module dependencies.
        context.modules.forEach(module -> {
            for (Configuration config : module.getConfigurations().values()) {
                config.setDependencies(config.getDependencies().stream()//
                        .map(e -> {
                            ProcessDependencyEvent event = new ProcessDependencyEvent(context, module, config, config, e);
                            ProcessDependencyEvent.REGISTRY.fireEvent(event);
                            return event.getResult();
                        })//
                        .filter(Objects::nonNull)//
                        .collect(Collectors.toSet())//
                );
            }
            SourceSet testSS = module.getSourceSets().get("test");
            if (testSS != null) {
                testSS.getCompileConfiguration().addDependency(new SourceSetDependencyImpl()//
                        .setModule(module)//
                        .setSourceSet("main")//
                );
            }
            SourceSet apiSS = module.getSourceSets().get("api");
            if (apiSS != null) {
                module.getSourceSets().get("main").getCompileConfiguration().addDependency(//
                        new SourceSetDependencyImpl().setModule(module).setSourceSet("api"));
            }
        });

        ProcessModulesEvent.REGISTRY.fireEvent(new ProcessModulesEvent(context));

        stream(allModules.spliterator(), true).forEach(context.dependencyLibrary::consume);

        WorkspaceHandler<?> workspaceHandler = context.workspaceRegistry.constructWorkspaceHandlerImpl(context.workspaceScript.getWorkspaceType());
        workspaceHandler.buildWorkspaceModules(unsafeCast(context.workspaceScript.getWorkspace()), context);

        ProcessWorkspaceModulesEvent.REGISTRY.fireEvent(new ProcessWorkspaceModulesEvent(context));

        logger.info("Writing workspace..");
        WorkspaceWriter<?> workspaceWriter = context.workspaceRegistry.getWorkspaceWriter(context.workspaceScript.getWorkspaceType(), context);
        workspaceWriter.write(unsafeCast(context.workspaceScript.getWorkspace()));

        //        logger.info("Constructed {} modules.", context.modules.size());
        //        context.modules.sort(Comparator.comparing(Module::getGroup).thenComparing(Module::getName));
        //        List<List<String>> rows = new ArrayList<>();
        //        rows.add(Arrays.asList("Group:", "Name:", "Path:"));
        //        for (Module module : context.modules) {
        //            rows.add(Arrays.asList(module.getGroup(), module.getName(), module.getPath().toString()));
        //        }
        //        ColFormatter.format(rows).forEach(e -> logger.info(" " + e));
    }

    private AbstractWorkspaceScript runScript(Binding binding, Path scriptFile, boolean firstPass) throws IOException {
        binding.setProperty("firstPass", firstPass);
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.setScriptBaseClass(AbstractWorkspaceScript.class.getName());
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports(//
                "net.covers1624.wt.api.script",//
                "net.covers1624.wt.api.script.module",//
                "net.covers1624.wt.api.script.runconfig"//
        );
        configuration.addCompilationCustomizers(importCustomizer);
        PrepareScriptEvent.REGISTRY.fireEvent(new PrepareScriptEvent(binding, scriptFile, configuration));
        GroovyShell shell = new GroovyShell(binding, configuration);
        logger.info("Compiling script..");
        AbstractWorkspaceScript script = (AbstractWorkspaceScript) shell.parse(scriptFile.toFile());
        logger.info("Executing script..");
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
        event.putVersionedClass("net.covers1624.wt.gradle.WorkspaceToolGradlePlugin");
        event.putVersionedClass("net.covers1624.wt.gradle.builder.AbstractModelBuilder");
        event.putVersionedClass("net.covers1624.wt.gradle.builder.ExtraDataBuilder");
        event.putVersionedClass("net.covers1624.wt.gradle.builder.WorkspaceToolModelBuilder");
    }

    private void onProcessDependency(ProcessDependencyEvent event) {
        Module module = event.getModule();
        Dependency dependency = event.getDependency();
        if (dependency instanceof MavenDependency) {
            MavenDependency mavenDep = (MavenDependency) dependency;
            MavenNotation notation = mavenDep.getNotation();
            Optional<Module> matchingModule = event.getContext().modules.parallelStream()//
                    .filter(e -> e instanceof GradleBackedModule)//
                    .map(e -> (GradleBackedModule) e)//
                    .filter(e -> e != module)//
                    .filter(e -> e.getProjectData().group.equals(notation.group))//
                    .filter(e -> e.getProjectData().archivesBaseName.equals(notation.module))//
                    .map(e -> (Module) e)//
                    .findFirst();
            if (matchingModule.isPresent()) {
                Module otherModule = matchingModule.get();
                event.setResult(new SourceSetDependencyImpl(otherModule, "main"));
            }
        }
    }
}

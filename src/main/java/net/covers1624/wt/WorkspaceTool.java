package net.covers1624.wt;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.covers1624.wt.api.Extension;
import net.covers1624.wt.api.ExtensionDetails;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.LibraryDependency;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.framework.FrameworkHandler;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.framework.ModdingFramework;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.impl.dependency.DependencyLibraryImpl;
import net.covers1624.wt.api.impl.dependency.LibraryDependencyImpl;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.module.ModuleImpl;
import net.covers1624.wt.api.impl.script.AbstractWorkspaceScript;
import net.covers1624.wt.api.impl.script.FrameworkRegistryImpl;
import net.covers1624.wt.api.impl.script.module.ModuleGroupImpl;
import net.covers1624.wt.api.impl.workspace.WorkspaceRegistryImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.ModuleList;
import net.covers1624.wt.api.workspace.WorkspaceRegistry;
import net.covers1624.wt.api.workspace.WorkspaceWriter;
import net.covers1624.wt.event.*;
import net.covers1624.wt.gradle.GradleManagerImpl;
import net.covers1624.wt.gradle.GradleModelCacheImpl;
import net.covers1624.wt.util.*;
import net.covers1624.wt.util.scala.ScalaSdk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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

    public Path projectDir;
    public Path cacheDir;

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
        projectDir = new File("").toPath().normalize().toAbsolutePath();
        cacheDir = projectDir.resolve(".workspace_tool");
        Path workspaceScript = projectDir.resolve("workspace.groovy");
        if (Files.notExists(workspaceScript)) {
            logger.error("'workspace.groovy' does not exist in the project directory. {}", projectDir);
            System.exit(1);
        }
        logger.info("WorkspaceTool@{}", VERSION);
        logger.info(" Project Dir:      {}", projectDir.toAbsolutePath());
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
        FrameworkRegistry frameworkRegistry = new FrameworkRegistryImpl();
        GradleManager gradleManager = new GradleManagerImpl();
        GradleModelCacheImpl modelCache = new GradleModelCacheImpl(gradleManager, projectDir, cacheDir);
        WorkspaceRegistry workspaceRegistry = new WorkspaceRegistryImpl();
        InitializationEvent.REGISTRY.fireEvent(new InitializationEvent(frameworkRegistry, gradleManager, workspaceRegistry));

        ModuleHashCheckEvent.REGISTRY.register(this::onModuleHashCheck);
        ProcessDependencyEvent.REGISTRY.register(Event.Priority.FIRST, this::onProcessDependency);

        logger.info("Preparing script..");
        Binding binding = new Binding();
        binding.setProperty(AbstractWorkspaceScript.FR_PROP, frameworkRegistry);
        binding.setProperty(AbstractWorkspaceScript.WR_PROP, workspaceRegistry);
        AbstractWorkspaceScript script = runScript(binding, workspaceScript, false);

        if (script.getFramework() == null) {
            logger.error("No framework specified in script.");
        }
        if (script.getWorkspace() == null) {
            logger.error("No workspace specified in script.");
        }

        DependencyAggregator dependencyResolver = new DependencyAggregator(script.getDepOverrides());
        DependencyLibraryImpl dependencyLibrary = new DependencyLibraryImpl();

        logger.info("Constructing module representation..");
        ModuleList moduleList = new ModuleList();
        List<Path> candidates = Files.walk(projectDir)//
                .parallel()//
                .filter(p -> {
                    if (Files.isDirectory(p) && !p.endsWith("buildSrc")) {
                        Path buildGradle = p.resolve("build.gradle");
                        return Files.isRegularFile(buildGradle) && Files.exists(buildGradle);
                    }
                    return false;
                })//
                .collect(Collectors.toList());
        for (Map.Entry<String, ModuleGroupImpl> entry : script.getWorkspaceModules().getGroups().entrySet()) {
            String group = entry.getKey();
            ModuleGroupImpl groupImpl = entry.getValue();
            Predicate<Path> matcher = groupImpl.createMatcher();
            ListIterator<Path> candidateIterator = candidates.listIterator();
            while (candidateIterator.hasNext()) {
                Path candidate = candidateIterator.next();
                if (matcher.test(projectDir.relativize(candidate))) {
                    candidateIterator.remove();
                    moduleList.modules.add(ModuleImpl.makeGradleModule(candidate.getFileName().toString(), group, candidate, modelCache));
                }
            }
        }
        logger.info("Constructing Framework module..");
        FrameworkHandler<?> frameworkHandler = frameworkRegistry.getFrameworkHandler(script.getFrameworkClass(), projectDir, cacheDir, gradleManager, modelCache);
        ModdingFramework framework = script.getFramework();
        frameworkHandler.constructFrameworkModules(unsafeCast(framework), moduleList);

        logger.info("Processing modules..");

        //TODO, Detect multiple scala SDK's, aggregate them and support multiple.
        ScalaSdk scalaSdk = new ScalaSdk();
        moduleList.getAllModules().forEach(scalaSdk::consume);
        //Resolve all dependencies, and remove duplicates.
        Iterable<Module> allModules = moduleList.getAllModules();
        allModules.forEach(dependencyResolver::consume);
        stream(allModules.spliterator(), true).forEach(module -> {
            module.getConfigurations().values().parallelStream().forEach(config -> {
                config.getDependencies().replaceAll(e -> {
                    if (e instanceof MavenDependency) {
                        return dependencyResolver.resolve(((MavenDependency) e).getNotation());
                    }
                    return e;
                });
            });
        });

        LibraryDependency scalaDep = new LibraryDependencyImpl()//
                .setLibraryName(scalaSdk.getSdkName());

        //Replace maven dependencies with module dependencies.
        moduleList.modules.forEach(module -> {
            module.getSourceSets().values().forEach(ss -> {
                for (Configuration config : Arrays.asList(//
                        ss.getCompileConfiguration(), //
                        ss.getCompileOnlyConfiguration(), //
                        ss.getRuntimeConfiguration())) {
                    if (config == null) {
                        continue;
                    }

                    config.walkHierarchy(cfg -> {
                        ListIterator<Dependency> itr = cfg.getDependencies().listIterator();
                        while (itr.hasNext()) {
                            Dependency dep = itr.next();
                            ProcessDependencyEvent event = new ProcessDependencyEvent(cacheDir, module, config, cfg, dep, moduleList);
                            ProcessDependencyEvent.REGISTRY.fireEvent(event);
                            Dependency result = event.getResult();
                            if (result == null) {
                                itr.remove();
                            } else {
                                itr.set(result);
                            }
                        }
                    });
                }
                Configuration compileConfig = ss.getCompileConfiguration();
                if (compileConfig != null) {
                    compileConfig.addDependency(scalaDep);
                }
            });
        });

        stream(allModules.spliterator(), true).forEach(dependencyLibrary::consume);

        ProcessModulesEvent.REGISTRY.fireEvent(new ProcessModulesEvent(moduleList, script));

        logger.info("Writing workspace..");
        WorkspaceWriter<?> workspaceWriter = workspaceRegistry.getWorkspaceWriter(script.getWorkspaceType(), projectDir, dependencyLibrary, scalaSdk);
        workspaceWriter.write(unsafeCast(script.getWorkspace()), moduleList, script.getRunConfigContainer().getRunConfigs());

        logger.info("Constructed {} modules.", moduleList.modules.size());
        moduleList.modules.sort(Comparator.comparing(Module::getGroup).thenComparing(Module::getName));
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("Group:", "Name:", "Path:"));
        for (Module module : moduleList.modules) {
            rows.add(Arrays.asList(module.getGroup(), module.getName(), module.getPath().toString()));
        }
        ColFormatter.format(rows).forEach(e -> logger.info(" " + e));
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
        importCustomizer.addImports("net.covers1624.wt.api.framework.ModdingFramework");
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
        event.putVersionedClass("net.covers1624.wt.api.data.ConfigurationData");
        event.putVersionedClass("net.covers1624.wt.api.data.ExtraData");
        event.putVersionedClass("net.covers1624.wt.api.data.ExtraDataExtensible");
        event.putVersionedClass("net.covers1624.wt.api.data.GradleData");
        event.putVersionedClass("net.covers1624.wt.api.data.PluginData");
        event.putVersionedClass("net.covers1624.wt.api.data.SourceSetData");
        event.putVersionedClass("net.covers1624.wt.api.gradle.model.WorkspaceToolModel");
        event.putVersionedClass("net.covers1624.wt.api.gradle.model.impl.WorkspaceToolModelImpl");

        event.putVersionedClass("net.covers1624.wt.gradle.builder.AbstractModelBuilder");
        event.putVersionedClass("net.covers1624.wt.gradle.builder.ExtraDataBuilder");
        event.putVersionedClass("net.covers1624.wt.gradle.builder.WorkspaceToolModelBuilder");
    }

    private void onProcessDependency(ProcessDependencyEvent event) {
        Module module = event.getModule();
        Dependency dependency = event.getDependency();
        ModuleList moduleList = event.getModuleList();
        if (dependency instanceof MavenDependency) {
            MavenDependency mavenDep = (MavenDependency) dependency;
            MavenNotation notation = mavenDep.getNotation();
            Optional<Module> matchingModule = moduleList.modules.parallelStream()//
                    .filter(e -> e instanceof GradleBackedModule)//
                    .map(e -> (GradleBackedModule) e)//
                    .filter(e -> e != module)//
                    .filter(e -> e.getGradleData().group.equals(notation.group))//
                    .filter(e -> e.getGradleData().archivesBaseName.equals(notation.module))//
                    .map(e -> (Module) e)//
                    .findFirst();
            if (matchingModule.isPresent()) {
                Module otherModule = matchingModule.get();
                event.setResult(new SourceSetDependencyImpl().setModule(otherModule).setSourceSet("main"));
            }
        }
    }

}

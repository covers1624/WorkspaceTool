/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.Extension;
import net.covers1624.wt.api.ExtensionDetails;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.api.script.module.ModuleSpec;
import net.covers1624.wt.api.script.runconfig.RunConfig;
import net.covers1624.wt.event.*;
import net.covers1624.wt.forge.api.impl.*;
import net.covers1624.wt.forge.api.script.*;
import net.covers1624.wt.forge.gradle.data.FG2McpMappingData;
import net.covers1624.wt.forge.gradle.data.FG3McpMappingData;
import net.covers1624.wt.forge.gradle.data.FGPluginData;
import net.covers1624.wt.forge.remap.CSVRemapper;
import net.covers1624.wt.forge.remap.DependencyRemapper;
import net.covers1624.wt.forge.remap.SRGToMCPRemapper;
import net.covers1624.wt.mc.data.VersionInfoJson;
import net.covers1624.wt.util.JarRemapper;
import net.covers1624.wt.util.JarStripper;
import net.covers1624.wt.util.TypedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * All Forge related handling for WorkspaceTool
 * Created by covers1624 on 17/6/19.
 */
@ExtensionDetails (name = "Forge", desc = "Provides integration for the MinecraftForge modding framework.")
public class ForgeExtension implements Extension {

    private static final Logger logger = LogManager.getLogger("ForgeExtension");

    public static final TypedMap.Key<VersionInfoJson> VERSION_INFO = new TypedMap.Key<>("forge:version_info");
    public static final TypedMap.Key<Path> ASSETS_PATH = new TypedMap.Key<>("forge:assets_path");

    private Optional<DependencyRemapper> remapper;

    @Override
    public void load() {
        InitializationEvent.REGISTRY.register(this::onInitialization);
        PrepareScriptEvent.REGISTRY.register(this::onPrepareScript);
        ModuleHashCheckEvent.REGISTRY.register(this::onModuleHashCheck);
        EarlyProcessDependencyEvent.REGISTRY.register(this::onProcessDependency);
        ProcessModulesEvent.REGISTRY.register(this::processModules);
        ScriptWorkspaceEvalEvent.REGISTRY.register(this::onScriptWorkspaceEvalEvent);
        Forge112Extension.init();
        Forge114Extension.init();
        Forge117Extension.init();
    }

    private void onInitialization(InitializationEvent event) {
        WorkspaceToolContext context = event.getContext();
        FrameworkRegistry registry = context.frameworkRegistry;
        GradleManager gradleManager = context.gradleManager;

        registry.registerScriptImpl(Forge112.class, Forge112Impl::new);
        registry.registerScriptImpl(Forge114.class, Forge114Impl::new);
        registry.registerScriptImpl(Forge117.class, Forge117Impl::new);

        registry.registerFrameworkHandler(Forge112.class, Forge112FrameworkHandler::new);
        registry.registerFrameworkHandler(Forge114.class, Forge114FrameworkHandler::new);
        registry.registerFrameworkHandler(Forge117.class, Forge117FrameworkHandler::new);

        // Add WT Forge-Gradle module.
        gradleManager.includeClassMarker("net.covers1624.wt.forge.gradle.FGDataBuilder");
        gradleManager.addDataBuilder("net.covers1624.wt.forge.gradle.FGDataBuilder");
        gradleManager.executeBefore("genSrgs", "mergeJars");
    }

    private void onPrepareScript(PrepareScriptEvent event) {
        ImportCustomizer customizer = new ImportCustomizer();
        customizer.addStarImports("net.covers1624.wt.forge.api.script");
        event.getCompilerConfiguration().addCompilationCustomizers(customizer);
    }

    private void onModuleHashCheck(ModuleHashCheckEvent event) {
        event.putClassBytes("net.covers1624.wt.forge.gradle.data.FG2McpMappingData");
        event.putClassBytes("net.covers1624.wt.forge.gradle.data.FGPluginData");
        event.putClassBytes("net.covers1624.wt.forge.gradle.data.FG2Data");
        event.putClassBytes("net.covers1624.wt.forge.gradle.FGDataBuilder");
        event.putClassBytes("net.covers1624.wt.forge.gradle.FGVersion");
    }

    private void onScriptWorkspaceEvalEvent(ScriptWorkspaceEvalEvent event) {
        if (event.getScript().getFrameworkClass() == Forge114.class || event.getScript().getFrameworkClass() == Forge117.class) {
            MixinInstantiator mixinInstantiator = event.getMixinInstantiator();
            mixinInstantiator.addMixinClass(RunConfig.class, Forge114RunConfig.class, Forge114RunConfigTemplate.class);
            mixinInstantiator.addMixinClass(ModuleSpec.class, Forge114ModuleSpec.class, Forge114ModuleSpecTemplate.class);
        }
    }

    private void onProcessDependency(EarlyProcessDependencyEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (event.getContext().workspaceScript.getFramework() instanceof ForgeFramework) {
            //Used as latch to compute. Empty means something else from null.
            //noinspection OptionalAssignedToNull
            if (remapper == null) {
                GradleBackedModule forgeModule = findForgeRootModule(context);
                ProjectData projectData = forgeModule.getProjectData();
                FGPluginData pluginData = projectData.pluginData.getData(FGPluginData.class);
                if (pluginData != null && pluginData.version.isFG2()) {
                    FG2McpMappingData mappingData = projectData.getData(FG2McpMappingData.class);
                    if (mappingData == null) {
                        throw new RuntimeException("Forge module missing mapping data.");
                    }
                    remapper = Optional.of(new DependencyRemapper(context.cacheDir, new JarRemapper(new SRGToMCPRemapper(mappingData))));
                } else {
                    ProjectData forgeSubModuleData = requireNonNull(projectData.subProjects.get("forge"), "'forge' submodule not found on Forge project.");
                    FG3McpMappingData mappingData = forgeSubModuleData.getData(FG3McpMappingData.class);
                    if (mappingData != null) {
                        try {
                            remapper = Optional.of(new DependencyRemapper(context.cacheDir, new JarRemapper(new CSVRemapper(mappingData.mappingsZip.toPath()))));
                        } catch (IOException e) {
                            logger.warn("Unable to setup CSVReampper!", e);
                        }
                    } else {
                        logger.warn("Forge project does not have FG3McpMappingData!");
                    }
                    //noinspection OptionalAssignedToNull
                    if (remapper == null) {
                        remapper = Optional.empty();
                    }
                }
            }
            Dependency dep = event.getDependency();
            remapper.ifPresent(remapper -> {
                Module module = event.getModule();
                Configuration config = event.getDependencyConfig();
                if (dep instanceof MavenDependency) {
                    MavenDependency mvnDep = (MavenDependency) dep;
                    MavenNotation notation = mvnDep.getNotation();
                    if (notation.group.startsWith("deobf.")) {
                        event.setResult(null);
                    } else if (config.getName().equals("deobfCompile") || config.getName().equals("deobfProvided")) {
                        event.setResult(remapper.process(mvnDep));
                    } else {
                        Configuration fg3Obfuscated = module.getConfigurations().get("__obfuscated");
                        if (fg3Obfuscated != null) {
                            Optional<MavenDependency> unobfDep = fg3Obfuscated.getAllDependencies().stream()
                                    .filter(e -> e instanceof MavenDependency)
                                    .map(e -> (MavenDependency) e)
                                    .filter(e -> {
                                        MavenNotation n2 = e.getNotation();
                                        if (!(n2.group.equals(notation.group))) return false;
                                        if (!(n2.module.equals(notation.module))) return false;
                                        if (!(Objects.equals(n2.classifier, notation.classifier))) return false;
                                        if (!notation.version.contains("_mapped_") && n2.version.equals(notation.version)) return true;
                                        int strip = notation.version.indexOf("_mapped_");
                                        return n2.version.equals(notation.version.substring(0, strip));
                                    })
                                    .findFirst();
                            unobfDep.ifPresent(e -> event.setResult(remapper.process(e)));
                        }
                    }
                }
            });
            //HACK, Remove Scala classes from Scorge jar.
            if (dep instanceof MavenDependency) {
                MavenDependency mvnDep = (MavenDependency) dep;
                if (mvnDep.getNotation().group.equals("net.minecraftforge") && mvnDep.getNotation().module.equals("Scorge")) {
                    String path = mvnDep.getNotation().toPath();
                    Path output = context.cacheDir.resolve("scorge_strip").resolve(path);
                    JarStripper.stripJar(mvnDep.getClasses(), output, e -> !e.toString().startsWith("scala/"));
                    MavenDependency newDep = new MavenDependencyImpl(mvnDep).setClasses(output);
                    event.setResult(newDep);
                }
            }
        }
    }

    public void processModules(ProcessModulesEvent event) {
        event.getContext().modules.forEach(m -> {
            Map<String, Configuration> cfgs = m.getConfigurations();
            SourceSet main = m.getSourceSets().get("main");
            Configuration deobfCompile = cfgs.get("deobfCompile");
            Configuration deobfProvided = cfgs.get("deobfProvided");
            Configuration compileConfiguration = main.getCompileConfiguration();
            Configuration compileOnlyConfiguration = main.getCompileOnlyConfiguration();
            if (compileConfiguration != null) {
                if (deobfCompile != null) {
                    compileConfiguration.addExtendsFrom(deobfCompile);
                }
            }
            if (compileOnlyConfiguration != null && deobfProvided != null) {
                compileOnlyConfiguration.addExtendsFrom(deobfProvided);
            }
        });

    }

    static GradleBackedModule findForgeRootModule(WorkspaceToolContext context) {
        return (GradleBackedModule) context.frameworkModules.stream()
                .filter(e -> e.getName().equals("Forge") || e.getName().equals("ForgeRoot"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Missing Forge module."));
    }

    static GradleBackedModule findForgeSubModule(WorkspaceToolContext context) {
        return (GradleBackedModule) context.frameworkModules.stream()
                .filter(e -> e.getName().equals("Forge/forge") || e.getName().equals("ForgeRoot/forge"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Missing Forge module."));
    }
}

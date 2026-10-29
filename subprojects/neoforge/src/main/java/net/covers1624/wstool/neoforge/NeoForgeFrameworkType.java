package net.covers1624.wstool.neoforge;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.GitRepoManager;
import net.covers1624.wstool.api.HashContainer;
import net.covers1624.wstool.api.ModuleProcessor;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.SourceSet;
import net.covers1624.wstool.api.workspace.Workspace;
import net.covers1624.wstool.api.workspace.runs.EvalValue.ClasspathValue;
import net.covers1624.wstool.api.workspace.runs.RunConfig;
import net.covers1624.wstool.gradle.GradleTaskExecutor;
import net.covers1624.wstool.gradle.api.data.ConfigurationList;
import net.covers1624.wstool.gradle.api.data.JavaToolchainData;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectExtData;
import net.covers1624.wstool.minecraft.*;
import net.covers1624.wstool.neoforge.gradle.api.NeoDevData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 5/19/25.
 */
public interface NeoForgeFrameworkType extends ForgeLikeFramework {

    String path();

    String url();

    String branch();

    String commit();

    @Nullable
    String parchment();

    @Override
    default void buildFrameworks(Environment env, Workspace workspace) {
        ModuleProcessor moduleProcessor = env.getService(ModuleProcessor.class);
        GradleTaskExecutor taskExecutor = env.getService(GradleTaskExecutor.class);
        JSTExecutor jstExecutor = env.getService(JSTExecutor.class);
        AssetDownloader assetDownloader = env.getService(AssetDownloader.class);

        Path rootDir = env.projectRoot().resolve(path());

        HashContainer hashContainer = new HashContainer(env.projectCache(), "neoforge");
        GitRepoManager gitManager = new GitRepoManager(rootDir);
        gitManager.setConfig(url(), branch(), commit());

        var requiresSetupProp = hashContainer.getProperty("requires_setup")
                .withDefault(true);

        try {
            if (gitManager.checkout()) {
                requiresSetupProp.setValue(true);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to update NeoForge clone.", ex);
        }

        var ifaceCache = hashContainer.getEntry("iface_injections");
        var ifaceInjections = new ArrayList<Path>();

        var atCache = hashContainer.getEntry("access_transformers");
        var accessTransformers = new ArrayList<Path>();

        var parchmentCache = hashContainer.getEntry("parchment");
        var parchmentVersion = parchment();
        if (parchmentVersion != null) {
            parchmentCache.putString(parchmentVersion);
        }

        var possibleMods = findPossibleMods(workspace);

        var nfModule = moduleProcessor.buildModule(workspace, rootDir, Set.of(), excludeModules());
        var nfSubModule = getMainSubModule(nfModule);
        var nfMain = nfSubModule.sourceSets().get("main");
        var nfClient = nfSubModule.sourceSets().get("client");
        var nfCoreMods = nfModule.subModules().get("neoforge-coremods");

        var legacyRemapper = getLegacyMappings(nfSubModule);

        for (Module module : workspace.allProjectModules()) {
            var projectData = module.projectData();
            if (projectData == null) continue;

            var ifaceData = projectData.getData(InterfaceInjectionData.class);
            if (ifaceData != null) {
                for (File e : ifaceData.files) {
                    if (e.exists()) {
                        ifaceInjections.add(e.toPath());
                    }
                }
            }

            var atData = projectData.getData(AccessTransformerData.class);
            if (atData != null) {
                atData.files.forEach(e -> {
                    if (e.exists()) {
                        accessTransformers.add(e.toPath());
                    }
                });
            }
        }

        ifaceInjections.forEach(ifaceCache::putFile);
        accessTransformers.forEach(atCache::putFile);
        if (requiresSetupProp.getBoolean() || ifaceCache.changed() || atCache.changed() || parchmentCache.changed()) {
            taskExecutor.runTask(rootDir, "setup");
            var importTasks = ideImportTasks(env, nfModule, nfSubModule);
            if (!importTasks.isEmpty()) {
                taskExecutor.tryRunTasks(rootDir, importTasks.toArray(String[]::new));
            }

            List<Path> mcSources = new ArrayList<>(
                    nfMain.sourcePaths().getOrDefault("java", List.of())
            );
            if (nfClient != null) {
                mcSources.addAll(nfClient.sourcePaths().getOrDefault("java", List.of()));
            }

            jstExecutor.applyJST(
                    nfMain,
                    mcSources,
                    ifaceInjections,
                    accessTransformers,
                    parchmentVersion,
                    legacyRemapper
            );

            requiresSetupProp.setValue(false);
            ifaceCache.pushChanges();
            atCache.pushChanges();
            parchmentCache.pushChanges();
        }

        applyNeoForgeToolchain(nfModule, nfSubModule, workspace);

        var legacyClasspath = new LinkedHashSet<>(nfMain.runtimeDependencies());

        var attachedSourceSets = new ArrayList<SourceSet>();
        for (Module module : workspace.allProjectModules()) {
            // TODO this needs to be better, perhaps we can add an exclude param to allProjectModules.
            if (module == nfModule || nfModule.subModules().containsValue(module)) continue;
            for (SourceSet ss : module.sourceSets().values()) {
                // Exclude test source set. Any attrs on it should be inherited
                // by the main source set, we also don't want it to be on the runtime classpath of Neo..
                if (ss.name().equals("test")) continue;

                var ssData = ss.sourceSetData();

                // Link older Forge modules to NeoForge as well.
                if (ssData.attributes.contains(ModLoaderAttribute.NEO_FORGE) || ssData.attributes.contains(ModLoaderAttribute.MC_FORGE)) {
                    // we always need a non-exported runtime dependency here, so any extra dependencies the module
                    // has are also available for the game to discover.
                    nfMain.runtimeDependencies().add(new Dependency.SourceSetDependency(ss, false));
                    attachedSourceSets.add(ss);

                    ss.compileDependencies().add(new Dependency.SourceSetDependency(nfMain, true));
                    // TODO split source set mods will need some work here.
                    if (nfClient != null) {
                        ss.compileDependencies().add(new Dependency.SourceSetDependency(nfClient, true));
                    }
                }
            }
        }

        var interfacesJar = env.projectCache().resolve("interfaces.jar");
        InterfaceInjectionStubGenerator.generate(interfacesJar, ifaceInjections);
        nfMain.compileDependencies().add(new Dependency.FileDependency(
                interfacesJar,
                FastStream.of(attachedSourceSets)
                        .flatMap(e -> e.sourcePaths().values())
                        .flatMap(e -> e)
                        .toList(),
                true
        ));

        var moduleClasspath = buildModuleClasspath(nfModule, nfSubModule, moduleProcessor);
        if (moduleClasspath != null) {
            legacyClasspath.removeAll(moduleClasspath);
        }
        adjustLegacyClasspath(legacyClasspath);

        var assetIndex = assetDownloader.downloadAssets(getMcVersion(nfModule, nfSubModule));

        for (RunConfig run : workspace.runConfigs().values()) {
            var type = run.config().get("type");
            if (type == null) throw new RuntimeException("Expected run config " + run.name() + " to contain 'type' config option.");

            // Any combination of 'client' or 'data', but as long as 'server' is missing, to negative match 'server_data'.
            var includeClient = type.contains("client") || type.contains("data") && !type.contains("server");
            var isSplitSourcesClient = nfClient != null && includeClient;

            List<Dependency> modClassesMcDependencies = new ArrayList<>();
            modClassesMcDependencies.add(new Dependency.SourceSetDependency(nfMain, true));
            modClassesMcDependencies.add(new Dependency.SourceSetDependency(nfMain, true));
            if (isSplitSourcesClient) {
                modClassesMcDependencies.add(new Dependency.SourceSetDependency(nfClient, true));
                modClassesMcDependencies.add(new Dependency.SourceSetDependency(nfClient, true));
            }

            List<ModClassesEvalValue.ModClass> modClasses = new ArrayList<>();
            modClasses.add(new ModClassesEvalValue.ModClass("minecraft", modClassesMcDependencies));
            if (nfCoreMods != null) {
                modClasses.add(new ModClassesEvalValue.ModClass("neoforge-coremods", new Dependency.SourceSetDependency(nfCoreMods.sourceSets().get("main"), true)));
            }
            possibleMods.forEach(mod -> modClasses.add(new ModClassesEvalValue.ModClass(mod.modId(), new Dependency.SourceSetDependency(mod.ss(), true))));
            run.envVars().putEval("MOD_CLASSES", new ModClassesEvalValue(modClasses));

            if (moduleClasspath != null) {
                run.vmArgs().addFirstEval(new ClasspathValue(moduleClasspath));
                run.vmArgs().addFirst("-p");
            }
            run.sysProps().put("java.net.preferIPv6Addresses", "system");
            run.sysProps().put("ignoreList", "mixinextras-neoforge-,client-extra,neoforge-");
            run.sysProps().putEval("legacyClassPath", new ClasspathValue(legacyClasspath));

            if (includeClient) {
                run.args().addAll(List.of(
                        "--assetsDir", assetDownloader.assetsDir.toAbsolutePath().toString(),
                        "--assetIndex", assetIndex.id()
                ));
            }

            addRunArguments(nfModule, nfSubModule, run, type);

            if (isSplitSourcesClient) {
                run.classpath(nfClient);
            } else {
                run.classpath(nfMain);
            }
        }

        postBuild(env, nfModule, nfSubModule);
    }

    default Set<String> excludeModules() {
        return Set.of(":NeoForge-Root:base", ":NeoForge-Root:clean");
    }

    default List<String> ideImportTasks(Environment env, Module nfModule, Module nfSubModule) {
        return List.of(":neoforge:neoForgeIdeSync");
    }

    default void postBuild(Environment env, Module nfModule, Module nfSubModule) {
    }

    default Module getMainSubModule(Module nfModule) {
        return nfModule.subModules().get("neoforge");
    }

    default @Nullable SRGRemapper getLegacyMappings(Module nfSubModule) {
        return null;
    }

    void addRunArguments(Module nfModule, Module nfSubModule, RunConfig run, String type);

    private void applyNeoForgeToolchain(Module rootModule, Module nfSubModule, Workspace workspace) {
        var projectData = nfSubModule.projectData();
        if (projectData == null) return;

        JavaToolchainData toolchainData = projectData.getData(JavaToolchainData.class);
        if (toolchainData == null) return;

        workspace.setJavaVersion(toolchainData.langVersion);
    }

    default @Nullable Set<Dependency> buildModuleClasspath(Module rootModule, Module module, ModuleProcessor processor) {
        var projectData = requireNonNull(module.projectData());
        var pluginData = projectData.getData(PluginData.class);
        if (pluginData == null) {
            throw new RuntimeException("Expected PluginData to exist. Unable to setup NeoDev workspace.");
        }
        var neoDevData = pluginData.getData(NeoDevData.class);
        if (neoDevData == null) {
            throw new RuntimeException("Expected NeoDevData to be extracted. Unable to setup NeoDev workspace.");
        }
        if (neoDevData.moduleClasspathConfiguration == null) return null;

        var configurations = requireNonNull(projectData.getData(ConfigurationList.class));
        var moduleClasspathConfiguration = configurations.get(neoDevData.moduleClasspathConfiguration);
        if (moduleClasspathConfiguration == null) {
            throw new RuntimeException("Expected configuration " + neoDevData.moduleClasspathConfiguration + " to be extracted. Unable to setup NeoDev workspace.");
        }

        return processor.processConfiguration(rootModule, moduleClasspathConfiguration);
    }

    default void adjustLegacyClasspath(Set<Dependency> dependencies) {
    }

    default String getMcVersion(Module rootModule, Module module) {
        var gradleData = requireNonNull(module.projectData());
        var properties = requireNonNull(gradleData.getData(ProjectExtData.class))
                .properties;
        return requireNonNull(properties.get("minecraft_version"), "I don't know how to get the Minecraft version from this NeoForge project.");
    }
}

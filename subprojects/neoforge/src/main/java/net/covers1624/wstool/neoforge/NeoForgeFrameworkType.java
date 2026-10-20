package net.covers1624.wstool.neoforge;

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
import net.covers1624.wstool.gradle.api.data.*;
import net.covers1624.wstool.minecraft.*;
import net.covers1624.wstool.neoforge.gradle.api.NeoDevData;
import org.jetbrains.annotations.Nullable;

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

        var nfModule = moduleProcessor.buildModule(workspace, rootDir, Set.of());
        var nfSubModule = nfModule.subModules().get("neoforge");
        var nfMain = nfSubModule.sourceSets().get("main");
        var nfClient = nfSubModule.sourceSets().get("client");
        var nfCoreMods = nfModule.subModules().get("neoforge-coremods");
        var nfCoreModsMain = nfCoreMods.sourceSets().get("main");

        // TODO this is not correct if we ever wire stuff up to the base/vanilla module.
        for (Module module : workspace.allProjectModules()) {
            var projectData = module.projectData();
            if (projectData == null) continue;

            var ifaceData = projectData.getData(InterfaceInjectionData.class);
            if (ifaceData != null) {
                ifaceData.files.forEach(e -> ifaceInjections.add(e.toPath()));
            }

            var atData = projectData.getData(AccessTransformerData.class);
            if (atData != null) {
                atData.files.forEach(e -> accessTransformers.add(e.toPath()));
            }
        }

        ifaceInjections.forEach(ifaceCache::putFile);
        accessTransformers.forEach(atCache::putFile);
        if (requiresSetupProp.getBoolean() || ifaceCache.changed() || atCache.changed() || parchmentCache.changed()) {
            taskExecutor.runTask(rootDir, "clean");
            taskExecutor.runTask(rootDir, "setup");
            taskExecutor.tryRunTasks(rootDir, ":neoforge:neoForgeIdeSync");

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
                    parchmentVersion
            );

            requiresSetupProp.setValue(false);
            ifaceCache.pushChanges();
            atCache.pushChanges();
            parchmentCache.pushChanges();
        }

        applyNeoForgeToolchain(requireNonNull(nfModule.projectData()), workspace);

        var legacyClasspath = new LinkedHashSet<>(nfMain.runtimeDependencies());

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
                    nfMain.compileDependencies().add(new Dependency.SourceSetDependency(ss));
                    ss.compileDependencies().add(new Dependency.SourceSetDependency(nfMain));
                    // TODO split source set mods will need some work here.
                    if (nfClient != null) {
                        ss.compileDependencies().add(new Dependency.SourceSetDependency(nfClient));
                    }
                }
            }
        }

        var moduleClasspath = buildModuleClasspath(nfModule, nfSubModule, moduleProcessor);
        if (moduleClasspath != null) {
            legacyClasspath.removeAll(moduleClasspath);
        }

        var assetIndex = assetDownloader.downloadAssets(getMcVersion(nfSubModule));

        var cliProperties = buildCliProperties(nfSubModule);
        for (RunConfig run : workspace.runConfigs().values()) {
            var type = run.config().get("type");
            if (type == null) throw new RuntimeException("Expected run config " + run.name() + " to contain 'type' config option.");

            var isSplitSourcesClient = nfClient != null && type.contains("client");

            List<Dependency> modClassesMcDependencies = new ArrayList<>();
            modClassesMcDependencies.add(new Dependency.SourceSetDependency(nfMain));
            modClassesMcDependencies.add(new Dependency.SourceSetDependency(nfMain));
            if (isSplitSourcesClient) {
                modClassesMcDependencies.add(new Dependency.SourceSetDependency(nfClient));
                modClassesMcDependencies.add(new Dependency.SourceSetDependency(nfClient));
            }

            List<ModClassesEvalValue.ModClass> modClasses = new ArrayList<>();
            modClasses.add(new ModClassesEvalValue.ModClass("minecraft", modClassesMcDependencies));
            modClasses.add(new ModClassesEvalValue.ModClass("neoforge-coremods", new Dependency.SourceSetDependency(nfCoreModsMain)));
            possibleMods.forEach(mod -> modClasses.add(new ModClassesEvalValue.ModClass(mod.modId(), new Dependency.SourceSetDependency(mod.ss()))));
            run.envVars().putEval("MOD_CLASSES", new ModClassesEvalValue(modClasses));

            // Use addFirst so we are in front of any user-added args.
            run.vmArgs().addFirst(getVMArgs());
            if (moduleClasspath != null) {
                run.vmArgs().addFirstEval(new ClasspathValue(moduleClasspath));
                run.vmArgs().addFirst("-p");
            }
            run.sysProps().put("java.net.preferIPv6Addresses", "system");
            run.sysProps().put("ignoreList", "mixinextras-neoforge-,client-extra,neoforge-");
            run.sysProps().putEval("legacyClassPath", new ClasspathValue(legacyClasspath));

            run.args().addAll(List.of(
                    "--gameDir", ".",
                    "--assetsDir", assetDownloader.assetsDir.toAbsolutePath().toString(),
                    "--assetIndex", assetIndex.id()
            ));

            addLaunchTarget(run, type);

            if (isSplitSourcesClient) {
                run.classpath(nfClient);
            } else {
                run.classpath(nfMain);
            }

            run.args().addAll(cliProperties);
        }
    }

    default void addLaunchTarget(RunConfig run, String type) {
        if (run.mainClass() == null) {
            run.mainClass("cpw.mods.bootstraplauncher.BootstrapLauncher");
        }
        var launchTarget = switch (type) {
            case "client" -> "forgeclientdev";
            case "data" -> "forgedatadev";
            case "server" -> "forgeserverdev";
            default -> throw new RuntimeException("Unknown type. Expecting 'client', 'server', 'data'. Got: " + type);
        };
        if (launchTarget != null) {
            run.args().addAll(List.of("--launchTarget", launchTarget));
        }
    }

    default List<String> getVMArgs() {
        return List.of(
                "--add-modules", "ALL-MODULE-PATH",
                "--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler",
                "--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler",
                "--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler",
                "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming"
        );
    }

    private void applyNeoForgeToolchain(ProjectData projectData, Workspace workspace) {
        var subProjectData = projectData.getData(SubProjectList.class);
        if (subProjectData == null) return;

        var nfSubProject = subProjectData.get("neoforge");
        if (nfSubProject == null) return;

        JavaToolchainData toolchainData = nfSubProject.getData(JavaToolchainData.class);
        if (toolchainData == null) return;

        workspace.setJavaVersion(toolchainData.langVersion);
    }

    private static @Nullable Set<Dependency> buildModuleClasspath(Module rootModule, Module module, ModuleProcessor processor) {
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

    private static String getMcVersion(Module module) {
        var gradleData = requireNonNull(module.projectData());
        var properties = requireNonNull(gradleData.getData(ProjectExtData.class))
                .properties;

        return properties.get("minecraft_version");
    }

    private static List<String> buildCliProperties(Module module) {
        var gradleData = requireNonNull(module.projectData());
        var properties = requireNonNull(gradleData.getData(ProjectExtData.class))
                .properties;

        var version = gradleData.version;
        return List.of(
                "--version", version,
                "--fml.neoForgeVersion", version,
                "--fml.fmlVersion", properties.get("fancy_mod_loader_version"),
                "--fml.mcVersion", properties.get("minecraft_version"),
                "--fml.neoFormVersion", properties.get("neoform_version")
        );
    }
}

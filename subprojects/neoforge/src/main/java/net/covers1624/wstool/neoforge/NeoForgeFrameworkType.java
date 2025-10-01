package net.covers1624.wstool.neoforge;

import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.quack.net.httpapi.java11.Java11HttpEngine;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.GitRepoManager;
import net.covers1624.wstool.api.HashContainer;
import net.covers1624.wstool.api.ModuleProcessor;
import net.covers1624.wstool.api.extension.FrameworkType;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.SourceSet;
import net.covers1624.wstool.api.workspace.Workspace;
import net.covers1624.wstool.api.workspace.runs.EvalValue.ClasspathValue;
import net.covers1624.wstool.api.workspace.runs.RunConfig;
import net.covers1624.wstool.gradle.api.data.*;
import net.covers1624.wstool.minecraft.AssetDownloader;
import net.covers1624.wstool.neoforge.gradle.api.NeoDevData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 5/19/25.
 */
public interface NeoForgeFrameworkType extends FrameworkType {

    String path();

    String url();

    String branch();

    String commit();

    @Override
    default void buildFrameworks(Environment env, Workspace workspace) {
        ModuleProcessor moduleProcessor = env.getService(ModuleProcessor.class);
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

        var nfModule = moduleProcessor.buildModule(workspace, rootDir, Set.of());

        if (requiresSetupProp.getBoolean()) {
            moduleProcessor.runTask(rootDir, "clean");
            moduleProcessor.runTask(rootDir, "setup");
            requiresSetupProp.setValue(false);
        }

        applyNeoForgeToolchain(requireNonNull(nfModule.projectData()), workspace);

        var nfSubModule = nfModule.subModules().get("neoforge");
        var nfMain = nfSubModule.sourceSets().get("main");
        var nfCoreMods = nfModule.subModules().get("neoforge-coremods");
        var nfCoreModsMain = nfCoreMods.sourceSets().get("main");

        var legacyClasspath = new LinkedHashSet<>(nfMain.runtimeDependencies());

        // TODO we should be able to detect which source sets are mods in Gradle.
        for (Module module : workspace.allProjectModules()) {
            // TODO this needs to be better, perhaps we can add an exclude param to allProjectModules.
            if (module == nfModule || nfModule.subModules().containsValue(module)) continue;
            for (SourceSet ss : module.sourceSets().values()) {
                if (isNeoForgeModPresent(ss)) {
                    nfMain.runtimeDependencies().add(new Dependency.SourceSetDependency(ss));
                    ss.compileDependencies().add(new Dependency.SourceSetDependency(nfMain));
                }
            }
        }

        var moduleClasspath = buildModuleClasspath(nfModule, nfSubModule, moduleProcessor);
        legacyClasspath.removeAll(moduleClasspath);

        HttpEngine http = Java11HttpEngine.create();
        var downloadResult = AssetDownloader.downloadAssets(env, http, getMcVersion(nfSubModule));

        var cliProperties = buildCliProperties(nfSubModule);
        for (RunConfig run : workspace.runConfigs().values()) {
            run.classpath(nfMain);

            if (run.mainClass() == null) {
                run.mainClass("cpw.mods.bootstraplauncher.BootstrapLauncher");
            }
            // TODO all the mods go here too?
            //      Apparently mods going here is only required in Older forge, or when they have split classes & resources?
            run.envVars().putEval("MOD_CLASSES", new ModClassesEvalValue(List.of(
                    new ModClassesEvalValue.ModClass("minecraft", new Dependency.SourceSetDependency(nfMain)),
                    new ModClassesEvalValue.ModClass("neoforge-coremods", new Dependency.SourceSetDependency(nfCoreModsMain))
            )));

            // Use addFirst so we are in front of any user-added args.
            run.vmArgs().addFirst(List.of(
                    "--add-modules", "ALL-MODULE-PATH",
                    "--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler",
                    "--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler",
                    "--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler",
                    "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming"
            ));
            run.vmArgs().addFirstEval(new ClasspathValue(moduleClasspath));
            run.vmArgs().addFirst("-p");
            run.sysProps().put("java.net.preferIPv6Addresses", "system");
            run.sysProps().put("ignoreList", "mixinextras-neoforge-,client-extra,neoforge-");
            run.sysProps().putEval("legacyClassPath", new ClasspathValue(legacyClasspath));

            run.args().addAll(List.of(
                    "--gameDir", ".",
                    "--assetsDir", downloadResult.assetsDir().toAbsolutePath().toString(),
                    "--assetIndex", downloadResult.assetIndex().id()
            ));

            var launchTarget = switch (run.config().get("type")) {
                case "client" -> "forgeclientdev";
                case "data" -> "forgedatadev";
                case "server" -> "forgeserverdev";
                case null, default -> null;
            };
            if (launchTarget != null) {
                run.args().addAll(List.of("--launchTarget", launchTarget));
            }
            run.args().addAll(cliProperties);
        }
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

    private static Set<Dependency> buildModuleClasspath(Module rootModule, Module module, ModuleProcessor processor) {
        var projectData = requireNonNull(module.projectData());
        var pluginData = projectData.getData(PluginData.class);
        if (pluginData == null) {
            throw new RuntimeException("Expected PluginData to exist. Unable to setup NeoDev workspace.");
        }
        var neoDevData = pluginData.getData(NeoDevData.class);
        if (neoDevData == null) {
            throw new RuntimeException("Expected NeoDevData to be extracted. Unable to setup NeoDev workspace.");
        }

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

    private static boolean isNeoForgeModPresent(SourceSet sourceSet) {
        for (Path resourcesDir : sourceSet.sourcePaths().getOrDefault("resources", List.of())) {
            if (Files.exists(resourcesDir.resolve("META-INF/mods.toml")) || Files.exists(resourcesDir.resolve("META-INF/neoforge.mods.toml"))) {
                return true;
            }
        }
        return false;
    }
}

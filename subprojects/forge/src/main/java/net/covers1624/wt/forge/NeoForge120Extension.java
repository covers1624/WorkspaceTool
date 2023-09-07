package net.covers1624.wt.forge;

import com.google.common.collect.ImmutableList;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.*;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.script.runconfig.RunConfig;
import net.covers1624.wt.api.workspace.WorkspaceModule;
import net.covers1624.wt.event.ProcessModulesEvent;
import net.covers1624.wt.event.ProcessWorkspaceModulesEvent;
import net.covers1624.wt.forge.api.export.ForgeExportedData;
import net.covers1624.wt.forge.api.script.Forge114RunConfig;
import net.covers1624.wt.forge.api.script.NeoForge120;
import net.covers1624.wt.mc.data.VersionInfoJson;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.collection.FastStream.of;
import static net.covers1624.wt.forge.ForgeExtension.*;

/**
 * Created by covers1624 on 7/9/23.
 */
public class NeoForge120Extension extends Forge117Extension {

    static void init() {
        ProcessModulesEvent.REGISTRY.register(NeoForge120Extension::onProcessModules);
        ProcessWorkspaceModulesEvent.REGISTRY.register(NeoForge120Extension::onProcessWorkspaceModules);
    }

    private static void onProcessModules(ProcessModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (!context.isFramework(NeoForge120.class)) return;

        // This is a bit of a hack, we remove the FG3+ 'minecraft' configuration from the 'implementation' configuration
        //  this configuration should only contain the Deobfuscated & Remapped Forge, and all forge + Minecraft dependencies.
        //  These dependencies are provided via the module dep on Forge and are safe to remove.
        for (Module module : event.getContext().modules) {
            Configuration minecraftConfig = module.getConfigurations().get("minecraft");
            if (minecraftConfig != null) {
                minecraftConfig.getDependencies().clear();
            }
        }
    }

    private static void onProcessWorkspaceModules(ProcessWorkspaceModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (!context.isFramework(NeoForge120.class)) return;

        GradleBackedModule forgeModule = findForgeRootModule(context);
        GradleBackedModule forgeSubModule = findForgeSubModule(context);
        ProjectData rootProject = forgeModule.getProjectData();
        ProjectData forgeSubProject = requireNonNull(rootProject.subProjects.get("forge"), "Missing forge submodule.");
        WorkspaceModule forgeSubProjectWM = of(context.workspaceModules)
                .filter(e -> e.getName().equals(forgeSubProject.getProjectCoords().replace(":", ".") + ".main"))
                .only();
        Map<DependencyScope, Set<Dependency>> depMap = forgeSubProjectWM.getDependencies();
        FastStream<LibraryDependency> modRuntimeDeps = of(depMap.get(DependencyScope.RUNTIME))
                .filter(e -> e instanceof WorkspaceModuleDependency)
                .map(e -> ((WorkspaceModuleDependency) e).getModule())
                .flatMap(e -> of(e.getDependencies().get(DependencyScope.COMPILE))
                        .concat(of(e.getDependencies().get(DependencyScope.RUNTIME)))
                )
                .filter(e -> e instanceof LibraryDependency)
                .map(e -> (LibraryDependency) e)
                .filter(e -> !(e.getDependency() instanceof MavenDependency dep) || !dep.isRemapped() && !isMod(dep.getClasses()));

        List<String> runtimeClasspath = of(depMap.get(DependencyScope.COMPILE))
                .concat(modRuntimeDeps)
                .map(Forge117Extension::evalDependency)
                .map(Path::toString)
                .distinct()
                .toLinkedList();

        List<Path> moduleOnly = of(forgeSubModule.getConfigurations().get("moduleonly").getAllDependencies())
                .map(Forge117Extension::evalDependency)
                .toList();
        List<Path> gameLayerLibrary = of(forgeSubModule.getConfigurations().get("gameLayerLibrary").getAllDependencies())
                .map(Forge117Extension::evalDependency)
                .toList();
        List<Path> pluginLayerLibrary = of(forgeSubModule.getConfigurations().get("pluginLayerLibrary").getAllDependencies())
                .map(Forge117Extension::evalDependency)
                .toList();

        String ignoreList = FastStream.concat(moduleOnly, gameLayerLibrary, pluginLayerLibrary)
                .map(e -> {
                    String fName = e.getFileName().toString();
                    if (!fName.startsWith("events") && !fName.startsWith("core")) {
                        fName = fName.replaceAll("([-_]([.\\d]*\\d+)|\\.jar$)", "");
                    }
                    return fName;
                })
                .join(",");
        if (!StringUtils.isEmpty(ignoreList)) {
            ignoreList += ',';
        }
        ignoreList += "client-extra,forge-";

        String mcVersion = rootProject.extraProperties.get("MC_VERSION");
        Path assetsDir = Objects.requireNonNull(context.blackboard.get(ASSETS_PATH));
        VersionInfoJson versionInfo = Objects.requireNonNull(context.blackboard.get(VERSION_INFO));

        ImmutableList.Builder<String> progArgs = ImmutableList.builder();
        progArgs.add("--gameDir", ".");
        progArgs.add("--fml.forgeVersion", forgeSubProject.version.substring(mcVersion.length() + 1).replace("-wt-local", ""));
        progArgs.add("--fml.mcVersion", mcVersion);
        progArgs.add("--fml.forgeGroup", forgeSubProject.group);
        progArgs.add("--fml.mcpVersion", rootProject.extraProperties.get("MCP_VERSION"));

        String fmlVersion = rootProject.extraProperties.get("FANCY_MOD_LOADER_VERSION");
        if (fmlVersion != null) {
            progArgs.add("--fml.fmlVersion", fmlVersion);
        }

        Map<String, String> envVars = new HashMap<>();
        envVars.put("FORGE_SPEC", forgeSubProject.extraProperties.get("SPEC_VERSION"));
        envVars.put("LAUNCHER_VERSION", forgeSubProject.extraProperties.get("SPEC_VERSION"));

        Map<String, String> sysProps = new HashMap<>();
        sysProps.put("eventbus.checkTypesOnDispatch", "true");

        sysProps.put("legacyClassPath", String.join(File.pathSeparator, runtimeClasspath));
        sysProps.put("ignoreList", ignoreList);
        sysProps.put("mergeModules", "jna-5.10.0.jar,jna-platform-5.10.0.jar"); // TODO, forge hardcodes these? We should probably extract this from their run configs.
        sysProps.put("fml.pluginLayerLibraries", of(pluginLayerLibrary).map(Path::getFileName).join(","));
        sysProps.put("fml.gameLayerLibraries", of(gameLayerLibrary).map(Path::getFileName).join(","));
        List<String> jvmArgs = ImmutableList.of(
                "-p", of(moduleOnly).join(File.pathSeparator),
                "--add-modules", "ALL-MODULE-PATH",
                "--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler",
                "--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler",
                "--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler",
                "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming"
        );

        List<String> modClasses = buildModClasses(context, new ForgeExportedData());
        modClasses.add(append("minecraft", forgeSubProjectWM.getOutput()));
        modClasses.add(append("minecraft", forgeSubProjectWM.getOutput()));
        envVars.put("MOD_CLASSES", Strings.join(modClasses, File.pathSeparatorChar));

        for (RunConfig runConfig : context.workspaceScript.getWorkspace().getRunConfigContainer().getRunConfigs().values()) {
            runConfig.envVar(envVars);
            runConfig.sysProp(sysProps);
            runConfig.vmArg(jvmArgs);
            runConfig.progArg(progArgs.build());
            String target = ((Forge114RunConfig) runConfig).getLaunchTarget();
            runConfig.progArg("--launchTarget", target);
            if (target.contains("client") || target.contains("data")) {
                runConfig.progArg("--assetsDir", assetsDir.toAbsolutePath().toString());
                runConfig.progArg("--assetIndex", versionInfo.assetIndex.id);
                runConfig.progArg("--version", "forge");
            }
        }
    }
}
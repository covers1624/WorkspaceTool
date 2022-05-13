/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge;

import com.google.common.collect.ImmutableList;
import net.covers1624.quack.collection.StreamableIterable;
import net.covers1624.quack.io.IOUtils;
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
import net.covers1624.wt.forge.api.script.Forge117;
import net.covers1624.wt.mc.data.VersionInfoJson;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.collection.StreamableIterable.of;
import static net.covers1624.wt.forge.ForgeExtension.*;

/**
 * Created by covers1624 on 27/10/21.
 */
public class Forge117Extension extends AbstractForge113PlusExtension {

    private static final Logger LOGGER = LogManager.getLogger();

    static void init() {
        ProcessModulesEvent.REGISTRY.register(Forge117Extension::onProcessModules);
        ProcessWorkspaceModulesEvent.REGISTRY.register(Forge117Extension::onProcessWorkspaceModules);
    }

    private static void onProcessModules(ProcessModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (!context.isFramework(Forge117.class)) return;

        // This is a bit of a hack, we remove the FG3+ 'minecraft' configuration from the 'implementation' configuration
        //  this configuration should only contain the Deobfuscated & Remapped Forge, and all forge + Minecraft dependencies.
        //  These dependencies are provided via the module dep on Forge and are safe to remove.
        for (Module module : event.getContext().modules) {
            module.getConfigurations().get("minecraft").getDependencies().clear();
        }
    }

    private static void onProcessWorkspaceModules(ProcessWorkspaceModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (!context.isFramework(Forge117.class)) return;

        GradleBackedModule forgeModule = findForgeRootModule(context);
        GradleBackedModule forgeSubModule = findForgeSubModule(context);
        ProjectData rootProject = forgeModule.getProjectData();
        ProjectData forgeSubProject = requireNonNull(rootProject.subProjects.get("forge"), "Missing forge submodule.");
        WorkspaceModule forgeSubProjectWM = of(context.workspaceModules)
                .filter(e -> e.getName().equals(forgeSubProject.getProjectCoords().replace(":", ".") + ".main"))
                .only();
        Map<DependencyScope, Set<Dependency>> depMap = forgeSubProjectWM.getDependencies();
        StreamableIterable<LibraryDependency> modRuntimeDeps = of(depMap.get(DependencyScope.RUNTIME))
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
        Configuration moduleOnlyConfig = forgeSubModule.getConfigurations().get("moduleonly");

        String modulePath = moduleOnlyConfig.getAllDependencies().stream()
                .map(Forge117Extension::evalDependency)
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        String ignoreList = moduleOnlyConfig.getAllDependencies().stream()
                .map(Forge117Extension::evalDependency)
                .map(e -> e.getFileName().toString().replaceAll("([-_]([.\\d]*\\d+)|\\.jar$)", ""))
                .collect(Collectors.joining(","));
        if (!StringUtils.isEmpty(ignoreList)) {
            ignoreList += ',';
        }

        String mcVersion = rootProject.extraProperties.get("MC_VERSION");
        Path assetsDir = Objects.requireNonNull(context.blackboard.get(ASSETS_PATH));
        VersionInfoJson versionInfo = Objects.requireNonNull(context.blackboard.get(VERSION_INFO));

        List<String> progArgs = ImmutableList.of(
                "--gameDir", ".",
                "--fml.forgeVersion", forgeSubProject.version.substring(mcVersion.length() + 1).replace("-wt-local", ""),
                "--fml.mcVersion", mcVersion,
                "--fml.forgeGroup", forgeSubProject.group,
                "--fml.mcpVersion", rootProject.extraProperties.get("MCP_VERSION")
        );

        Map<String, String> envVars = new HashMap<>();
        envVars.put("FORGE_SPEC", forgeSubProject.extraProperties.get("SPEC_VERSION"));
        envVars.put("LAUNCHER_VERSION", forgeSubProject.extraProperties.get("SPEC_VERSION"));

        Map<String, String> sysProps = new HashMap<>();
        sysProps.put("eventbus.checkTypesOnDispatch", "true");

        sysProps.put("legacyClassPath", String.join(File.pathSeparator, runtimeClasspath));
        sysProps.put("ignoreList", ignoreList + "client-extra,ForgeRoot_fmlcore,ForgeRoot_javafmllanguage,ForgeRoot_mclanguage");
        sysProps.put("mergeModules", "jna-5.8.0.jar,jna-platform-58.0.jar,java-objc-bridge-1.0.0.jar"); // TODO, forge hardcodes these? We should probably extract this from their run configs.
        List<String> jvmArgs = ImmutableList.of(
                "-p", modulePath,
                "--add-modules", "ALL-MODULE-PATH",
                "--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler",
                "--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler",
                "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming",
                "--add-exports", "cpw.mods.bootstraplauncher/cpw.mods.bootstraplauncher=ALL-UNNAMED"
        );

        List<String> modClasses = buildModClasses(context, new ForgeExportedData());
        modClasses.add(append("minecraft", forgeSubProjectWM.getOutput()));
        modClasses.add(append("minecraft", forgeSubProjectWM.getOutput()));
        envVars.put("MOD_CLASSES", Strings.join(modClasses, File.pathSeparatorChar));

        for (RunConfig runConfig : context.workspaceScript.getWorkspace().getRunConfigContainer().getRunConfigs().values()) {
            runConfig.envVar(envVars);
            runConfig.sysProp(sysProps);
            runConfig.vmArg(jvmArgs);
            runConfig.progArg(progArgs);
            String target = ((Forge114RunConfig) runConfig).getLaunchTarget();
            runConfig.progArg("--launchTarget", target);
            if (target.contains("client") || target.contains("data")) {
                runConfig.progArg("--assetsDir", assetsDir.toAbsolutePath().toString());
                runConfig.progArg("--assetIndex", versionInfo.assetIndex.id);
            }
        }
    }

    private static Path evalDependency(Dependency dep) {
        if (dep instanceof MavenDependency mvnDep) {
            return mvnDep.getClasses().toAbsolutePath();
        }
        if (dep instanceof LibraryDependency) {
            return evalDependency(((LibraryDependency) dep).getDependency());
        }
        if (dep instanceof WorkspaceModuleDependency) {
            return ((WorkspaceModuleDependency) dep).getModule().getOutput().toAbsolutePath();
        }
        throw new RuntimeException("Unhandled dependency: " + dep.getClass());
    }

    private static boolean isMod(Path file) {
        // Sure?
        if (!Files.isRegularFile(file)) return true;

        try (FileSystem fs = IOUtils.getJarFileSystem(file, true)) {
            if (Files.exists(fs.getPath("/META-INF/mods.toml"))) return true;
            try (InputStream is = Files.newInputStream(fs.getPath("META-INF/MANIFEST.MF"))) {
                Manifest manifest = new Manifest(is);
                if (manifest.getMainAttributes().getValue("FMLModType") != null) {
                    return true;
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Failed to determine if {} is a Forge mod.", file);
        }
        return false;
    }

}

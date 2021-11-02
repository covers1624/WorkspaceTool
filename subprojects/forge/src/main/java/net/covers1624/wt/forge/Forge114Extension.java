/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.gradle.data.ConfigurationData;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.script.module.ModuleSpec;
import net.covers1624.wt.api.script.runconfig.RunConfig;
import net.covers1624.wt.event.ProcessModulesEvent;
import net.covers1624.wt.event.ProcessProjectDataEvent;
import net.covers1624.wt.event.ProcessWorkspaceModulesEvent;
import net.covers1624.wt.forge.api.export.ForgeExportedData;
import net.covers1624.wt.forge.api.script.Forge114;
import net.covers1624.wt.forge.api.script.Forge114ModuleSpec;
import net.covers1624.wt.forge.api.script.Forge114RunConfig;
import net.covers1624.wt.forge.api.script.ModuleModsContainer;
import net.covers1624.wt.forge.gradle.data.FG3McpMappingData;
import net.covers1624.wt.mc.data.VersionInfoJson;
import net.covers1624.wt.util.Utils;
import org.apache.logging.log4j.util.Strings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static net.covers1624.wt.forge.ForgeExtension.*;

/**
 * Created by covers1624 on 27/10/21.
 */
public class Forge114Extension extends AbstractForge113PlusExtension {

    static void init() {
        ProcessModulesEvent.REGISTRY.register(Forge114Extension::onProcessModules);
        ProcessProjectDataEvent.REGISTRY.register(Forge114Extension::onProcessProjectData);
        ProcessWorkspaceModulesEvent.REGISTRY.register(Forge114Extension::onProcessWorkspaceModules);
    }

    private static void onProcessModules(ProcessModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (!context.isFramework(Forge114.class)) return;

        GradleBackedModule forgeModule = findForgeRootModule(context);
        ProjectData rootProject = forgeModule.getProjectData();
        ProjectData forgeSubProject = requireNonNull(rootProject.subProjects.get("forge"), "Missing forge submodule.");
        Map<String, String> envVars = new HashMap<>();
        String mcVersion = rootProject.extraProperties.get("MC_VERSION");
        VersionInfoJson versionInfo = context.blackboard.get(VERSION_INFO);

        envVars.put("assetIndex", versionInfo.assetIndex.id);
        envVars.put("assetDirectory", context.blackboard.get(ASSETS_PATH).toAbsolutePath().toString());
        envVars.put("MC_VERSION", mcVersion);
        envVars.put("MCP_VERSION", rootProject.extraProperties.get("MCP_VERSION"));
        envVars.put("FORGE_GROUP", forgeSubProject.group);
        envVars.put("FORGE_SPEC", forgeSubProject.extraProperties.get("SPEC_VERSION"));
        envVars.put("FORGE_VERSION", forgeSubProject.version.substring(mcVersion.length() + 1).replace("-wt-local", ""));
        envVars.put("LAUNCHER_VERSION", forgeSubProject.extraProperties.get("SPEC_VERSION"));
        for (RunConfig runConfig : context.workspaceScript.getWorkspace().getRunConfigContainer().getRunConfigs().values()) {
            runConfig.envVar(envVars);
            runConfig.envVar(Collections.singletonMap("target", ((Forge114RunConfig) runConfig).getLaunchTarget()));
        }

        // This is a bit of a hack, we remove the FG3+ 'minecraft' configuration from the 'implementation' configuration
        //  this configuration should only contain the Deobfuscated & Remapped Forge, and all forge + Minecraft dependencies.
        //  These dependencies are provided via the module dep on Forge and are safe to remove.
        for (Module module : event.getContext().modules) {
            Configuration config = module.getConfigurations().get("implementation");
            if (config == null) continue;
            config.getExtendsFrom().removeIf(e -> e.getName().equals("minecraft"));
        }
    }

    private static void onProcessProjectData(ProcessProjectDataEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (!context.isFramework(Forge114.class)) return;

        Deque<ProjectData> stack = new ArrayDeque<>();
        stack.push(event.getProjectData());
        while (!stack.isEmpty()) {
            ProjectData data = stack.pop();
            stack.addAll(data.subProjects.values());
            ConfigurationData configuration = data.configurations.get("compile");
            if (configuration != null) {
                configuration.extendsFrom.remove("minecraft");
            }
        }
    }

    private static void onProcessWorkspaceModules(ProcessWorkspaceModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (!context.isFramework(Forge114.class)) return;

        ForgeExportedData exportedData = new ForgeExportedData();
        Forge114 forge114 = (Forge114) context.workspaceScript.getFramework();
        GradleBackedModule forgeModule = findForgeRootModule(context);
        ProjectData rootProject = forgeModule.getProjectData();
        ProjectData forgeSubProject = requireNonNull(rootProject.subProjects.get("forge"), "Missing forge submodule.");
        String mcVersion = rootProject.extraProperties.get("MC_VERSION");

        exportedData.forgeRepo = forge114.getUrl();
        exportedData.forgeCommit = forge114.getCommit();
        exportedData.forgeBranch = forge114.getBranch();
        exportedData.forgeVersion = forgeSubProject.version.substring(mcVersion.length() + 1).replace("-wt-local", "");
        exportedData.mcVersion = mcVersion;
        FG3McpMappingData fg3McpMappingData = forgeModule.getProjectData().getData(FG3McpMappingData.class);
        if (fg3McpMappingData != null) {
            exportedData.fg3Data = new ForgeExportedData.FG3MCPData(fg3McpMappingData);
        }

        //TODO, generate AccessList.

        List<String> modClasses = buildModClasses(context, exportedData);
        Map<String, String> envVars = Collections.singletonMap("MOD_CLASSES", Strings.join(modClasses, File.pathSeparatorChar));
        for (RunConfig runConfig : context.workspaceScript.getWorkspace().getRunConfigContainer().getRunConfigs().values()) {
            runConfig.envVar(envVars);
        }
        Utils.toJson(exportedData, ForgeExportedData.class, context.cacheDir.resolve(ForgeExportedData.PATH));
    }

}

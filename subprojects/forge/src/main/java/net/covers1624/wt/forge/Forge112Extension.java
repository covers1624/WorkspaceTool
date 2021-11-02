/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.ScalaSdkDependency;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.script.WorkspaceScript;
import net.covers1624.wt.event.ProcessModulesEvent;
import net.covers1624.wt.forge.api.script.Forge112;
import net.covers1624.wt.forge.gradle.data.FG2Data;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static net.covers1624.wt.forge.ForgeExtension.findForgeRootModule;

/**
 * Created by covers1624 on 27/10/21.
 */
public class Forge112Extension {

    static void init() {
        ProcessModulesEvent.REGISTRY.register(Forge112Extension::onProcessModules);
    }

    //put extracted FMLCoreMods && TweakClasses into all run configurations.
    private static void onProcessModules(ProcessModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (!context.isFramework(Forge112.class)) return;

        WorkspaceScript script = context.workspaceScript;
        Set<String> fmlCoreMods = new HashSet<>();
        Set<String> tweakClasses = new HashSet<>();
        context.modules.forEach(m -> {
            if (m instanceof GradleBackedModule) {
                GradleBackedModule module = (GradleBackedModule) m;
                ProjectData projectData = module.getProjectData();
                FG2Data fgData = projectData.getData(FG2Data.class);
                if (fgData != null) {
                    fmlCoreMods.addAll(fgData.fmlCoreMods);
                    tweakClasses.addAll(fgData.tweakClasses);
                }
            }
        });
        script.getWorkspace().getRunConfigContainer().getRunConfigs().values().forEach(config -> {
            Map<String, String> sysProps = config.getSysProps();
            List<String> progArgs = config.getProgArgs();

            Set<String> coreMods = new HashSet<>(fmlCoreMods);
            String existing = sysProps.get("fml.coreMods.load");
            if (StringUtils.isNotEmpty(existing)) {
                Collections.addAll(coreMods, existing.split(","));
            }
            if (!coreMods.isEmpty()) {
                sysProps.put("fml.coreMods.load", String.join(",", coreMods));
            }
            if (!tweakClasses.isEmpty()) {
                tweakClasses.forEach(tweak -> {
                    progArgs.add("--tweakClass");
                    progArgs.add(tweak);
                });
            }
        });
        GradleBackedModule forgeModule = findForgeRootModule(context);
        Configuration config = forgeModule.getSourceSets().get("main").getCompileConfiguration();
        Optional<ScalaSdkDependency> dep = config.getAllDependencies().stream()
                .filter(e -> e instanceof ScalaSdkDependency)
                .map(e -> (ScalaSdkDependency) e)
                .findFirst();
        dep.ifPresent(scalaSdk -> {
            context.modules.forEach(e -> {
                e.getSourceSets().get("main").getCompileConfiguration().addDependency(scalaSdk);
            });
        });
    }
}

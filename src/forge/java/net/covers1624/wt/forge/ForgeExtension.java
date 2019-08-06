package net.covers1624.wt.forge;

import net.covers1624.wt.api.Extension;
import net.covers1624.wt.api.ExtensionDetails;
import net.covers1624.wt.api.data.GradleData;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.ModuleList;
import net.covers1624.wt.api.script.WorkspaceScript;
import net.covers1624.wt.event.*;
import net.covers1624.wt.forge.api.impl.Forge112Impl;
import net.covers1624.wt.forge.api.script.Forge112;
import net.covers1624.wt.forge.gradle.data.ForgeGradleData;
import net.covers1624.wt.forge.gradle.data.McpMappingData;
import net.covers1624.wt.forge.remap.DependencyRemapper;
import net.covers1624.wt.forge.remap.JarRemapper;
import net.covers1624.wt.forge.remap.SRGToMCPRemapper;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.util.*;

/**
 * All Forge related handling for WorkspaceTool
 * Created by covers1624 on 17/6/19.
 */
@ExtensionDetails (name = "Forge", desc = "Provides integration for the MinecraftForge modding framework.")
public class ForgeExtension implements Extension {

    private DependencyRemapper remapper;

    @Override
    public void load() {
        InitializationEvent.REGISTRY.register(this::onInitialization);
        PrepareScriptEvent.REGISTRY.register(this::onPrepareScript);
        ModuleHashCheckEvent.REGISTRY.register(this::onModuleHashCheck);
        ProcessModulesEvent.REGISTRY.register(this::onProcessModules);
        RunConfigModuleEvent.REGISTRY.register(this::onRunConfigModuleEvent);
        ProcessDependencyEvent.REGISTRY.register(this::onProcessDependency);
    }

    private void onInitialization(InitializationEvent event) {
        FrameworkRegistry registry = event.getFrameworkRegistry();
        GradleManager gradleManager = event.getGradleManager();

        registry.registerScriptImpl(Forge112.class, Forge112Impl::new);
        registry.registerFrameworkHandler(Forge112.class, Forge112FrameworkHandler::new);

        gradleManager.includeClassMarker("net.covers1624.wt.forge.gradle.ForgeGradleDataBuilder");
        gradleManager.addDataBuilder("net.covers1624.wt.forge.gradle.ForgeGradleDataBuilder");
        gradleManager.executeBefore("genSrgs", "mergeJars");
    }

    private void onPrepareScript(PrepareScriptEvent event) {
        ImportCustomizer customizer = new ImportCustomizer();
        customizer.addStarImports("net.covers1624.wt.forge.api.script");
        event.getCompilerConfiguration().addCompilationCustomizers(customizer);
    }

    private void onModuleHashCheck(ModuleHashCheckEvent event) {
        event.putVersionedClass("net.covers1624.wt.forge.gradle.data.ForgeGradleData");
        event.putVersionedClass("net.covers1624.wt.forge.gradle.data.ForgeGradlePluginData");
        event.putVersionedClass("net.covers1624.wt.forge.gradle.data.McpMappingData");
        event.putVersionedClass("net.covers1624.wt.forge.gradle.ForgeGradleDataBuilder");
    }

    //put extracted FMLCoreMods && TweakClasses into all run configurations.
    private void onProcessModules(ProcessModulesEvent event) {
        ModuleList moduleList = event.getModuleList();
        WorkspaceScript script = event.getScript();
        Set<String> fmlCoreMods = new HashSet<>();
        Set<String> tweakClasses = new HashSet<>();
        moduleList.modules.forEach(m -> {
            if (m instanceof GradleBackedModule) {
                GradleBackedModule module = (GradleBackedModule) m;
                GradleData gradleData = module.getGradleData();
                ForgeGradleData fgData = gradleData.getData(ForgeGradleData.class);
                if (fgData != null) {
                    fmlCoreMods.addAll(fgData.fmlCoreMods);
                    tweakClasses.addAll(fgData.tweakClasses);
                }

            }
        });
        script.getRunConfigContainer().getRunConfigs().values().forEach(config -> {
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
    }

    private void onRunConfigModuleEvent(RunConfigModuleEvent event) {
        for (Module module : event.getModuleList().frameworkModules) {
            if (module.getName().equals("Forge")) {
                event.setResult(module);
                break;
            }
        }
    }

    private void onProcessDependency(ProcessDependencyEvent event) {
        if (remapper == null) {
            GradleBackedModule forgeModule = (GradleBackedModule) event.getModuleList().frameworkModules.stream()//
                    .filter(e -> e.getName().equals("Forge"))//
                    .findFirst()//
                    .orElseThrow(() -> new RuntimeException("Missing Forge module."));
            GradleData gradleData = forgeModule.getGradleData();
            McpMappingData mappingData = gradleData.getData(McpMappingData.class);
            if (mappingData == null) {
                throw new RuntimeException("Forge module missing mapping data.");
            }
            remapper = new DependencyRemapper(event.getCacheDir(), new JarRemapper(new SRGToMCPRemapper(mappingData)));
        }
        Configuration config = event.getDependencyConfig();
        Dependency dep = event.getDependency();
        if (dep instanceof MavenDependency) {
            MavenDependency mvnDep = (MavenDependency) dep;
            if (mvnDep.getNotation().group.startsWith("deobf.")) {
                event.setResult(null);
            } else if (config.getName().equals("deobfCompile") || config.getName().equals("deobfProvided")) {
                event.setResult(remapper.process(mvnDep));
            }
        }
    }
}

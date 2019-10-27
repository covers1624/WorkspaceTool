package net.covers1624.wt.forge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.covers1624.wt.api.Extension;
import net.covers1624.wt.api.ExtensionDetails;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.data.ConfigurationData;
import net.covers1624.wt.api.data.ProjectData;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.script.WorkspaceScript;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.script.module.ModuleSpec;
import net.covers1624.wt.api.script.runconfig.RunConfig;
import net.covers1624.wt.event.*;
import net.covers1624.wt.forge.api.impl.Forge112Impl;
import net.covers1624.wt.forge.api.impl.Forge114Impl;
import net.covers1624.wt.forge.api.impl.Forge114ModuleSpecTemplate;
import net.covers1624.wt.forge.api.impl.Forge114RunConfigTemplate;
import net.covers1624.wt.forge.api.script.*;
import net.covers1624.wt.forge.gradle.FGDataBuilder;
import net.covers1624.wt.forge.gradle.FGVersion;
import net.covers1624.wt.forge.gradle.data.FG2Data;
import net.covers1624.wt.forge.gradle.data.FG2McpMappingData;
import net.covers1624.wt.forge.gradle.data.FGPluginData;
import net.covers1624.wt.forge.remap.DependencyRemapper;
import net.covers1624.wt.forge.remap.JarRemapper;
import net.covers1624.wt.forge.remap.SRGToMCPRemapper;
import net.covers1624.wt.mc.data.VersionInfoJson;
import net.covers1624.wt.util.TypedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * All Forge related handling for WorkspaceTool
 * Created by covers1624 on 17/6/19.
 */
@ExtensionDetails (name = "Forge", desc = "Provides integration for the MinecraftForge modding framework.")
public class ForgeExtension implements Extension {

    public static final TypedMap.Key<VersionInfoJson> VERSION_INFO = new TypedMap.Key<>("forge:version_info");
    public static final TypedMap.Key<Path> ASSETS_PATH = new TypedMap.Key<>("forge:assets_path");

    private DependencyRemapper remapper;

    @Override
    public void load() {
        InitializationEvent.REGISTRY.register(this::onInitialization);
        PrepareScriptEvent.REGISTRY.register(this::onPrepareScript);
        ModuleHashCheckEvent.REGISTRY.register(this::onModuleHashCheck);
        ProcessModulesEvent.REGISTRY.register(this::onProcessModules112);
        ProcessDependencyEvent.REGISTRY.register(this::onProcessDependency112);
        ScriptWorkspaceEvalEvent.REGISTRY.register(this::onScriptWorkspaceEvalEvent);

        ProcessModulesEvent.REGISTRY.register(this::onProcessModules114);
        ProcessProjectDataEvent.REGISTRY.register(this::onProcessProjectData114);
        ProcessWorkspaceModulesEvent.REGISTRY.register(this::onProcessWorkspaceModules114);
    }

    private void onInitialization(InitializationEvent event) {
        WorkspaceToolContext context = event.getContext();
        FrameworkRegistry registry = context.frameworkRegistry;
        GradleManager gradleManager = context.gradleManager;

        registry.registerScriptImpl(Forge112.class, Forge112Impl::new);
        registry.registerScriptImpl(Forge114.class, Forge114Impl::new);

        registry.registerFrameworkHandler(Forge112.class, Forge112FrameworkHandler::new);
        registry.registerFrameworkHandler(Forge114.class, Forge114FrameworkHandler::new);

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
        event.putVersionedClass(FG2McpMappingData.class);
        event.putVersionedClass(FGPluginData.class);
        event.putVersionedClass(FG2Data.class);
        event.putVersionedClass(FGDataBuilder.class);
        event.putVersionedClass(FGVersion.class);
    }

    private void onScriptWorkspaceEvalEvent(ScriptWorkspaceEvalEvent event) {
        if (event.getScript().getFrameworkClass() == Forge114.class) {
            MixinInstantiator mixinInstantiator = event.getMixinInstantiator();
            mixinInstantiator.addMixinClass(RunConfig.class, Forge114RunConfig.class, Forge114RunConfigTemplate.class);
            mixinInstantiator.addMixinClass(ModuleSpec.class, Forge114ModuleSpec.class, Forge114ModuleSpecTemplate.class);
        }
    }

    //put extracted FMLCoreMods && TweakClasses into all run configurations.
    private void onProcessModules112(ProcessModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        WorkspaceScript script = context.workspaceScript;
        if (script.getFrameworkClass().equals(Forge112.class)) {
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
        }
    }

    private void onProcessDependency112(ProcessDependencyEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (context.workspaceScript.getFrameworkClass().equals(Forge112.class)) {
            if (remapper == null) {
                GradleBackedModule forgeModule = findForgeModule(context);
                ProjectData projectData = forgeModule.getProjectData();
                FG2McpMappingData mappingData = projectData.getData(FG2McpMappingData.class);
                if (mappingData == null) {
                    throw new RuntimeException("Forge module missing mapping data.");
                }
                remapper = new DependencyRemapper(context.cacheDir, new JarRemapper(new SRGToMCPRemapper(mappingData)));
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

    private void onProcessModules114(ProcessModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (context.workspaceScript.getFrameworkClass().equals(Forge114.class)) {
            GradleBackedModule forgeModule = findForgeModule(context);
            ProjectData rootProject = forgeModule.getProjectData();
            ProjectData forgeSubProject = rootProject.subProjects.stream()//
                    .filter(e -> e.name.equals("forge"))//
                    .findFirst()//
                    .orElseThrow(() -> new RuntimeException("Missing forge submodule."));
            Map<String, String> envVars = new HashMap<>();
            String mcVer = rootProject.extraProperties.get("MC_VERSION");
            VersionInfoJson versionInfo = context.blackboard.get(VERSION_INFO);

            envVars.put("assetIndex", versionInfo.assetIndex.id);
            envVars.put("assetDirectory", context.blackboard.get(ASSETS_PATH).toAbsolutePath().toString());
            envVars.put("MC_VERSION", mcVer);
            envVars.put("MCP_VERSION", rootProject.extraProperties.get("MCP_VERSION"));
            envVars.put("FORGE_GROUP", forgeSubProject.group);
            envVars.put("FORGE_SPEC", forgeSubProject.extraProperties.get("SPEC_VERSION"));
            //TODO, This strips the minecraft version from the beginning, perhaps strip branch name off the end?
            envVars.put("FORGE_VERSION", forgeSubProject.version.substring(mcVer.length() + 1).replace("-wt-local", ""));
            envVars.put("LAUNCHER_VERSION", forgeSubProject.extraProperties.get("SPEC_VERSION"));
            for (RunConfig runConfig : context.workspaceScript.getWorkspace().getRunConfigContainer().getRunConfigs().values()) {
                runConfig.envVar(envVars);
                runConfig.envVar(Collections.singletonMap("target", ((Forge114RunConfig) runConfig).getLaunchTarget()));
            }
        }
    }

    private void onProcessProjectData114(ProcessProjectDataEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (context.workspaceScript.getFrameworkClass().equals(Forge114.class)) {
            Deque<ProjectData> stack = new ArrayDeque<>();
            stack.push(event.getProjectData());
            while (!stack.isEmpty()) {
                ProjectData data = stack.pop();
                stack.addAll(data.subProjects);
                ConfigurationData configuration = data.configurations.get("compile");
                if (configuration != null) {
                    configuration.extendsFrom.remove("minecraft");
                }
            }
        }
    }

    private void onProcessWorkspaceModules114(ProcessWorkspaceModulesEvent event) {
        WorkspaceToolContext context = event.getContext();
        if (context.workspaceScript.getFrameworkClass().equals(Forge114.class)) {
            List<String> modClasses = new ArrayList<>();

            ModuleContainerSpec moduleContainerSpec = context.workspaceScript.getModuleContainer();
            Map<String, ModuleSpec> customModules = moduleContainerSpec.getCustomModules();
            context.workspaceModules.stream().filter(e -> !e.getIsGroup()).forEach(module -> {
                int lastDot = module.getName().lastIndexOf(".");
                String moduleName = module.getName().substring(0, lastDot);
                String sourceSet = module.getName().substring(lastDot + 1);
                Forge114ModuleSpec spec = (Forge114ModuleSpec) customModules.get(moduleName.replace(".", "/"));
                if (spec != null) {
                    ModuleModsContainer moduleMods = spec.getForgeModuleModsContainer();
                    moduleMods.getModSourceSets().entrySet().stream()//
                            .filter(e -> e.getValue().equals(sourceSet))//
                            .forEach(e -> {
                                //Add twice, forge doesnt expect sources and resources to be in the same folder??
                                modClasses.add(append(e.getKey(), module.getOutput()));
                                modClasses.add(append(e.getKey(), module.getOutput()));
                            });
                } else if (sourceSet.equals("main")) {
                    for (Path resourceDir : module.getResources()) {
                        Path modsToml = resourceDir.resolve("META-INF/mods.toml");
                        if (Files.exists(modsToml)) {
                            try (FileConfig config = FileConfig.builder(modsToml).build()) {
                                config.load();
                                if (config.contains("mods") && !(config.get("mods") instanceof Collection)) {
                                    throw new RuntimeException(String.format("ModsToml file %s expected mods as list.", modsToml));
                                }
                                List<UnmodifiableConfig> modConfigs = config.getOrElse("mods", ArrayList::new);
                                modConfigs.stream()//
                                        .map(mi -> mi.get("modId"))//
                                        .map(e -> (String) e)//
                                        .filter(Objects::nonNull)//
                                        .forEach(mod -> {
                                            //Add twice, forge doesnt expect sources and resources to be in the same folder??
                                            modClasses.add(append(mod, module.getOutput()));
                                            modClasses.add(append(mod, module.getOutput()));
                                        });
                            }
                        }
                    }
                }
            });

            Map<String, String> envVars = Collections.singletonMap("MOD_CLASSES", Strings.join(modClasses, File.pathSeparatorChar));
            for (RunConfig runConfig : context.workspaceScript.getWorkspace().getRunConfigContainer().getRunConfigs().values()) {
                runConfig.envVar(envVars);
            }
        }
    }

    private static String append(String mod, Path path) {
        return mod + "%%" + path.toAbsolutePath().toString();
    }

    private GradleBackedModule findForgeModule(WorkspaceToolContext context) {
        return (GradleBackedModule) context.frameworkModules.stream()//
                .filter(e -> e.getName().equals("Forge"))//
                .findFirst()//
                .orElseThrow(() -> new RuntimeException("Missing Forge module."));
    }
}

package net.covers1624.wt.forge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.covers1624.wt.api.Extension;
import net.covers1624.wt.api.ExtensionDetails;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.dependency.ScalaSdkDependency;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.gradle.data.ConfigurationData;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.api.script.WorkspaceScript;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.script.module.ModuleSpec;
import net.covers1624.wt.api.script.runconfig.RunConfig;
import net.covers1624.wt.event.*;
import net.covers1624.wt.forge.api.export.ForgeExportedData;
import net.covers1624.wt.forge.api.impl.Forge112Impl;
import net.covers1624.wt.forge.api.impl.Forge114Impl;
import net.covers1624.wt.forge.api.impl.Forge114ModuleSpecTemplate;
import net.covers1624.wt.forge.api.impl.Forge114RunConfigTemplate;
import net.covers1624.wt.forge.api.script.*;
import net.covers1624.wt.forge.gradle.FGDataBuilder;
import net.covers1624.wt.forge.gradle.FGVersion;
import net.covers1624.wt.forge.gradle.data.FG2Data;
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
import net.covers1624.wt.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.stream.Collectors.toList;

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
        ProcessModulesEvent.REGISTRY.register(this::onProcessModules112);
        ProcessDependencyEvent.REGISTRY.register(this::onProcessDependency);
        ProcessModulesEvent.REGISTRY.register(this::processModules);
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
            GradleBackedModule forgeModule = findForgeModule(context);
            Configuration config = forgeModule.getSourceSets().get("main").getCompileConfiguration();
            Optional<ScalaSdkDependency> dep = config.getAllDependencies().stream()//
                    .filter(e -> e instanceof ScalaSdkDependency)//
                    .map(e -> (ScalaSdkDependency) e)//
                    .findFirst();
            dep.ifPresent(scalaSdk -> {
                context.modules.forEach(e -> {
                    e.getSourceSets().get("main").getCompileConfiguration().addDependency(scalaSdk);
                });
            });
        }
    }

    private void onProcessDependency(ProcessDependencyEvent event) {
        WorkspaceToolContext context = event.getContext();
        //Used as latch to compute. Empty means something else from null.
        //noinspection OptionalAssignedToNull
        if (event.getContext().workspaceScript.getFramework() instanceof ForgeFramework) {
            if (remapper == null) {
                GradleBackedModule forgeModule = findForgeModule(context);
                ProjectData projectData = forgeModule.getProjectData();
                FGPluginData pluginData = projectData.pluginData.getData(FGPluginData.class);
                if (pluginData != null && pluginData.version.isFg2()) {
                    FG2McpMappingData mappingData = projectData.getData(FG2McpMappingData.class);
                    if (mappingData == null) {
                        throw new RuntimeException("Forge module missing mapping data.");
                    }
                    remapper = Optional.of(new DependencyRemapper(context.cacheDir, new JarRemapper(new SRGToMCPRemapper(mappingData))));
                } else {
                    ProjectData forgeSubModuleData = projectData.subProjects.stream()//
                            .filter(e -> e.name.equals("forge"))//
                            .findFirst()//
                            .orElseThrow(() -> new RuntimeException("'forge' submodule not found on Forge project."));
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
                Configuration config = event.getDependencyConfig();
                if (dep instanceof MavenDependency) {
                    MavenDependency mvnDep = (MavenDependency) dep;
                    if (mvnDep.getNotation().group.startsWith("deobf.")) {
                        event.setResult(null);
                    } else if (config.getName().equals("deobfCompile") || config.getName().equals("deobfProvided")) {
                        event.setResult(remapper.process(mvnDep));
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
            String mcVersion = rootProject.extraProperties.get("MC_VERSION");
            VersionInfoJson versionInfo = context.blackboard.get(VERSION_INFO);

            envVars.put("assetIndex", versionInfo.assetIndex.id);
            envVars.put("assetDirectory", context.blackboard.get(ASSETS_PATH).toAbsolutePath().toString());
            envVars.put("MC_VERSION", mcVersion);
            envVars.put("MCP_VERSION", rootProject.extraProperties.get("MCP_VERSION"));
            envVars.put("FORGE_GROUP", forgeSubProject.group);
            envVars.put("FORGE_SPEC", forgeSubProject.extraProperties.get("SPEC_VERSION"));
            //TODO, This strips the minecraft version from the beginning, perhaps strip branch name off the end?
            envVars.put("FORGE_VERSION", forgeSubProject.version.substring(mcVersion.length() + 1).replace("-wt-local", ""));
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
            ForgeExportedData exportedData = new ForgeExportedData();
            Forge114 forge114 = (Forge114) context.workspaceScript.getFramework();
            GradleBackedModule forgeModule = findForgeModule(context);
            ProjectData rootProject = forgeModule.getProjectData();
            ProjectData forgeSubProject = rootProject.subProjects.stream()//
                    .filter(e -> e.name.equals("forge"))//
                    .findFirst()//
                    .orElseThrow(() -> new RuntimeException("Missing forge submodule."));
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
                                ForgeExportedData.ModData modData = new ForgeExportedData.ModData();
                                modData.moduleId = module.getName();
                                modData.modId = e.getKey();
                                modData.moduleName = moduleName;
                                modData.sourceSet = sourceSet;
                                module.getSourceMap().forEach((k, v) -> modData.sources.put(k, v.stream().map(Path::toFile).collect(toList())));
                                modData.sources.put("resources", module.getResources().stream().map(Path::toFile).collect(toList()));
                                modData.output = module.getOutput().toFile();
                                exportedData.mods.add(modData);
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
                                        .forEach(modId -> {
                                            ForgeExportedData.ModData modData = new ForgeExportedData.ModData();
                                            modData.moduleId = module.getName();
                                            modData.modId = modId;
                                            modData.moduleName = moduleName;
                                            modData.sourceSet = sourceSet;
                                            module.getSourceMap().forEach((k, v) -> modData.sources.put(k, v.stream().map(Path::toFile).collect(toList())));
                                            modData.sources.put("resources", module.getResources().stream().map(Path::toFile).collect(toList()));
                                            modData.output = module.getOutput().toFile();
                                            exportedData.mods.add(modData);

                                            //Add twice, forge doesnt expect sources and resources to be in the same folder??
                                            modClasses.add(append(modId, module.getOutput()));
                                            modClasses.add(append(modId, module.getOutput()));
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
            Utils.toJson(exportedData, ForgeExportedData.class, context.cacheDir.resolve(ForgeExportedData.PATH));
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

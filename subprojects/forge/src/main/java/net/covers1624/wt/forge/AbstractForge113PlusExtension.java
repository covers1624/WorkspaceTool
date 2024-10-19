/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.forge.api.export.ForgeExportedData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Created by covers1624 on 27/10/21.
 */
public class AbstractForge113PlusExtension {

    static List<String> buildModClasses(WorkspaceToolContext context, ForgeExportedData exportedData) {
        List<String> modClasses = new ArrayList<>();
        context.workspaceModules.stream()
                .filter(e -> !e.getIsGroup() && !e.getName().startsWith("Forge.") && !e.getName().startsWith("ForgeRoot.") && !e.getName().startsWith("NeoForge."))
                .forEach(module -> {
                    int lastDot = module.getName().lastIndexOf(".");
                    String moduleName = module.getName().substring(0, lastDot);
                    String sourceSet = module.getName().substring(lastDot + 1);
                    for (Path resourceDir : module.getResources()) {
                        Path modsToml = resourceDir.resolve("META-INF/mods.toml");
                        Path neoModsToml = resourceDir.resolve("META-INF/neoforge.mods.toml");
                        if (!Files.exists(modsToml)) {
                            if (!Files.exists(neoModsToml)) continue;
                            modsToml = neoModsToml;
                        }
                        try (FileConfig config = FileConfig.builder(modsToml).build()) {
                            config.load();
                            if (config.contains("mods") && !(config.get("mods") instanceof Collection)) {
                                throw new RuntimeException(String.format("ModsToml file %s expected mods as list.", modsToml));
                            }
                            List<UnmodifiableConfig> modConfigs = config.getOrElse("mods", ArrayList::new);
                            modConfigs.stream()
                                    .map(mi -> mi.get("modId"))
                                    .map(e -> (String) e)
                                    .filter(Objects::nonNull)
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
                });
        return modClasses;
    }

    static String append(String mod, Path path) {
        return mod + "%%" + path.toAbsolutePath();
    }
}

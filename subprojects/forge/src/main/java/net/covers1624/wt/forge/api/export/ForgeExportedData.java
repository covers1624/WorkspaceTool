/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.api.export;

import com.google.gson.annotations.SerializedName;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.forge.gradle.data.FG2McpMappingData;
import net.covers1624.wt.forge.gradle.data.FG3McpMappingData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 14/11/19.
 */
public class ForgeExportedData {

    public static final String PATH = "forge_exported_data.json";

    public String forgeRepo;
    public String forgeCommit;
    public String forgeBranch;

    public String forgeVersion;
    public String mcVersion;

    @SerializedName ("fg2_data")
    public FG2MCPData fg2Data;

    @SerializedName ("fg3_data")
    public FG3MCPData fg3Data;

    public File accessList;
    public List<ModData> mods = new ArrayList<>();

    public static class ModData {

        public String moduleId;
        public String modId;
        public String moduleName;
        public String sourceSet;
        public Map<String, List<File>> sources = new HashMap<>();
        public File output;
    }

    public static class FG2MCPData {

        public MavenNotation mappingsArtifact;
        public MavenNotation mcpDataArtifact;

        public File mappings;
        public File data;

        public File mergedJar;
        public File notchToSrg;
        public File notchToMcp;
        public File mcpToNotch;
        public File srgToMcp;
        public File mcpToSrg;

        public FG2MCPData(FG2McpMappingData data) {
            mappingsArtifact = data.mappingsArtifact;
            mcpDataArtifact = data.mcpDataArtifact;
            mappings = data.mappings;
            this.data = data.data;
            mergedJar = data.mergedJar;
            notchToSrg = data.notchToSrg;
            notchToMcp = data.notchToMcp;
            mcpToNotch = data.mcpToNotch;
            srgToMcp = data.srgToMcp;
            mcpToSrg = data.mcpToSrg;
        }
    }

    public static class FG3MCPData {

        public MavenNotation mappingsArtifact;
        public File mappingsZip;

        public FG3MCPData(FG3McpMappingData data) {
            mappingsArtifact = data.mappingsArtifact;
            mappingsZip = data.mappingsZip;
        }
    }

}

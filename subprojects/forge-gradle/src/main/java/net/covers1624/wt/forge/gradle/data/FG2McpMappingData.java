/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.gradle.data;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.event.VersionedClass;
import net.covers1624.wt.api.gradle.data.ExtraData;

import java.io.File;

/**
 * Created by covers1624 on 31/05/19.
 */
@VersionedClass (1)
public class FG2McpMappingData implements ExtraData {

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

}

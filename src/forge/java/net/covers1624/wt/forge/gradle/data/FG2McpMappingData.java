package net.covers1624.wt.forge.gradle.data;

import net.covers1624.wt.api.gradle.data.ExtraData;
import net.covers1624.wt.event.VersionedClass;
import net.covers1624.wt.util.MavenNotation;

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

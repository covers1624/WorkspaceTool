package net.covers1624.wt.forge.gradle.data;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.gradle.data.ExtraData;
import net.covers1624.wt.event.VersionedClass;

import java.io.File;

/**
 * Created by covers1624 on 14/11/19.
 */
@VersionedClass (1)
public class FG3McpMappingData implements ExtraData {

    public MavenNotation mappingsArtifact;
    public File mappingsZip;

}

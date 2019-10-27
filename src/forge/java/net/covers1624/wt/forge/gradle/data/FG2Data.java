package net.covers1624.wt.forge.gradle.data;

import net.covers1624.wt.api.data.ExtraData;
import net.covers1624.wt.event.VersionedClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by covers1624 on 31/05/19.
 */
@VersionedClass (1)
public class FG2Data implements ExtraData {

    public String mcpMappings;
    public String mcVersion;
    public String forgeVersion;

    public List<String> fmlCoreMods = new ArrayList<>();
    public List<String> tweakClasses = new ArrayList<>();
}

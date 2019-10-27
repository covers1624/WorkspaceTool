package net.covers1624.wt.forge.gradle.data;

import net.covers1624.wt.api.data.ExtraData;
import net.covers1624.wt.event.VersionedClass;

import java.io.File;

/**
 * Created by covers1624 on 9/8/19.
 */
@VersionedClass (1)
public class FG3RunData {

    public String name;
    public String assetIndex;
    public File assetsDirectory;
    public File nativesDirectory;

}

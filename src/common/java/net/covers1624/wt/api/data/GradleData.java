package net.covers1624.wt.api.data;

import net.covers1624.wt.event.VersionedClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Data model for data extracted from Gradle.
 *
 * Created by covers1624 on 31/05/19.
 */
@VersionedClass (1)
public class GradleData extends ExtraDataExtensible {

    public String group;
    public String archivesBaseName;

    public Map<String, ConfigurationData> configurations = new HashMap<>();
    public Map<String, SourceSetData> sourceSets = new HashMap<>();

}

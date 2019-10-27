package net.covers1624.wt.api.module;

import java.nio.file.Path;
import java.util.Map;

/**
 * Created by covers1624 on 24/05/19.
 */
public interface Module {

    /**
     * @return The name for the Module.
     */
    String getName();

    /**
     * @return The Path to this Module's root.
     */
    Path getPath();

    @Deprecated
    Path getCompileOutput();

    @Deprecated
    void setCompileOutput(Path compileOutput);

    /**
     * @return Any SourceSets this Module has.
     */
    Map<String, SourceSet> getSourceSets();

    /**
     * Adds a SourceSet to this Module.
     *
     * @param name      The name for the SourceSet.
     * @param sourceSet The SourceSet.
     */
    void addSourceSet(String name, SourceSet sourceSet);

    /**
     * Replaces all SourceSets this Module has with the provided Map.
     *
     * @param sourceSets The SourceSet Map.
     */
    void setSourceSets(Map<String, SourceSet> sourceSets);

    /**
     * @return Any Configurations this Module has.
     */
    Map<String, Configuration> getConfigurations();

    /**
     * Adds a Configuration to this Module.
     *
     * @param name          The name for the Configuration.
     * @param configuration The Configuration.
     */
    void addConfiguration(String name, Configuration configuration);

    /**
     * Replaces all Configurations this Module has with the provided Map.
     *
     * @param configurations The Configuration Map.
     */
    void setConfigurations(Map<String, Configuration> configurations);

    boolean getModulePerSourceSet();

    void setModulePerSourceSet(boolean value);
}

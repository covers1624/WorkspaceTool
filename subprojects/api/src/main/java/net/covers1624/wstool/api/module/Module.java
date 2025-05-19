package net.covers1624.wstool.api.module;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Represents a module/project.
 * <p>
 * Created by covers1624 on 2/3/25.
 */
public interface Module {

    /**
     * @return The root directory of this module.
     */
    Path rootDir();

    /**
     * @return The name of this module.
     */
    String name();

    /**
     * @return The submodules of this module.
     */
    Map<String, ? extends Module> subModules();

    /**
     * @return The source sets within this module.
     */
    Map<String, ? extends SourceSet> sourceSets();

    /**
     * @return A list of paths within this module that should be ignored by the workspace/ide.
     */
    List<Path> excludes();

    /**
     * Create a new submodule from this module.
     *
     * @param path The path to this submodule.
     * @param name The name of this submodule.
     * @return The new submodule.
     */
    Module newSubModule(Path path, String name);

    /**
     * Create a new {@link SourceSet} in this module.
     *
     * @param name The name of the {@link SourceSet}.
     * @return The {@link SourceSet}.
     */
    SourceSet newSourceSet(String name);

}

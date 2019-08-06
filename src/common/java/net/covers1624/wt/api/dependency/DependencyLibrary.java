package net.covers1624.wt.api.dependency;

import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.util.MavenNotation;

import java.util.Map;

/**
 * A library of dependencies. That's all really..
 *
 * Created by covers1624 on 20/7/19.
 */
public interface DependencyLibrary {

    /**
     * @return All the dependencies contained in this library.
     */
    Map<MavenNotation, LibraryDependency> getDependencies();

    void consume(Module module);

    /**
     * Adds a MavenDependency to this library.
     * Either returns an existing LibraryDependency or creates a new one.
     *
     * @param mavenDep The MavenDependency to add.
     * @return
     */
    LibraryDependency resolve(MavenDependency mavenDep);
}

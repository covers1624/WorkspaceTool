package net.covers1624.wstool.api.workspace;

import net.covers1624.quack.maven.MavenNotation;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 2/3/25.
 */
public sealed interface Dependency permits Dependency.MavenDependency, Dependency.SourceSetDependency, Dependency.FileDependency {

    boolean exported();

    record MavenDependency(MavenNotation notation, Map<String, Path> files, boolean exported) implements Dependency { }

    record SourceSetDependency(SourceSet sourceSet, boolean exported) implements Dependency { }

    record FileDependency(Path file, List<Path> sources, boolean exported) implements Dependency { }
}

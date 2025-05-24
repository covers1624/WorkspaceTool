package net.covers1624.wstool.api.workspace;

import net.covers1624.quack.maven.MavenNotation;

import java.nio.file.Path;
import java.util.Map;

/**
 * Created by covers1624 on 2/3/25.
 */
public sealed interface Dependency permits Dependency.MavenDependency, Dependency.SourceSetDependency {

    record MavenDependency(MavenNotation notation, Map<String, Path> files) implements Dependency { }

    record SourceSetDependency(SourceSet sourceSet) implements Dependency { }
}

package net.covers1624.wstool.api.workspace;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Created by covers1624 on 10/25/26.
 */
public interface DependencyResolver {

    @Nullable String lookupName(Dependency.MavenDependency dep);

    @Nullable Path resolvePath(Dependency dep);
}

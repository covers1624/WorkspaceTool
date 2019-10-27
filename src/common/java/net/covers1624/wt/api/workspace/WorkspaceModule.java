package net.covers1624.wt.api.workspace;

import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.DependencyScope;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by covers1624 on 3/9/19.
 */
public interface WorkspaceModule {

    Path getPath();

    String getName();

    boolean getIsGroup();

    //java -> paths
    //scala -> paths
    //groovy -> paths
    Map<String, List<Path>> getSourceMap();

    //Resources.
    List<Path> getResources();

    Path getOutput();

    Map<DependencyScope, Set<Dependency>> getDependencies();

}

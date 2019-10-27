package net.covers1624.wt.intellij.api.impl

import net.covers1624.wt.api.dependency.Dependency
import net.covers1624.wt.api.dependency.DependencyScope
import net.covers1624.wt.api.workspace.WorkspaceModule
import net.covers1624.wt.intellij.api.workspace.IJWorkspaceModule

import java.nio.file.Path

/**
 * Created by covers1624 on 3/9/19.
 */
//Groovy bug, WorkspaceModule and IJWorkspaceModule need to be specified since the latter has a default method.
class IJWorkspaceModuleImpl implements WorkspaceModule, IJWorkspaceModule {

    private Path path
    private String name
    private boolean isGroup
    private Map<String, List<Path>> sourceMap = [:]
    private List<Path> resources = []
    private Path output
    private Map<DependencyScope, Set<Dependency>> dependencies = [:]
    private IJWorkspaceModule parent
    private List<IJWorkspaceModule> children = []
    private String sourceSetName

    @Override
    Path getPath() {
        return path
    }

    @Override
    String getName() {
        return name
    }

    @Override
    boolean getIsGroup() {
        return isGroup
    }

    @Override
    Map<String, List<Path>> getSourceMap() {
        return sourceMap
    }

    @Override
    List<Path> getResources() {
        return resources
    }

    @Override
    Path getOutput() {
        return output
    }

    @Override
    Map<DependencyScope, Set<Dependency>> getDependencies() {
        return dependencies
    }

    @Override
    IJWorkspaceModule getParent() {
        return parent
    }

    @Override
    List<IJWorkspaceModule> getChildren() {
        return children
    }

    @Override
    String getSourceSetName() {
        return sourceSetName
    }
}

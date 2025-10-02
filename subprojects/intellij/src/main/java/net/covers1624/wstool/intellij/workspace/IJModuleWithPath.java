package net.covers1624.wstool.intellij.workspace;

import java.nio.file.Path;

/**
 * Created by covers1624 on 5/7/25.
 */
public abstract sealed class IJModuleWithPath extends IJModule permits IJProjectModule, IJWorkspace.RootModule{

    private final Path rootDir;

    public IJModuleWithPath(Path rootDir, ModulePath path) {
        super(path);
        this.rootDir = rootDir;
    }

    public Path rootDir() {
        return rootDir;
    }
}

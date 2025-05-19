package net.covers1624.wstool.intellij.module;

import java.nio.file.Path;

/**
 * Created by covers1624 on 5/7/25.
 */
public abstract class IJModuleWithPath extends IJModule {

    protected final Path rootDir;

    public IJModuleWithPath(Path rootDir, ModulePath path) {
        super(path);
        this.rootDir = rootDir;
    }

    public Path rootDir() {
        return rootDir;
    }
}

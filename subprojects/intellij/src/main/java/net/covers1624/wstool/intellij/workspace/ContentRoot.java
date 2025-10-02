package net.covers1624.wstool.intellij.workspace;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by covers1624 on 10/2/25.
 */
public record ContentRoot(Path root, List<ContentPath> contentRootPaths) { }

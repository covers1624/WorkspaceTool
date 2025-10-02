package net.covers1624.wstool.intellij.workspace;

import java.nio.file.Path;

/**
 * Created by covers1624 on 10/2/25.
 */
public record ContentPath(Path path, PathType type) {

    public static ContentPath exclude(Path path) {
        return new ContentPath(path, PathType.EXCLUDE);
    }

    public static ContentPath code(Path path, boolean forTests) {
        return new ContentPath(path, forTests ? PathType.TEST_CODE : PathType.CODE);
    }

    public static ContentPath resources(Path path, boolean forTests) {
        return new ContentPath(path, forTests ? PathType.TEST_RESOURCES : PathType.RESOURCES);
    }

    public enum PathType {
        EXCLUDE,
        CODE,
        RESOURCES,
        TEST_CODE,
        TEST_RESOURCES,
    }
}

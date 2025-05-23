package net.covers1624.wstool.intellij;

import java.nio.file.Path;

/**
 * Created by covers1624 on 5/19/25.
 */
public class IJUtils {

    public static String fileUrl(Path path) {
        String absPath = path.toAbsolutePath().toString();
        if (absPath.endsWith(".jar")) {
            return "jar://" + absPath + "!/";
        }
        return "file://" + absPath;
    }
}

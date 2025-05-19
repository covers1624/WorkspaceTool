package net.covers1624.wstool.module;

import net.covers1624.quack.collection.FastStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by covers1624 on 1/31/25.
 */
public class ModuleUtils {

    /**
     * Expand a 'module reference' into a set of concrete paths.
     * <p>
     * This is commonly referred to as a 'glob' operation.
     *
     * @param baseDir   The base directory.
     * @param reference The reference to resolve.
     * @return The files.
     */
    public static Set<Path> expandModuleReference(Path baseDir, String reference) throws IOException {
        if (reference.startsWith("/")) throw new IllegalArgumentException("Module reference must not start with a slash. " + reference);

        if (!reference.endsWith("**")) {
            Path ret = baseDir.resolve(reference);
            if (!Files.exists(ret)) return Set.of();

            return Set.of(ret);
        }

        String ref = reference.substring(0, reference.length() - 2);
        int lastSlash = ref.lastIndexOf("/");
        // The file filter is any characters between the last / and the trailing **.
        // For example 'file' in For example asdf/file**
        String fileFilter = lastSlash != -1 ? ref.substring(lastSlash + 1) : "";
        // The folder path to search in is any chars before the last slash.
        Path folder = lastSlash != -1 ? baseDir.resolve(reference.substring(0, lastSlash)) : baseDir;
        // Womp, womp, we tried.
        if (Files.notExists(folder)) return Set.of();

        try (Stream<Path> files = Files.list(folder)) {
            return FastStream.of(files)
                    .filter(Files::isDirectory)
                    .filter(e -> fileFilter.isEmpty() || e.getFileName().toString().startsWith(fileFilter))
                    .toSet();
        }
    }
}

package net.covers1624.wstool.util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A {@link FileVisitor} implementation that deletes all files inside a directory tree.
 * <p>
 * Created by covers1624 on 5/21/25.
 */
// TODO move to Quack.
public class DeletingFileVisitor extends SimpleFileVisitor<Path> {

    private final @Nullable Path startingDir;

    /**
     * Whn constructed via this constructor, deletes all folders, including
     * the starting folder.
     */
    public DeletingFileVisitor() {
        this(null);
    }

    /**
     * When constructed via this constructor, does not delete the folder specified,
     * usually your starting folder.
     */
    public DeletingFileVisitor(@Nullable Path startingDir) {
        this.startingDir = startingDir;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.deleteIfExists(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
        if (startingDir == null || !startingDir.equals(dir)) {
            Files.deleteIfExists(dir);
        }
        return FileVisitResult.CONTINUE;
    }
}

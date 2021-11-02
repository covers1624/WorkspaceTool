/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A FileVisitor for Path's that copies from a to b.
 *
 * Created by covers1624 on 14/6/19.
 */
public class CopyingFileVisitor extends SimpleFileVisitor<Path> {

    private final Path fromRoot;
    private final Path toRoot;

    public CopyingFileVisitor(Path fromRoot, Path toRoot) {
        this.fromRoot = fromRoot;
        this.toRoot = toRoot;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path to = toRoot.resolve(fromRoot.relativize(file).toString());
        Files.createDirectories(to.getParent());
        Files.copy(file, to, StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
    }
}

package net.covers1624.wt.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

/**
 * Created by covers1624 on 16/12/19.
 */
public class JarStripper {

    public static void stripJar(Path input, Path output, Predicate<Path> predicate) {
        if (Files.notExists(output)) {
            if (Files.notExists(output.getParent())) {
                Utils.sneaky(() -> Files.createDirectories(output.getParent()));
            }
        }
        if (Files.exists(output)) {
            Utils.sneaky(() -> Files.delete(output));
        }
        try (FileSystem inFs = Utils.getJarFileSystem(input, true);//
             FileSystem outFs = Utils.getJarFileSystem(output, true)) {
            Path inRoot = inFs.getPath("/");
            Path outRoot = outFs.getPath("/");
            Files.walkFileTree(inRoot, new Visitor(inRoot, outRoot, predicate));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Visitor extends SimpleFileVisitor<Path> {

        private final Path inRoot;
        private final Path outRoot;
        private final Predicate<Path> predicate;

        private Visitor(Path inRoot, Path outRoot, Predicate<Path> predicate) {
            this.inRoot = inRoot;
            this.outRoot = outRoot;
            this.predicate = predicate;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path outDir = outRoot.resolve(inRoot.relativize(dir).toString());
            if (Files.notExists(outDir)) {
                Files.createDirectories(outDir);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path inFile, BasicFileAttributes attrs) throws IOException {
            Path rel = inRoot.relativize(inFile);
            Path outFile = outRoot.resolve(rel.toString());
            if (predicate.test(rel)) {
                Files.copy(inFile, outFile);
            }
            return FileVisitResult.CONTINUE;
        }
    }

}

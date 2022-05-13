/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import net.covers1624.quack.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

import static net.covers1624.quack.util.SneakyUtils.sneaky;

/**
 * A simple Jar remapper.
 * Strips signing information, uses the provided Remapper.
 * Created by covers1624 on 10/01/19.
 */
public class JarRemapper {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Remapper remapper;

    public JarRemapper(Remapper remapper) {
        this.remapper = remapper;
    }

    public void process(Path input, Path output) {
        if (Files.notExists(output)) {
            if (Files.notExists(output.getParent())) {
                sneaky(() -> Files.createDirectories(output.getParent()));
            }
        }
        try (FileSystem inFs = IOUtils.getJarFileSystem(input, true);
             FileSystem outFs = IOUtils.getJarFileSystem(output, true)) {
            Path inRoot = inFs.getPath("/");
            Path outRoot = outFs.getPath("/");
            Files.walkFileTree(inRoot, new Visitor(inRoot, outRoot, remapper));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Visitor extends SimpleFileVisitor<Path> {

        private final Path inRoot;
        private final Path outRoot;
        private final Remapper remapper;

        private Visitor(Path inRoot, Path outRoot, Remapper remapper) {
            this.inRoot = inRoot;
            this.outRoot = outRoot;
            this.remapper = remapper;
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
            Path outFile = outRoot.resolve(inRoot.relativize(inFile).toString());
            if (!outFile.endsWith(".SF") && !outFile.endsWith(".DSA") && !outFile.endsWith(".RSA")) {
                if (outFile.toString().endsWith("META-INF/MANIFEST.MF")) {
                    try (InputStream is = Files.newInputStream(inFile);
                         OutputStream os = Files.newOutputStream(outFile, StandardOpenOption.CREATE)) {
                        Manifest manifest = new Manifest(is);
                        manifest.getEntries().clear();
                        manifest.write(os);
                        os.flush();
                    }
                } else if (outFile.toString().endsWith(".class")) {
                    try (InputStream is = Files.newInputStream(inFile);
                         OutputStream os = Files.newOutputStream(outFile, StandardOpenOption.CREATE)) {
                        ClassReader reader = new ClassReader(is);
                        ClassWriter writer = new ClassWriter(0);
                        ClassRemapper remapper = new ClassRemapper(writer, this.remapper);
                        reader.accept(remapper, 0);
                        os.write(writer.toByteArray());
                        os.flush();
                    }
                } else if (outFile.toString().endsWith(".refmap.json")) {
                    MixinRefMap refMap = Utils.fromJson(inFile, MixinRefMap.class);
                    // This is a very brute-forced remap.
                    // We are assuming we will only have srg based data pass through (which is a valid assertion).
                    refMap.mappings.values().forEach(this::transformRefMap);
                    refMap.data.values().forEach(e -> e.values().forEach(this::transformRefMap));
                    Utils.toJson(refMap, MixinRefMap.class, outFile);
                } else {
                    Files.copy(inFile, outFile);
                }
            }

            return FileVisitResult.CONTINUE;
        }

        private void transformRefMap(Map<String, String> mappings) {
            for (Map.Entry<String, String> entry : mappings.entrySet()) {
                String line = entry.getValue();
                try {
                    if (line.startsWith("L")) {
                        entry.setValue(remapTarget(line));
                        continue;
                    }
                    if (line.contains(":")) {
                        entry.setValue(remapField(line));
                        continue;
                    }
                    if (line.contains("(")) {
                        entry.setValue(remapMethod(line));
                        continue;
                    }
                } catch (Throwable ex) {
                    LOGGER.error("Failed to remap line: '{}'", line, ex);
                    continue;
                }
                LOGGER.warn("Unknown entry in refmap json! '{}'", line);
            }
        }

        private String remapTarget(String target) {
            int firstSemiColon = target.indexOf(";");
            int descStart = target.indexOf("(");
            String owner = target.substring(1, firstSemiColon);
            String name = target.substring(firstSemiColon + 1, descStart);
            String desc = target.substring(descStart);

            String mappedOwner = remapper.mapType(owner);
            String mappedName = remapper.mapMethodName(owner, name, desc);
            String mappedDesc = remapper.mapMethodDesc(desc);
            return "L" + mappedOwner + ";" + mappedName + mappedDesc;
        }

        private String remapField(String field) {
            int colon = field.indexOf(':');
            String fName = field.substring(0, colon);
            String type = field.substring(colon + 1);
            return remapper.mapFieldName("", fName, "") + ":" + remapper.mapType(type);
        }

        private String remapMethod(String method) {
            int startDesc = method.indexOf('(');
            String mName = method.substring(0, startDesc);
            String desc = method.substring(startDesc);
            return remapper.mapMethodName("", mName, "") + ":" + remapper.mapMethodDesc(desc);
        }
    }

    private static class MixinRefMap {

        private final Map<String, Map<String, String>> mappings = new HashMap<>();
        private final Map<String, Map<String, Map<String, String>>> data = new HashMap<>();
    }
}

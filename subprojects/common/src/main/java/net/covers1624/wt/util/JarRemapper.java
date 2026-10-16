/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.util.SneakyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

    public void process(Path input, Path output) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(input));
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(IOUtils.makeParents(output)))) {
            process(zin, zos);
        }
    }

    public void process(ZipInputStream zis, ZipOutputStream zos) throws IOException {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.isDirectory()) continue; // Not important.
            var fName = entry.getName();
            // Signing info, skip.
            if (fName.endsWith(".SF") || fName.endsWith(".DSA") || fName.endsWith(".RSA")) continue;

            if (fName.equals("META-INF/MANIFEST.MF")) {
                writeEntry(zos, fName, out -> {
                    Manifest manifest = new Manifest(zis);
                    manifest.getEntries().clear();
                    manifest.write(out);
                });
            } else if (fName.endsWith(".class")) {
                writeEntry(zos, fName, out -> {
                    ClassReader reader = new ClassReader(zis);
                    ClassWriter writer = new ClassWriter(0);
                    ClassRemapper remapper = new ClassRemapper(writer, this.remapper);
                    reader.accept(remapper, 0);
                    out.write(writer.toByteArray());
                });
            } else if (fName.endsWith(".refmap.json")) {
                writeEntry(zos, fName, out -> {
                    MixinRefMap refMap = JsonUtils.parse(Utils.gson, IOUtils.protectClose(zis), MixinRefMap.class);
                    // This is a very brute-forced remap.
                    // We are assuming we will only have srg based data pass through (which is a valid assertion).
                    refMap.mappings.values().forEach(this::transformRefMap);
                    refMap.data.values().forEach(e -> e.values().forEach(this::transformRefMap));
                    JsonUtils.write(Utils.gson, IOUtils.protectClose(out), refMap, MixinRefMap.class);
                });
            } else if (fName.endsWith(".jar")) {
                writeEntry(zos, fName, out -> {
                    LOGGER.info("Remapping nested jar: {}", fName);
                    try (var nestedZis = new ZipInputStream(IOUtils.protectClose(zis));
                         var nestedZos = new ZipOutputStream(IOUtils.protectClose(out))) {
                        process(nestedZis, nestedZos);
                    }
                });
            } else {
                writeEntry(zos, fName, zis::transferTo);
            }
        }
    }

    private static void writeEntry(ZipOutputStream zos, String fName, SneakyUtils.ThrowingConsumer<OutputStream, IOException> func) throws IOException {
        zos.putNextEntry(new ZipEntry(fName));
        func.accept(zos);
        zos.closeEntry();
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
        return remapper.mapMethodName("", mName, "") + remapper.mapMethodDesc(desc);
    }

    private static class MixinRefMap {

        private final Map<String, Map<String, String>> mappings = new HashMap<>();
        private final Map<String, Map<String, Map<String, String>>> data = new HashMap<>();
    }
}

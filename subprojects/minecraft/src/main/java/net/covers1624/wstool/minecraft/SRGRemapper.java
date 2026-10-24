package net.covers1624.wstool.minecraft;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by covers1624 on 10/24/26.
 */
public final class SRGRemapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SRGRemapper.class);

    public static final Pattern SRG = Pattern.compile("\\b[mf]_\\d+_\\b");

    private final Map<String, String> methods = new HashMap<>();
    private final Map<String, String> fields = new HashMap<>();

    private SRGRemapper() {
    }

    public String mapName(String name) {
        var mapped = methods.get(name);
        if (mapped != null) return mapped;

        mapped = fields.get(name);
        if (mapped != null) return mapped;

        return name;
    }

    public String mapAllNames(String line) {
        return SRG.matcher(line).replaceAll(e -> Matcher.quoteReplacement(mapName(e.group())));
    }

    public static @Nullable SRGRemapper fromMcpZip(@Nullable Path zip) {
        if (zip == null) return null;

        var remapper = new SRGRemapper();
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry ent;
            while ((ent = zin.getNextEntry()) != null) {
                if (ent.getName().equals("methods.csv")) {
                    loadMappingCSV(zin, remapper.methods::put);
                }
                if (ent.getName().equals("fields.csv")) {
                    loadMappingCSV(zin, remapper.fields::put);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load mcp mappings.", ex);
        }
        if (remapper.fields.isEmpty() && remapper.methods.isEmpty()) {
            LOGGER.warn("Did not load any mappings from {} are you sure its an mcp zip?", zip);
            return null;
        }
        return remapper;
    }

    private static void loadMappingCSV(InputStream in, BiConsumer<String, String> names) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(IOUtils.protectClose(in), StandardCharsets.UTF_8))) {
            for (String line : FastStream.of(reader.lines()).skip(1)) {
                if (line.isBlank()) continue;
                var splits = line.split(",");
                if (splits.length < 2) continue;

                names.accept(splits[0], splits[1]);
            }
        }
    }
}

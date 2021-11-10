/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.java;

import net.covers1624.quack.io.IOUtils;
import net.rubygrapefruit.platform.internal.Platform;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 30/10/21.
 */
public class JavaUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaUtils.class);

    private static final Platform PLATFORM = Platform.current();
    private static final String EXE_SUFFIX = PLATFORM.isWindows() ? ".exe" : "";

    public static List<JavaInstall> locateExistingInstalls(JavaVersion version) throws IOException {
        JavaLocator javaLocator = JavaLocator.createForPlatform(PLATFORM);
        List<JavaInstall> installedJavas = javaLocator.findJavaVersions();

        return installedJavas.stream()
                .filter(e -> !e.isOpenJ9) // Ignore OpenJ9
                .filter(e -> e.langVersion == version)
                .collect(Collectors.toList());
    }

    @Nullable
    static JavaInstall parseInstall(Path executable) {
        if (Files.notExists(executable)) return null;
        try {
            Path tempDir = Files.createTempDirectory("java_prop_extract");
            JavaPropExtractGenerator.writeClass(tempDir);
            ProcessBuilder builder = new ProcessBuilder()
                    .directory(tempDir.toFile())
                    .command(
                            executable.normalize().toAbsolutePath().toString(),
                            "-Dfile.encoding=UTF8",
                            "-cp",
                            ".",
                            "PropExtract"
                    );

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Process process = builder.start();
            IOUtils.copy(process.getInputStream(), os);
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted.", e);
            }

            Map<String, String> properties = new HashMap<>();
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] split = line.split("=", 1);
                    if (split.length != 2) continue;
                    properties.put(split[0], split[1]);
                }
            }

            return new JavaInstall(
                    Paths.get(requireNonNull(properties.get("java.home"), "Missing 'java.home' property for vm: " + executable)),
                    requireNonNull(properties.get("java.vendor"), "Missing 'java.vendor' property for vm: " + executable),
                    requireNonNull(properties.get("java.vm.name"), "Missing 'java.name' property for vm: " + executable),
                    requireNonNull(properties.get("java.version"), "Missing 'java.version' property for vm: " + executable),
                    requireNonNull(properties.get("java.runtime.name"), "Missing 'java.runtime.name' property for vm: " + executable),
                    requireNonNull(properties.get("java.runtime.version"), "Missing 'java.runtime.version' property for vm: " + executable),
                    requireNonNull(properties.get("os.arch"), "Missing 'os.arch' property for vm: " + executable).contains("64")
            );
        } catch (IOException e) {
            if (JavaLocator.DEBUG) {
                LOGGER.error("Failed to parse Java install.", e);
            }
            return null;
        }
    }

    public static Path getJavaExecutable(Path javaHome) {
        return javaHome.resolve("bin/java" + EXE_SUFFIX);
    }
}

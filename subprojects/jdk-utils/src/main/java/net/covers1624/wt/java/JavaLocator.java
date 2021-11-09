/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.java;

import net.rubygrapefruit.platform.internal.Platform;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static net.covers1624.quack.collection.ColUtils.iterable;

/**
 * Created by covers1624 on 28/10/21.
 */
public abstract class JavaLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaLocator.class);
    protected static final boolean DEBUG = Boolean.getBoolean("net.covers1624.wt.wrapper.java.debug");

    // TODO, add VersionCollector parameter.
    //  - VersionCollector can be constructed with a given JavaVersion.
    //  - When versions are added this filter is automatically applied.
    public abstract List<JavaInstall> findJavaVersions() throws IOException;

    public static JavaLocator createForPlatform(Platform platform) {
        if (platform.isWindows()) {
            return new WindowsJavaLocator();
        }
        if (platform.isLinux()) {
            return new LinuxJavaLocator();
        }
        throw new RuntimeException("Unsupported platform: " + System.getProperty("os.name"));
    }

    protected static void findJavasInFolder(Map<String, JavaInstall> installs, Path folder) throws IOException {
        if (Files.notExists(folder)) return;
        for (Path path : iterable(Files.list(folder))) {
            if (!Files.isDirectory(path)) continue;
            Path javaExecutable = JavaUtils.getJavaExecutable(path);
            JavaInstall install = JavaUtils.parseInstall(javaExecutable);
            considerJava(installs, install);
        }
    }

    protected static void considerJava(Map<String, JavaInstall> installs, @Nullable JavaInstall install) {
        if (install != null && !installs.containsKey(install.javaHome.toString())) {
            installs.put(install.javaHome.toString(), install);
        }
    }

}

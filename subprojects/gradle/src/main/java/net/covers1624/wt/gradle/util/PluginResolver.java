/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.gradle.util;

import net.covers1624.quack.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Plugin;
import org.gradle.api.plugins.PluginContainer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static net.covers1624.quack.collection.ColUtils.iterable;
import static net.covers1624.quack.util.SneakyUtils.sneak;
import static net.covers1624.wt.util.Utils.extractRoot;

/**
 * Created by covers1624 on 15/6/19.
 */
public class PluginResolver {

    public static Map<String, String> extractPlugins(PluginContainer container) {
        Map<String, String> classToName = new HashMap<>();
        for (Plugin<?> plugin : container) {
            try {
                Class<?> pluginClass = plugin.getClass();
                Enumeration<URL> urls = pluginClass.getClassLoader().getResources("META-INF/gradle-plugins/");
                for (URL url : iterable(urls)) {
                    FileSystem fs;
                    Path folder;
                    if (url.getProtocol().equals("jar")) {
                        String root = extractRoot(url, "/META-INF/gradle-plugins/");
                        fs = IOUtils.getJarFileSystem(Paths.get(root), true);
                        folder = fs.getPath("/META-INF/gradle-plugins/");
                    } else if (url.getProtocol().equals("file")) {
                        fs = IOUtils.protectClose(FileSystems.getDefault());
                        folder = fs.getPath(url.getFile());
                    } else {
                        continue;
                    }
                    Files.walk(folder).forEach(sneak(p -> {
                        if (Files.isRegularFile(p) && p.getFileName().toString().endsWith(".properties")) {
                            String pluginName = p.getFileName().toString().replace(".properties", "");
                            try (InputStream is = Files.newInputStream(p)) {
                                Properties properties = new Properties();
                                properties.load(is);
                                String clsName = properties.getProperty("implementation-class");
                                if (!classToName.containsKey(clsName)) {
                                    classToName.put(clsName, StringUtils.removeStart(pluginName, "org.gradle."));
                                }
                            }
                        }
                    }));
                    fs.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        return classToName;
    }
}

package net.covers1624.wt.gradle.util;

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

import static net.covers1624.wt.util.Utils.*;

/**
 * Created by covers1624 on 15/6/19.
 */
public class PluginResolver {

    private Map<String, String> classToName = new HashMap<>();

    public void process(PluginContainer container) {
        for (Plugin plugin : container) {
            try {
                Class pluginClass = plugin.getClass();
                Enumeration<URL> urls = pluginClass.getClassLoader().getResources("META-INF/gradle-plugins/");
                for (URL url : toIterable(urls)) {
                    FileSystem fs;
                    Path folder;
                    if (url.getProtocol().equals("jar")) {
                        String root = extractRoot(url, "/META-INF/gradle-plugins/");
                        fs = getJarFileSystem(Paths.get(root), true);
                        folder = fs.getPath("/META-INF/gradle-plugins/");
                    } else if (url.getProtocol().equals("file")) {
                        fs = protectClose(FileSystems.getDefault());
                        folder = fs.getPath(url.getFile());
                    } else {
                        continue;
                    }
                    Files.walk(folder).forEach(sneakyL(p -> {
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
    }

    public Map<String, String> getClassNameMappings() {
        return classToName;
    }
}

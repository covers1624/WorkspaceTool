package net.covers1624.wt.wrapper.java;

import net.covers1624.wt.wrapper.Main;
import net.rubygrapefruit.platform.MissingRegistryEntryException;
import net.rubygrapefruit.platform.Native;
import net.rubygrapefruit.platform.WindowsRegistry;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 28/10/21.
 */
public class WindowsJavaLocator extends JavaLocator {

    // Oracle Enterprise.
    private static final String[] ORACLE = {
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\JavaSoft\\Java Development Kit",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\JavaSoft\\Java Runtime Environment",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\JavaSoft\\JRE",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\JavaSoft\\JDK",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\JavaSoft\\Java Development Kit",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\JavaSoft\\Java Runtime Environment",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\JavaSoft\\JRE",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\JavaSoft\\JDK",
    };

    // Adoptium predecessor (OpenJDK).
    private static final String[] ADOPT_OPEN_JDK = {
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\AdoptOpenJDK\\JDK",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\AdoptOpenJDK\\JRE",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\AdoptOpenJDK\\JDK",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\AdoptOpenJDK\\JRE"
    };

    // AdoptOpenJDK successor (Eclipse, OpenJDK).
    private static final String[] ADOPTIUM = {
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Eclipse Foundation\\JDK",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Eclipse Foundation\\JRE",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\Eclipse Foundation\\JDK",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\Eclipse Foundation\\JRE",
    };

    // Microsoft (OpenJDK).
    private static final String[] MICROSOFT = {
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Microsoft\\JDK",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Microsoft\\JRE",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\Microsoft\\JDK",
            /*HKEY_LOCAL_MACHINE\\*/ "SOFTWARE\\Wow6432Node\\Microsoft\\JRE",
    };

    private static final String[] PATHS = {
            "C:/Program Files/AdoptOpenJDK/",
            "C:/Program Files/Eclipse Foundation/",
            "C:/Program Files/Java/",
            "C:/Program Files/Microsoft/",
            "C:/Program Files (x86)/AdoptOpenJDK/",
            "C:/Program Files (x86)/Eclipse Foundation/",
            "C:/Program Files (x86)/Java",
            "C:/Program Files (x86)/Microsoft/",
    };

    @Override
    public List<JavaInstall> findJavaVersions() throws IOException {
        Native.init(null);
        WindowsRegistry registry = Native.get(WindowsRegistry.class);
        Map<String, JavaInstall> installs = new LinkedHashMap<>();
        findAll(registry, installs, ORACLE, "", "JavaHome");
        findAll(registry, installs, ADOPT_OPEN_JDK, join("hotspot", "MSI"), "Path");
        findAll(registry, installs, ADOPTIUM, join("hotspot", "MSI"), "Path");
        findAll(registry, installs, MICROSOFT, join("hotspot", "MSI"), "Path");
        for (String path : PATHS) {
            findJavasInFolder(installs, Paths.get(path));
        }
        // Gradle installed
        findJavasInFolder(installs, Paths.get(System.getProperty("user.home"), ".gradle/jdks"));
        // Intellij installed
        findJavasInFolder(installs, Paths.get(System.getProperty("user.home"), ".jdks"));
        // Workspace installed JDK's
        findJavasInFolder(installs, Main.WT_JDKS);
        return new ArrayList<>(installs.values());
    }

    private static void findAll(WindowsRegistry registry, Map<String, JavaInstall> installs, String[] keys, String keySuffix, String pathKey) {
        for (String key : keys) {
            for (String subkey : getSubkeys(registry, WindowsRegistry.Key.HKEY_LOCAL_MACHINE, key)) {
                Path javaHome = getPathValue(registry, WindowsRegistry.Key.HKEY_LOCAL_MACHINE, join(subkey, keySuffix), pathKey);
                if (javaHome == null) continue;

                javaHome = javaHome.toAbsolutePath();

                Path javaExecutable = JavaUtils.getJavaExecutable(javaHome);
                JavaInstall javaInstall = JavaUtils.parseInstall(javaExecutable);
                considerJava(installs, javaInstall);
            }
        }
    }

    @Nullable
    private static Path getPathValue(WindowsRegistry registry, WindowsRegistry.Key key, String subkey, String value) {
        try {
            return Paths.get(registry.getStringValue(key, subkey, value));
        } catch (MissingRegistryEntryException e) {
            return null;
        }
    }

    private static List<String> getSubkeys(WindowsRegistry registry, WindowsRegistry.Key key, String subkey) {
        try {
            return registry.getSubkeys(key, subkey).stream()
                    .map(e -> join(subkey, e))
                    .collect(Collectors.toList());
        } catch (MissingRegistryEntryException e) {
            return Collections.emptyList();
        }
    }

    private static String join(String... strs) {
        return Arrays.stream(strs)
                .filter(e -> e != null && !e.isEmpty())
                .collect(Collectors.joining("\\"));
    }
}

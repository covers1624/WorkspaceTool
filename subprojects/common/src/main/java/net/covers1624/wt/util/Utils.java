/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Table;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.covers1624.quack.gson.FileAdapter;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.covers1624.quack.gson.LowerCaseEnumAdapterFactory;
import net.covers1624.quack.gson.MavenNotationAdapter;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.util.HashUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static net.covers1624.quack.util.SneakyUtils.sneaky;

/**
 * Created by covers1624 on 6/01/19.
 */
@SuppressWarnings ({ "ResultOfMethodCallIgnored", "UnstableApiUsage" })
public class Utils {

    private static final Logger LOGGER = LogManager.getLogger(Utils.class);

    public static final Gson gson = sneaky(() -> {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(File.class, new FileAdapter())
                .registerTypeAdapter(HashCode.class, new HashCodeAdapter())
                .registerTypeAdapterFactory(new LowerCaseEnumAdapterFactory())
                .registerTypeAdapter(MavenNotation.class, new MavenNotationAdapter());
        return builder.create();
    });

    /**
     * Wrapper for {@link Gson#fromJson(Reader, Class)} except from a file.
     *
     * @param file     The file to read from.
     * @param classOfT The class to use.
     * @return The parsed object.
     */
    public static <T> T fromJson(File file, Class<T> classOfT) {
        return fromJson(file.toPath(), classOfT);
    }

    public static <T> T fromJson(Path path, Class<T> classOfT) {
        try (Reader reader = Files.newBufferedReader(path)) {
            return gson.fromJson(reader, classOfT);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read json from file. " + path);
        }
    }

    /**
     * Wrapper for {@link Gson#fromJson(Reader, Type)} except from a file.
     *
     * @param file    The file to read from.
     * @param typeOfT The type to use.
     * @return The parsed object.
     */
    public static <T> T fromJson(File file, Type typeOfT) {
        return fromJson(file.toPath(), typeOfT);
    }

    public static <T> T fromJson(Path path, Type typeOfT) {
        try (Reader reader = Files.newBufferedReader(path)) {
            return gson.fromJson(reader, typeOfT);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read json from file. " + path, e);
        }
    }

    /**
     * Wrapper for {@link Gson#toJson(Object, Appendable)} except to a File.
     *
     * @param obj  The Object to write.
     * @param file The file to write to.
     */
    public static void toJson(Object obj, File file) {
        toJson(obj, obj.getClass(), file);
    }

    /**
     * Wrapper for {@link Gson#toJson(Object, Type, Appendable)} except to a File.
     *
     * @param obj       The object to write.
     * @param typeOfObj The Type to use.
     * @param file      The File to write to.
     */
    public static void toJson(Object obj, Type typeOfObj, File file) {
        toJson(obj, typeOfObj, file.toPath());
    }

    public static void toJson(Object obj, Type typeOfObj, Path path) {
        try {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete file: " + path, e);
        }
        try (Writer writer = Files.newBufferedWriter(path, WRITE, CREATE)) {
            gson.toJson(obj, typeOfObj, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write json to file. " + path);
        }
    }

    /**
     * Deletes the given directory and all containing files.
     *
     * @param directory The directory to delete.
     */
    public static void deleteFolder(File directory) {
        Deque<File> toDelete = new ArrayDeque<>();
        toDelete.push(directory);
        while (!toDelete.isEmpty()) {
            File peek = toDelete.peek();
            if (peek.isDirectory()) {
                File[] list = peek.listFiles();
                if (list == null || list.length == 0) {
                    toDelete.pop().delete();
                } else {
                    for (File f : list) {
                        toDelete.push(f);
                    }
                }
            } else {
                toDelete.pop().delete();
            }
        }
    }

    public static Path makeFile(Path path) {
        if (Files.notExists(path)) {
            Path parent = path.toAbsolutePath().getParent();
            if (Files.notExists(parent)) {
                sneaky(() -> Files.createDirectories(parent));
            }
        } else {
            sneaky(() -> Files.delete(path));
        }
        sneaky(() -> Files.createFile(path));
        return path;
    }

    /**
     * Copies the content of the provided resource to the provided Hasher.
     *
     * @param hasher   The hasher.
     * @param resource The resource.
     */
    public static void addToHasher(Hasher hasher, String resource) {
        try (InputStream is = Utils.class.getResourceAsStream(resource)) {
            HashUtils.addToHasher(hasher, is);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read resource: " + resource, e);
        }
    }

    public static void addToHasher(Hasher hasher, Path path) {
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                HashUtils.addToHasher(hasher, is);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read file: " + path, e);
            }
        }
    }

    public static List<Path> toPaths(List<File> from) {
        return from.parallelStream().map(File::toPath).collect(Collectors.toList());
    }

    public static Path getJarPathForClass(String aClass) {
        String resourceRoot = getResourceRoot(Utils.class, "/" + aClass.replace('.', '/') + ".class");
        return resourceRoot != null ? new File(resourceRoot).getAbsoluteFile().toPath() : null;
    }

    public static Path getJarPathForClass(Class<?> aClass) {
        String resourceRoot = getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
        return resourceRoot != null ? new File(resourceRoot).getAbsoluteFile().toPath() : null;
    }

    public static Path getJarPathForResource(String resource) {
        String resourceRoot = getResourceRoot(Utils.class, "/" + resource);
        return resourceRoot != null ? new File(resourceRoot).getAbsoluteFile().toPath() : null;
    }

    public static String getResourceRoot(Class<?> context, String path) {
        URL url = context.getResource(path);
        if (url == null) {
            url = ClassLoader.getSystemResource(path.substring(1));
        }
        return url != null ? extractRoot(url, path) : null;
    }

    public static String extractRoot(URL resourceURL, String resourcePath) {
        if (!(StringUtils.startsWith(resourcePath, "/") || StringUtils.startsWith(resourcePath, "\\"))) {
            LOGGER.warn("precondition failed: " + resourcePath);
            return null;
        }

        String resultPath = null;
        String protocol = resourceURL.getProtocol();
        if ("file".equals(protocol)) {
            String path = urlToFile(resourceURL).getPath();
            String testPath = path.replace('\\', '/');
            String testResourcePath = resourcePath.replace('\\', '/');
            if (StringUtils.endsWithIgnoreCase(testPath, testResourcePath)) {
                resultPath = path.substring(0, path.length() - resourcePath.length());
            }
        } else if ("jar".equals(protocol)) {
            Pair<String, String> paths = splitJarUrl(resourceURL.getFile());
            if (paths != null && paths.getLeft() != null) {
                resultPath = paths.getLeft().replace("\\", "/");
            }
        } else if ("jrt".equals(protocol)) {
            return null;
        }

        if (resultPath == null) {
            LOGGER.warn("cannot extract '" + resourcePath + "' from '" + resourceURL + "'");
            return null;
        }

        return StringUtils.removeEnd(resultPath, "/");
    }

    @Nullable
    public static Pair<String, String> splitJarUrl(String url) {
        int pivot = url.indexOf("!/");
        if (pivot < 0) {
            return null;
        }

        String resourcePath = url.substring(pivot + 2);
        String jarPath = url.substring(0, pivot);

        if (jarPath.startsWith("jar:")) {
            jarPath = jarPath.substring("jar".length() + 1);
        }

        if (jarPath.startsWith("file")) {
            try {
                jarPath = urlToFile(new URL(jarPath)).getPath().replace('\\', '/');
            } catch (Exception e) {
                jarPath = jarPath.substring("file".length());
                if (jarPath.startsWith("://")) {
                    jarPath = jarPath.substring("://".length());
                } else if (StringUtils.startsWith(jarPath, ":")) {
                    jarPath = jarPath.substring(1);
                }
            }
        }

        return Pair.of(jarPath, resourcePath);
    }

    public static File urlToFile(URL url) {
        try {
            return new File(url.toURI().getSchemeSpecificPart());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL='" + url + "'", e);
        }
    }

    public static boolean isAncestor(Path path, Path prefix, boolean strict) {
        return startsWith(path, prefix, strict, IOCase.SYSTEM, true);
    }

    public static boolean startsWith(Path _path, Path _prefix, boolean strict, IOCase ioCase, boolean checkImmediateParent) {
        String path = _path.normalize().toAbsolutePath().toString();
        String prefix = _prefix.normalize().toAbsolutePath().toString();
        int pathLength = path.length();
        int prefixLength = prefix.length();
        if (prefixLength == 0) {
            return pathLength == 0;
        }
        if (prefixLength > pathLength) {
            return false;
        }
        if (!path.regionMatches(!ioCase.isCaseSensitive(), 0, prefix, 0, prefixLength)) {
            return false;
        }
        if (pathLength == prefixLength) {
            return !strict;
        }

        char lastPrefixChar = prefix.charAt(prefixLength - 1);
        int slashOrSeparatorIdx = prefixLength;
        if (lastPrefixChar == '/' || lastPrefixChar == File.separatorChar) {
            slashOrSeparatorIdx = prefixLength - 1;
        }
        char next1 = path.charAt(slashOrSeparatorIdx);
        if (next1 == '/' || next1 == File.separatorChar) {
            if (!checkImmediateParent) {
                return true;
            }

            if (slashOrSeparatorIdx == pathLength - 1) {
                return true;
            }
            int idxNext = path.indexOf(next1, slashOrSeparatorIdx + 1);
            idxNext = idxNext == -1 ? path.indexOf(next1 == '/' ? '\\' : '/', slashOrSeparatorIdx + 1) : idxNext;
            return idxNext == -1;
        }
        return false;

    }

    /**
     * Extracts a resource to the provided file.
     *
     * @param resource The resource.
     * @param to       The file.
     */
    public static void extractResource(String resource, Path to) {
        try (InputStream is = Utils.class.getResourceAsStream(resource)) {
            Path parent = to.getParent();
            if (!Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (OutputStream fos = Files.newOutputStream(to, WRITE, CREATE)) {
                IOUtils.copy(is, fos);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract resource '" + resource + "' to file '" + to + "'.", e);
        }
    }

    public static <R, C, V> V computeIfAbsent(Table<R, C, V> table, R r, C c, Supplier<V> vFunc) {
        V val = table.get(r, c);
        if (val == null) {
            val = vFunc.get();
            table.put(r, c, val);
        }
        return val;
    }

    public static <T extends AccessibleObject> T makeAccessible(T thing) {
        thing.setAccessible(true);
        return thing;
    }

    @SuppressWarnings ("unchecked")
    public static <T> T getField(Field field, Object instance) {
        return (T) sneaky(() -> makeAccessible(field).get(instance));
    }
}

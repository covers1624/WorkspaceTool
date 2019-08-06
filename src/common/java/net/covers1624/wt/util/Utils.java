/*
 * Copyright (c) 2018-2019 covers1624
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package net.covers1624.wt.util;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.WillNotClose;
import java.io.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Created by covers1624 on 6/01/19.
 */
@SuppressWarnings ({ "ResultOfMethodCallIgnored", "UnstableApiUsage" })
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger("Utils");

    //32k buffer.
    private static final ThreadLocal<byte[]> bufferCache = ThreadLocal.withInitial(() -> new byte[32 * 1024]);
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
    private static final Map<String, String> jfsArgsCreate = ImmutableMap.of("create", "true");

    private static boolean PRETTY_JSON = true;//Boolean.getBoolean("workspacetool.pretty_json");
    public static final Gson gson = sneaky(() -> {
        GsonBuilder builder = new GsonBuilder()//
                .registerTypeAdapter(File.class, new FileAdapter())//
                .registerTypeAdapter(HashCode.class, new HashCodeAdapter());//
        if (PRETTY_JSON) {
            builder = builder.setPrettyPrinting();
        }
        return builder.create();
    });

    /**
     * Makes a ProcessExecutor.
     * Blank executor with a callback for logging.
     *
     * @return The Executor.
     */
    public static ProcessExecutor makeExecutor() {
        Logger logger = LoggerFactory.getLogger("ProcessExecutor");
        return new ProcessExecutor().addPreStartCallback(e -> logger.debug("Executing '{}' in '{}'.", Joiner.on(' ').join(e.getCmd()), e.getWorkingDir()));
    }

    public static Runnable sneak(ThrowingRunnable<Throwable> tr) {
        return () -> sneaky(tr);
    }

    public static void sneaky(ThrowingRunnable<Throwable> tr) {
        try {
            tr.run();
        } catch (Throwable t) {
            throwUnchecked(t);
        }
    }

    public static <T> T sneaky(ThrowingProducer<T, Throwable> tp) {
        try {
            return tp.get();
        } catch (Throwable t) {
            throwUnchecked(t);
            return null;//Un possible
        }
    }

    public static <T> Consumer<T> sneakyL(ThrowingConsumer<T, Throwable> tc) {
        return t -> {
            try {
                tc.accept(t);
            } catch (Throwable th) {
                throwUnchecked(th);
            }
        };
    }

    @SuppressWarnings ("unchecked")
    public static <T> T unsafeCast(Object object) {
        return (T) object;
    }

    /**
     * Throws an exception without compiler warnings.
     */
    @SuppressWarnings ("unchecked")
    public static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }

    public static void doLoudly(ThrowingRunnable<Throwable> runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static <T> T doLoudly(ThrowingProducer<T, Throwable> producer) {
        try {
            return producer.get();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

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
     * Represents this Enumeration as an Iterable.
     *
     * @param enumeration The Enumeration.
     * @param <E>         The Type.
     * @return The Iterable.
     */
    public static <E> Iterable<E> toIterable(Enumeration<E> enumeration) {
        return () -> new Iterator<E>() {
            //@formatter:off
            @Override public boolean hasNext() { return enumeration.hasMoreElements(); }
            @Override public E next() { return enumeration.nextElement(); }
            //@formatter:on
        };
    }

    /**
     * Walks all files from the given directory.
     *
     * @param directory    The base.
     * @param fileConsumer The consumer to handle files.
     */
    public static void walkFiles(File directory, Consumer<File> fileConsumer) {
        Deque<File> toSearch = new ArrayDeque<>();
        toSearch.push(directory);
        while (!toSearch.isEmpty()) {
            File file = toSearch.pop();
            if (file.isDirectory()) {
                File[] list = file.listFiles();
                if (list != null) {
                    for (File f : list) {
                        toSearch.push(f);
                    }
                }
            } else if (file.isFile()) {
                fileConsumer.accept(file);
            }
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
                } else if (list != null) {
                    for (File f : list) {
                        toDelete.push(f);
                    }
                }
            } else {
                toDelete.pop().delete();
            }
        }
    }

    /**
     * Ensures a file exists, creating parent directories if necessary.
     *
     * @param file The file.
     * @return The same file.
     */
    public static File makeFile(File file) {
        if (!file.exists()) {
            File p = file.getAbsoluteFile().getParentFile();
            if (!p.exists()) {
                p.mkdirs();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create a new file.", e);
            }
        }
        return file;
    }

    /**
     * Reads all lines of a file to a List of strings.
     * Charset defaults to UTF_8
     *
     * @param file The File.
     * @return The lines.
     */
    public static List<String> readLines(File file) {
        return readLines(file, StandardCharsets.UTF_8);
    }

    /**
     * Reads all lines of a file to a list of strings.
     *
     * @param file    The File.
     * @param charset The Charset.
     * @return The lines.
     */
    public static List<String> readLines(File file, Charset charset) {
        try {
            return Files.readAllLines(file.toPath(), charset);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read file to lines.", e);
        }
    }

    /**
     * Reads an InputStream to a byte array.
     *
     * @param is The InputStream.
     * @return The bytes.
     * @throws IOException If something is bork.
     */
    public static byte[] toBytes(@WillNotClose InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copy(is, os);
        return os.toByteArray();
    }

    public static InputStream toInputStream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    /**
     * Copies a File from one location to another.
     *
     * @param in  From.
     * @param out To.
     */
    public static void copyFile(File in, File out) {
        try (FileInputStream fis = new FileInputStream(in)) {
            try (FileOutputStream fos = new FileOutputStream(makeFile(out))) {
                copy(fis, fos);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file from: '" + in + "', to: '" + out + "'.", e);
        }
    }

    /**
     * Copies the content of an InputStream to an OutputStream.
     *
     * @param is The InputStream.
     * @param os The OutputStream.
     * @throws IOException If something is bork.
     */
    public static void copy(@WillNotClose InputStream is, @WillNotClose OutputStream os) throws IOException {
        byte[] buffer = bufferCache.get();
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    /**
     * Copies the content of the provided resource to the provided Hasher.
     *
     * @param hasher   The hasher.
     * @param resource The resource.
     */
    public static void addToHasher(Hasher hasher, String resource) {
        try (InputStream is = Utils.class.getResourceAsStream(resource)) {
            addToHasher(hasher, is);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read resource: " + resource, e);
        }
    }

    /**
     * Copies the content of the provided File to the provided Hasher.
     *
     * @param hasher The Hasher.
     * @param file   The File.
     */
    public static void addToHasher(Hasher hasher, File file) {
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                addToHasher(hasher, fis);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read file: " + file, e);
            }
        }
    }

    public static void addToHasher(Hasher hasher, Path path) {
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                addToHasher(hasher, is);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read file: " + path, e);
            }
        }
    }

    /**
     * Copies the content of the provided InputStream to the provided Hasher.
     *
     * @param hasher The hasher.
     * @param is     The InputStream.
     * @throws IOException If something is bork.
     */
    public static void addToHasher(Hasher hasher, @WillNotClose InputStream is) throws IOException {
        byte[] buffer = bufferCache.get();
        int len;
        while ((len = is.read(buffer)) != -1) {
            hasher.putBytes(buffer, 0, len);
        }
    }

    public static List<Path> toPaths(List<File> from) {
        return from.parallelStream().map(File::toPath).collect(Collectors.toList());
    }

    public static void maybeExtractResource(String resource, File file) {
        if (!file.exists()) {
            extractResource(resource, file);
        }
    }

    public static void maybeExtractResource(String resource, Path path) {
        if (Files.notExists(path)) {
            extractResource(resource, path);
        }
    }

    public static Path commonRootPath(List<Path> paths, Path preferred) {
        Preconditions.checkArgument(!paths.isEmpty(), "Paths cannot be empty.");
        FileSystem fs = preferred.getFileSystem();
        if (paths.stream().anyMatch(e -> e.getFileSystem().provider() != fs.provider())) {
            throw new RuntimeException("All provided paths are not on the same FileSystem.");
        }
        Path commonPath = fs.getPath("/");
        String[][] folders = paths.parallelStream().map(e -> e.toString().split("[/\\\\]")).toArray(String[][]::new);
        outer:
        for (int i = 0; i < folders[0].length; i++) {
            String s = folders[0][i];
            for (int j = 0; j < paths.size(); j++) {
                if (!s.equals(folders[j][i])) {
                    break outer;
                }
            }
            commonPath = commonPath.resolve(s);
            if (commonPath.equals(preferred)) {
                return preferred;
            }
        }
        return commonPath;
    }

    public static FileSystem getJarFileSystem(File file, boolean create) throws IOException {
        return getJarFileSystem(file.toURI(), create);
    }

    public static FileSystem getJarFileSystem(Path path, boolean create) throws IOException {
        return getJarFileSystem(path.toUri(), create);
    }

    public static FileSystem getJarFileSystem(URI path, boolean create) throws IOException {
        URI jarURI;
        try {
            jarURI = new URI("jar:file", null, path.getPath(), "");
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return getFileSystem(jarURI, create ? jfsArgsCreate : Collections.emptyMap());
    }

    public static FileSystem getFileSystem(URI uri) throws IOException {
        return getFileSystem(uri, Collections.emptyMap());
    }

    public static FileSystem getFileSystem(URI uri, Map<String, ?> env) throws IOException {
        FileSystem fs;
        boolean owner = true;
        try {
            fs = FileSystems.newFileSystem(uri, env);
        } catch (FileSystemAlreadyExistsException e) {
            fs = FileSystems.getFileSystem(uri);
            owner = false;
        }
        return owner ? fs : protectClose(fs);
    }

    public static FileSystem protectClose(FileSystem fs) {
        return new DelegateFileSystem(fs) {
            @Override
            public void close() {
            }
        };
    }

    public static Path getJarPathForClass(String aClass) {
        String resourceRoot = getResourceRoot(Utils.class, "/" + aClass.replace('.', '/') + ".class");
        return resourceRoot != null ? new File(resourceRoot).getAbsoluteFile().toPath() : null;
    }

    public static Path getJarPathForClass(Class aClass) {
        String resourceRoot = getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
        return resourceRoot != null ? new File(resourceRoot).getAbsoluteFile().toPath() : null;
    }

    public static Path getJarPathForResource(String resource) {
        String resourceRoot = getResourceRoot(Utils.class, "/" + resource);
        return resourceRoot != null ? new File(resourceRoot).getAbsoluteFile().toPath() : null;
    }

    public static String getResourceRoot(Class context, String path) {
        URL url = context.getResource(path);
        if (url == null) {
            url = ClassLoader.getSystemResource(path.substring(1));
        }
        return url != null ? extractRoot(url, path) : null;
    }

    public static String extractRoot(URL resourceURL, String resourcePath) {
        if (!(StringUtils.startsWith(resourcePath, "/") || StringUtils.startsWith(resourcePath, "\\"))) {
            logger.warn("precondition failed: " + resourcePath);
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
            logger.warn("cannot extract '" + resourcePath + "' from '" + resourceURL + "'");
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
            throw new IllegalArgumentException("URL='" + url.toString() + "'", e);
        }
    }

    public static Path getResourcePath(String resource) {
        return sneaky(() -> Paths.get(Utils.class.getResource(resource).toURI()));
    }

    /**
     * Extracts a resource to the provided file.
     *
     * @param resource The resource.
     * @param file     The file.
     */
    public static void extractResource(String resource, File file) {
        try (InputStream is = Utils.class.getResourceAsStream(resource)) {
            try (FileOutputStream fos = new FileOutputStream(makeFile(file))) {
                copy(is, fos);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract resource '" + resource + "' to file '" + file + "'.", e);
        }
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
                copy(is, fos);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract resource '" + resource + "' to file '" + to + "'.", e);
        }
    }

    public static String makePad(int num) {
        if (num == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(num);
        for (int i = 0; i < num; i++) {
            builder.append(" ");
        }
        return builder.toString();
    }

    /**
     * Splits a string into segments for a command.
     * Supports escaping.
     *
     * @param toProcess The string.
     * @return The arguments.
     */
    public static String[] cmdSplit(String toProcess) {
        if (toProcess == null || toProcess.length() == 0) {
            //no command? no string
            return new String[0];
        }
        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        final ArrayList<String> result = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("\'".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("\'".equals(nextTok)) {
                        state = inQuote;
                    } else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    } else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || current.length() != 0) {
                            result.add(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }
        if (lastTokenHasBeenQuoted || current.length() != 0) {
            result.add(current.toString());
        }
        if (state == inQuote || state == inDoubleQuote) {
            throw new RuntimeException("unbalanced quotes in " + toProcess);
        }
        return result.toArray(new String[0]);
    }

    /**
     * Sets up a ProcessBuilder for invoking a gradle wrapper.
     * The Process will be executed in 'projectDir' using the wrapper
     * from 'wrapperFrom'.
     *
     * @param projectDir  The project folder to execute in.
     * @param wrapperFrom The wrapper to use.
     * @return The ProcessBuilder
     */
    public static ProcessExecutor buildGradlewInvoke(File projectDir, File wrapperFrom) {
        ProcessExecutor executor = makeExecutor();
        executor.setWorkingDir(projectDir);
        List<String> cmd = Lists.newArrayList(//
                System.getProperty("wt.java", "java"),//
                "-classpath",//
                new File(wrapperFrom, "gradle/wrapper/gradle-wrapper.jar").getAbsolutePath(),//
                "org.gradle.wrapper.GradleWrapperMain"//
        );
        executor.setCmd(cmd);
        executor.addEnvVar("org.gradle.appname", "gradlew");
        return executor;
    }

    /**
     * Generates a Random Hex string with the provided length.
     * Uses SecureRandom, because reasons, probably not secure.
     *
     * @param len The length.
     * @return The random String.
     */
    public static String randomHex(int len) {
        SecureRandom randy = new SecureRandom();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            builder.append(HEX_CHARS[randy.nextInt(HEX_CHARS.length - 1)]);
        }
        return builder.toString();
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
        return (T) sneaky(() -> field.get(instance));
    }

    /**
     * A simple TypeAdapter for Files.
     * Maps a single String to a File.
     */
    private static class FileAdapter extends TypeAdapter<File> {

        @Override
        public void write(JsonWriter out, File value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.getAbsolutePath());
        }

        @Override
        public File read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return new File(in.nextString());
        }
    }

    private static class HashCodeAdapter extends TypeAdapter<HashCode> {

        @Override
        public void write(JsonWriter out, HashCode value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toString());
        }

        @Override
        public HashCode read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return HashCode.fromString(in.nextString());
        }
    }
}

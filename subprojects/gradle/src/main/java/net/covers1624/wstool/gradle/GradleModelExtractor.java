package net.covers1624.wstool.gradle;

import com.google.common.base.Suppliers;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.ConsumingOutputStream;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.HashContainer;
import net.covers1624.wstool.api.JdkProvider;
import net.covers1624.wstool.gradle.api.WorkspaceToolModelAction;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SubProjectList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gradle.internal.impldep.com.google.common.collect.ImmutableMap;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.Task;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by covers1624 on 17/5/23.
 */
public class GradleModelExtractor {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String GRADLE_PLUGIN_CLASS = "net.covers1624.wstool.gradle.WorkspaceToolGradlePlugin";

    // Regex to get the gradle version from a Gradle wrapper properties file.
    private static final Pattern WRAPPER_URL_REGEX = Pattern.compile("gradle-(.*)(?>-bin|-all).zip$");

    // The minimum version of Gradle we support extracting data from.
    private static final GradleVersion MIN_GRADLE_VERSION = GradleVersion.version("4.10.3");

    // The version above which we run Gradle with Java 16.
    // TODO, can we turn this into a list of configurable constraints?
    //       Forge for 1.17 is the only version which requires this.
    private static final GradleVersion MIN_GRADLE_USE_J16 = GradleVersion.version("7.0");

    // The version above which we run Gradle with Java 17.
    private static final GradleVersion MIN_GRADLE_USE_J17 = GradleVersion.version("7.3");

    private final Supplier<List<Path>> gradleClassPath = Suppliers.memoize(this::buildClassPath);
    private final Supplier<String> gradleClassPathHash = Suppliers.memoize(this::hashGradleClassPath);
    private final Supplier<Path> initScriptPath = Suppliers.memoize(this::buildInitScript);

    private final JdkProvider jdkProvider;
    private final Path gradleCacheDir;
    private final Path projectRootDir;

    private final List<String> hashableFiles = new ArrayList<>(List.of(
            "buildSrc/build.gradle",
            "buildSrc/settings.gradle",
            "build.gradle",
            "settings.gradle",
            "gradle.properties",
            "build.properties",
            "gradle/wrapper/gradle-wrapper.properties"
    ));

    public GradleModelExtractor(Environment env, List<String> hashableFiles) {
        jdkProvider = env.getService(JdkProvider.class);
        this.hashableFiles.addAll(hashableFiles);

        gradleCacheDir = env.projectCache().resolve("gradle");
        projectRootDir = env.projectRoot();
    }

    public ProjectData extractProjectData(Path project, Set<String> extraTasks) {
        return extractProjectData(project, computeProjectGradleVersion(project), extraTasks);
    }

    public ProjectData extractProjectData(Path project, GradleVersion gradleVersion, Set<String> extraTasks) {
        Path cacheFile = gradleCacheDir.resolve(projectRootDir.relativize(project).toString().replaceAll("[/\\\\]", "_") + ".dat");
        HashContainer container = new HashContainer(gradleCacheDir, cacheFile.getFileName().toString());
        HashContainer.Entry gradle = container.getEntry("gradle");
        gradle.putString(gradleClassPathHash.get());

        HashContainer.Entry outputEntry = container.getEntry("output");
        outputEntry.putFile(cacheFile);

        // Incorporate previous run data for which files to cache key against.
        HashContainer.Entry hashablesCache = getGradleFilesCache(container, cacheFile);
        if (Files.notExists(cacheFile) || gradle.changed() || hashablesCache == null || hashablesCache.changed() || outputEntry.changed()) {
            JavaVersion javaVersion = getJavaVersionForGradle(gradleVersion);
            Path javaHome = jdkProvider.findOrProvisionJdk(javaVersion);
            extractProjectData(javaHome, project, cacheFile, gradleVersion, extraTasks);
            gradle.pushChanges();
            // Data is extracted, re-build the hashables list and save it.
            Objects.requireNonNull(getGradleFilesCache(container, cacheFile)).pushChanges();
            outputEntry.pushChanges();
        }

        return parseProjectData(cacheFile);
    }

    private @Nullable HashContainer.Entry getGradleFilesCache(HashContainer container, Path oldCache) {
        if (!Files.exists(oldCache)) return null;
        List<Path> projectPaths = collectProjectPaths(oldCache);
        if (projectPaths.isEmpty()) return null;

        HashContainer.Entry entry = container.getEntry("hashables");
        FastStream.of(projectPaths)
                .flatMap(e -> FastStream.of(hashableFiles)
                        .map(e::resolve)
                        .filter(Files::exists)
                )
                .forEach(entry::putFile);
        return entry;
    }

    private List<Path> collectProjectPaths(Path cache) {
        try {
            return collectProjectPaths(parseProjectData(cache));
        } catch (Throwable ex) {
            LOGGER.warn("Failed to collect project paths.", ex);
            return List.of();
        }
    }

    private List<Path> collectProjectPaths(ProjectData data) {
        return FastStream.concat(
                        FastStream.ofNullable(data.projectDir.toPath()),
                        FastStream.ofNullable(data.getData(SubProjectList.class))
                                .flatMap(e -> e.asMap().values())
                                .flatMap(this::collectProjectPaths)
                )
                .toList();
    }

    private ProjectData parseProjectData(Path cache) {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cache))) {
            return (ProjectData) in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException("Failed to read project data.", ex);
        }
    }

    private void extractProjectData(Path javaHome, Path projectDir, Path cacheFile, GradleVersion gradleVersion, Set<String> extraTasks) {
        GradleConnector connector = GradleConnector.newConnector()
                .useGradleVersion(gradleVersion.getVersion())
                .forProjectDirectory(projectDir.toFile());
        try (ProjectConnection connection = connector.connect()) {
            LOGGER.info("Starting Project data extract for {}", projectDir);
            LOGGER.info("Extracting available task information..");
            GradleProject project = connection.model(GradleProject.class)
                    .setJavaHome(javaHome.toFile())
                    .setJvmArguments("-Xmx3G")
                    .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                    .setStandardError(new ConsumingOutputStream(LOGGER::info))
                    .get();
            Set<String> tasksToExecute = FastStream.of(project.getTasks())
                    .map(Task::getName)
                    .filter(extraTasks::contains)
                    .toSet();
            LOGGER.info("Extracting WorkspaceTool project data..");
            try {
                connection
                        .action(new WorkspaceToolModelAction(cacheFile.toFile()))
                        .setJavaHome(javaHome.toFile())
                        .setJvmArguments(FastStream.of("-Xmx3G").concat(extraJvmArgs()))
                        .setEnvironmentVariables(ImmutableMap.copyOf(System.getenv()))
                        .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                        .setStandardError(new ConsumingOutputStream(LOGGER::info))
                        .withArguments("--no-parallel", "-si", "-I", initScriptPath.get().toAbsolutePath().toString())
                        .forTasks(tasksToExecute)
                        .run();
            } finally {
                // TODO only do this when unit testing? Makes attaching debugger easier.
                // Tell the Daemon to shut down after this build.
                //noinspection UnstableApiUsage
                connector.disconnect();
            }
        }
    }

    protected Iterable<String> extraJvmArgs() {
        return FastStream.empty();
    }

    static GradleVersion computeProjectGradleVersion(Path project) {
        GradleVersion gradleVersion = getGradleVersionFromWrapper(project);
        if (gradleVersion == null) {
            LOGGER.info("Could not determine Project gradle version. Using {}", MIN_GRADLE_VERSION);
            return MIN_GRADLE_VERSION;
        }
        if (gradleVersion.compareTo(MIN_GRADLE_VERSION) < 0) {
            LOGGER.info("Forcing project to use Gradle {} instead of {}", MIN_GRADLE_VERSION, gradleVersion);
            return MIN_GRADLE_VERSION;
        }
        return gradleVersion;
    }

    @Nullable
    private static GradleVersion getGradleVersionFromWrapper(Path project) {
        Path wrapperProperties = project.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (Files.notExists(wrapperProperties)) {
            LOGGER.warn("Project does not have a gradle-wrapper.properties file.");
            return null; // Wrapper does not exist.
        }

        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(wrapperProperties)) {
            properties.load(is);
        } catch (IOException ex) {
            LOGGER.warn("Failed to load gradle-wrapper.properties", ex);
            return null;
        }

        String distributionUrl = properties.getProperty("distributionUrl");
        if (distributionUrl == null) {
            LOGGER.warn("Did not find distributionUrl in gradle-wrapper.properties.");
            return null;
        }

        Matcher matcher = WRAPPER_URL_REGEX.matcher(distributionUrl);
        if (!matcher.find()) {
            LOGGER.warn("Wrapper distributionUrl did not match expected regex pattern.");
            return null;
        }

        return GradleVersion.version(matcher.group(1));
    }

    static JavaVersion getJavaVersionForGradle(GradleVersion gradleVersion) {
        if (gradleVersion.compareTo(MIN_GRADLE_USE_J17) >= 0) return JavaVersion.JAVA_17;
        if (gradleVersion.compareTo(MIN_GRADLE_USE_J16) >= 0) return JavaVersion.JAVA_16;
        return JavaVersion.JAVA_1_8;
    }

    private Path buildInitScript() {
        LOGGER.info("Building init script.");
        List<String> lines = new LinkedList<>();
        lines.add("initscript {");
        lines.add("  dependencies {");
        for (Path path : gradleClassPath.get()) {
            lines.add("    classpath files('" + path.toAbsolutePath().toString().replace("\\", "\\\\") + "')");
        }
        lines.add("  }");
        lines.add("}");
        lines.add("apply plugin: " + GRADLE_PLUGIN_CLASS);
        Path temp;
        try {
            temp = Files.createTempFile("wt_init", ".gradle");
            temp.toFile().deleteOnExit();
            Files.write(temp, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write gradle init script temp file.");
        }
        LOGGER.info("Built init script to: {}", temp);
        return temp;
    }

    private List<Path> buildClassPath() {
        String classpath = System.getProperty("net.covers1624.wstool.gradle_classpath");
        if (classpath == null) {
            LOGGER.info(" Using dev metadata for gradle plugin dependencies..");
            return buildClassPathDev();
        }

        return FastStream.of(classpath.split(File.pathSeparator))
                .map(Path::of)
                .toList();
    }

    /**
     * Generate a stable hash of the Gradle classpath, composed of individual class files, not their
     * containers or other metadata. Assuming all classes on this classpath don't embed any version numbers
     * this will only change if the class file changes.
     *
     * @return The hash.
     */
    @SuppressWarnings ("UnstableApiUsage")
    private String hashGradleClassPath() {
        Hasher hasher = Hashing.sha256().newHasher();

        try {
            for (Path path : FastStream.of(gradleClassPath.get()).sorted()) {
                if (Files.isDirectory(path)) {
                    try (Stream<Path> files = Files.walk(path)) {
                        var sorted = FastStream.of(files)
                                .filter(Files::isRegularFile)
                                .filter(e -> e.toString().endsWith(".class"))
                                .sorted(Comparator.comparing(e -> path.relativize(e).toString()))
                                .toList();
                        for (Path clazzFile : sorted) {
                            HashUtils.addToHasher(hasher, clazzFile);
                        }
                    }
                } else if (path.toString().endsWith(".jar")) {
                    try (ZipFile zip = new ZipFile(path.toFile())) {
                        var sorted = FastStream.of(zip.stream())
                                .filter(e -> e.getName().endsWith(".class"))
                                .sorted(Comparator.comparing(ZipEntry::getName))
                                .toList();
                        for (ZipEntry entry : sorted) {
                            try (InputStream is = zip.getInputStream(entry)) {
                                HashUtils.addToHasher(hasher, is);
                            }
                        }
                    }
                } else {
                    throw new RuntimeException("I don't know how to hash this file. " + path);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to hash class path.", ex);
        }
        return hasher.hash().toString();
    }

    private static final Gson GSON = new Gson();
    private static final Type LIST_STRING = new TypeToken<List<String>>() { }.getType();

    private static List<Path> buildClassPathDev() {
        boolean isGradleTesting = Boolean.getBoolean("GradleModelExtractor.isGradleTesting");
        List<String> paths;
        try (InputStream is = GradleModelExtractor.class.getResourceAsStream("/gradle_plugin_data.json")) {
            if (is == null) throw new IllegalStateException("Missing dev data. Did the genGradlePluginMetaDev gradle task not run on import?");

            paths = JsonUtils.parse(GSON, is, LIST_STRING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load dev data.", ex);
        }
        return FastStream.of(paths)
                .map(Path::of)
                .flatMap(p -> {
                    if (!Files.isDirectory(p)) {
                        return List.of(p);
                    }
                    if (isGradleTesting) {
                        return List.of(
                                p.resolve("build/classes/java/main"),
                                p.resolve("build/resources/main")
                        );
                    }

                    return List.of(
                            p.resolve("out/production/classes"),
                            p.resolve("out/production/resources")
                    );
                })
                .filter(Files::exists)
                .toImmutableList();
    }
}

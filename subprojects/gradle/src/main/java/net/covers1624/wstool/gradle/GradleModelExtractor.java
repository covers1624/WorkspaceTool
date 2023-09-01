package net.covers1624.wstool.gradle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.ConsumingOutputStream;
import net.covers1624.quack.util.JavaVersion;
import net.covers1624.quack.util.LazyValue;
import net.covers1624.wstool.api.WorkspaceToolEnvironment;
import net.covers1624.wstool.gradle.api.WorkspaceToolModelAction;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gradle.internal.impldep.com.google.common.collect.ImmutableMap;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.Task;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by covers1624 on 17/5/23.
 */
public class GradleModelExtractor {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String GRADLE_PLUGIN_CLASS = "net.covers1624.wstool.gradle.WorkspaceToolGradlePlugin";

    private final LazyValue<Path> initScriptPath = new LazyValue<>(GradleModelExtractor::buildInitScript);

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

//    public ProjectData extractProjectData(Path project, Set<String> extraTasks) {
//        GradleVersion gradleVersion = computeProjectGradleVersion(project);
//
//    }

    @VisibleForTesting // TODO, this should not be used for tests, when the rest of this class is built, mock the env and call the regular extractProjectData function.
    public ProjectData extractProjectData(Path projectDir, Path cacheFile, GradleVersion gradleVersion, Set<String> extraTasks) {
        GradleConnector connector = GradleConnector.newConnector()
                .useGradleVersion(gradleVersion.getVersion())
                .forProjectDirectory(projectDir.toFile());
        try (ProjectConnection connection = connector.connect()) {
            LOGGER.info("Starting Project data extract..");
            LOGGER.info("Extracting available task information..");
            GradleProject project = connection.model(GradleProject.class)
                    .setJvmArguments("-Xmx3G", "-Dorg.gradle.daemon=false")
                    .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                    .setStandardError(new ConsumingOutputStream(LOGGER::info))
                    .get();
            Set<String> tasksToExecute = FastStream.of(project.getTasks())
                    .map(Task::getName)
                    .filter(extraTasks::contains)
                    .toSet();
            LOGGER.info("Extracting WorkspaceTool project data..");
            connection
                    .action(new WorkspaceToolModelAction(cacheFile.toFile(), new HashSet<>()))
                    .setJvmArguments("-Xmx3G", "-Dorg.gradle.daemon=false")
                    .setEnvironmentVariables(ImmutableMap.copyOf(System.getenv()))
                    .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                    .setStandardError(new ConsumingOutputStream(LOGGER::info))
                    .withArguments("-si", "-I", initScriptPath.get().toAbsolutePath().toString())
                    .forTasks(tasksToExecute)
                    .run();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cacheFile))) {
            return (ProjectData) in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException("Failed to read cache file after Gradle execution.", ex);
        }
    }

    private static GradleVersion computeProjectGradleVersion(Path project) {
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

    @VisibleForTesting
    static JavaVersion getJavaVersionForGradle(GradleVersion gradleVersion) {
        if (gradleVersion.compareTo(MIN_GRADLE_USE_J17) >= 0) return JavaVersion.JAVA_17;
        if (gradleVersion.compareTo(MIN_GRADLE_USE_J16) >= 0) return JavaVersion.JAVA_16;
        return JavaVersion.JAVA_1_8;
    }

    private static Path buildInitScript() {
        LOGGER.info("Building init script.");
        List<String> lines = new LinkedList<>();
        lines.add("initscript {");
        lines.add("  dependencies {");
        for (Path path : buildJarPath()) {
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

    private static List<Path> buildJarPath() {
        if (WorkspaceToolEnvironment.WSTOOL_MANIFEST != null) {
            throw new NotImplementedException("Not runnable out-of-dev yet.");
        } else {
            LOGGER.info(" Using dev metadata for gradle plugin dependencies..");
            return buildJarPathDev();
        }
    }

    private static final Gson GSON = new Gson();
    private static final Type LIST_STRING = new TypeToken<List<String>>() { }.getType();

    private static List<Path> buildJarPathDev() {
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
                    return List.of(
                            p.resolve("out/production/classes"),
                            p.resolve("out/production/resources")
                    );
                })
                .filter(Files::exists)
                .toList();
    }
}

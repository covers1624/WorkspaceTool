package net.covers1624.wstool.gradle;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.ConsumingOutputStream;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.JdkProvider;
import org.gradle.internal.impldep.com.google.common.collect.ImmutableMap;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Created by covers1624 on 10/1/25.
 */
public class GradleTaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradleTaskExecutor.class);

    private final JdkProvider jdkProvider;

    public GradleTaskExecutor(Environment env) {
        jdkProvider = env.getService(JdkProvider.class);
    }

    public void runTask(Path projectDir, String task) {
        var gradleVersion = GradleModelExtractor.computeProjectGradleVersion(projectDir);
        var javaVersion = GradleModelExtractor.getJavaVersionForGradle(gradleVersion);
        var javaHome = jdkProvider.findOrProvisionJdk(javaVersion);
        GradleConnector connector = GradleConnector.newConnector()
                .useGradleVersion(gradleVersion.getVersion())
                .forProjectDirectory(projectDir.toFile());
        try (ProjectConnection connection = connector.connect()) {
            LOGGER.info("Running Gradle task {} on {}", task, projectDir);
            connection.newBuild()
                    .forTasks(task)
                    .setJavaHome(javaHome.toFile())
                    .setJvmArguments("-Xmx3G")
                    .setEnvironmentVariables(ImmutableMap.copyOf(System.getenv()))
                    .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                    .setStandardError(new ConsumingOutputStream(LOGGER::info))
                    .run();
        } finally {
            connector.disconnect();
        }
    }

    public void tryRunTasks(Path projectDir, String... tasks) {
        var gradleVersion = GradleModelExtractor.computeProjectGradleVersion(projectDir);
        var javaVersion = GradleModelExtractor.getJavaVersionForGradle(gradleVersion);
        var javaHome = jdkProvider.findOrProvisionJdk(javaVersion);
        GradleConnector connector = GradleConnector.newConnector()
                .useGradleVersion(gradleVersion.getVersion())
                .forProjectDirectory(projectDir.toFile());
        try (ProjectConnection connection = connector.connect()) {
            LOGGER.info("Running Gradle tasks {} on {}", tasks, projectDir);
            GradleProject project = connection.model(GradleProject.class)
                    .setJavaHome(javaHome.toFile())
                    .setJvmArguments("-Xmx3G")
                    .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                    .setStandardError(new ConsumingOutputStream(LOGGER::info))
                    .get();

            var toExec = FastStream.of(tasks)
                    .filter(e -> doesTaskExist(project, e))
                    .toArray(String[]::new);

            connection.newBuild()
                    .forTasks(toExec)
                    .setJavaHome(javaHome.toFile())
                    .setJvmArguments("-Xmx3G")
                    .setEnvironmentVariables(ImmutableMap.copyOf(System.getenv()))
                    .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                    .setStandardError(new ConsumingOutputStream(LOGGER::info))
                    .run();
        } finally {
            connector.disconnect();
        }
    }

    private boolean doesTaskExist(GradleProject project, String taskPath) {
        if (ColUtils.anyMatch(project.getTasks(), e -> e.getPath().equals(taskPath))) return true;

        for (GradleProject child : project.getChildren()) {
            if (doesTaskExist(child, taskPath)) return true;
        }

        return false;
    }
}

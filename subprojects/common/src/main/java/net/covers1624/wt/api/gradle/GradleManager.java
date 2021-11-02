package net.covers1624.wt.api.gradle;

import net.covers1624.wt.api.gradle.data.ExtraData;

import java.nio.file.Path;
import java.util.Set;

/**
 * Core manager for Gradle integration. Allows adding things to Gradle's Classpath,
 * adding DataBuilders for {@link ExtraData}'s, and Gradle Tasks to execute before
 * anything is done.
 * <p>
 * TODO, we now completely depend on gradle. Some restrictions could be lifted here.
 * Created by covers1624 on 17/6/19.
 */
public interface GradleManager {

    String MIN_GRADLE_VERSION = "4.10.3";

    /**
     * Provides what ever path the specified class was loaded from
     * to WorkspaceTool's Gradle InitScript for use with Model building.
     *
     * @param clazz The class.
     */
    void includeClassMarker(Class<?> clazz);

    /**
     * Provides what ever path the specified class was loaded from
     * to WorkspaceTool's Gradle InitScript for use with Model building.
     *
     * @param clazz The class.
     */
    void includeClassMarker(String clazz);

    /**
     * Provides what ever path the specified resource was loaded from
     * to WorkspaceTool's Gradle InitScript for use with Model building.
     *
     * @param resource The resource.
     */
    void includeResourceMarker(String resource);

    /**
     * Lets WorkspaceTool's Gradle plugin know about a DataBuilder.
     * Please note that due to using core gradle classes from the DataBuilder interface.
     * It is not possible to use `SomeClass.class.getName()` for this method and is why
     * this method does not take a Class.
     *
     * @param className The class name for the builder.
     */
    void addDataBuilder(String className);

    /**
     * Lets WorkspaceTool know about any tasks that need to be executed
     * before any data can be extracted from the build.
     *
     * @param tasks The tasks.
     */
    void executeBefore(String... tasks);

    /**
     * @return The DataBuilders to run with.
     */
    Set<String> getDataBuilders();

    /**
     * @return The Tasks to execute before extracting data.
     */
    Set<String> getExecuteBefore();

    /**
     * @return The Path to the generated Gradle InitScript
     */
    Path getInitScript();

    /**
     * Returns the Gradle Version to use for the specified project.
     * <p>
     * If the project uses a Gradle Version earlier than {@link #MIN_GRADLE_VERSION},
     * then this function will return {@link #MIN_GRADLE_VERSION}. Otherwise, the configured
     * gradle wrapper version for the project will be returned.
     *
     * @param projectDir The Project to get the Gradle Version for.
     * @return The Gradle Version.
     */
    String getGradleVersionForProject(Path projectDir);
}

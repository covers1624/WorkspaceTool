package net.covers1624.wt.gradle.builder;

import net.covers1624.quack.maven.MavenNotation;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Visits dependencies in a given configuration.
 * <p>
 * Created by covers1624 on 2/8/19.
 */
public interface ConfigurationVisitor {

    /**
     * Start visiting a given {@link Configuration}.
     * <p>
     * Will not be called multiple times without {@link #visitEnd()} being called.
     *
     * @param config The configuration being visited.
     */
    default void visitStart(Configuration config) { }

    /**
     * Called once per Module dependency.
     *
     * @param notation Notation describing this dependency.
     * @param classes  The classes for this notation.
     * @param sources  The sources for this notation.
     * @param javadoc  The javadoc for this notation.
     */
    default void visitModuleDependency(MavenNotation notation, File classes, @Nullable File sources, @Nullable File javadoc) { }

    /**
     * Visits a {@link SourceSet} dependency.
     *
     * @param ssOutput The {@link SourceSetOutput}.
     */
    default void visitSourceSetDependency(SourceSetOutput ssOutput) { }

    /**
     * Visits a {@link Project} dependency.
     *
     * @param project The {@link Project}.
     */
    default void visitProjectDependency(Project project) { }

    /**
     * Finished visiting the current {@link Configuration}.
     */
    default void visitEnd() { }
}

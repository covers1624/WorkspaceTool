package net.covers1624.wt.api.dependency;

import net.covers1624.quack.maven.MavenNotation;

import java.nio.file.Path;

/**
 * Represents a Dependency that exists on Maven.
 *
 * TODO, this could probably return a List of Paths for classes, sources and javadoc.
 * Created by covers1624 on 30/6/19.
 */
public interface MavenDependency extends Dependency {

    /**
     * The MavenNotation for this Dependency.
     *
     * @return The notation.
     */
    MavenNotation getNotation();

    /**
     * @return The Path to the Classes artifact.
     */
    Path getClasses();

    /**
     * @return The Path to the Javadoc artifact.
     */
    Path getJavadoc();

    /**
     * @return The Path to the Sources artifact.
     */
    Path getSources();

    /**
     * Sets the MavenNotation for this Dependency.
     *
     * @param notation The notation.
     * @return The same MavenDependency.
     */
    MavenDependency setNotation(MavenNotation notation);

    /**
     * Sets the Path for the Classes artifact.
     *
     * @param path The path.
     * @return The same MavenDependency.
     */
    MavenDependency setClasses(Path path);

    /**
     * Sets the Path for the Javadoc artifact.
     *
     * @param path The path.
     * @return The same MavenDependency.
     */
    MavenDependency setJavadoc(Path path);

    /**
     * Sets the Path for the Sources artifact.
     *
     * @param path The path.
     * @return The same MavenDependency.
     */
    MavenDependency setSources(Path path);

    /**
     * {@inheritDoc}
     */
    @Override
    MavenDependency setExport(boolean value);

    /**
     * {@inheritDoc}
     */
    @Override
    MavenDependency copy();
}

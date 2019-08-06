package net.covers1624.wt.api.impl.dependency;

import net.covers1624.wt.api.data.ConfigurationData;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.util.MavenNotation;

import java.nio.file.Path;

/**
 * Created by covers1624 on 30/6/19.
 */
public class MavenDependencyImpl extends AbstractDependency implements MavenDependency {

    private MavenNotation notation;
    private Path classes;
    private Path javadoc;
    private Path sources;

    public MavenDependencyImpl() {
        super();
    }

    public MavenDependencyImpl(ConfigurationData.MavenDependency other) {
        this();
        setNotation(other.mavenNotation);
        if (other.classes != null) {
            setClasses(other.classes.toPath());
        }
        if (other.javadoc != null) {
            setJavadoc(other.javadoc.toPath());
        }
        if (other.sources != null) {
            setSources(other.sources.toPath());
        }
    }

    public MavenDependencyImpl(MavenDependency other) {
        super(other);
        setNotation(other.getNotation());
        setClasses(other.getClasses());
        setJavadoc(other.getJavadoc());
        setSources(other.getSources());
    }

    @Override
    public MavenDependency setExport(boolean export) {
        super.setExport(export);
        return this;
    }

    @Override
    public MavenNotation getNotation() {
        return notation;
    }

    @Override
    public Path getClasses() {
        return classes;
    }

    @Override
    public Path getJavadoc() {
        return javadoc;
    }

    @Override
    public Path getSources() {
        return sources;
    }

    @Override
    public MavenDependency setNotation(MavenNotation notation) {
        this.notation = notation;
        return this;
    }

    @Override
    public MavenDependency setClasses(Path path) {
        this.classes = path;
        return this;
    }

    @Override
    public MavenDependency setJavadoc(Path path) {
        this.javadoc = path;
        return this;
    }

    @Override
    public MavenDependency setSources(Path path) {
        this.sources = path;
        return this;
    }

    @Override
    public MavenDependency copy() {
        return new MavenDependencyImpl(this);
    }
}

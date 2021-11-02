package net.covers1624.wt.api.impl.dependency;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.gradle.data.ConfigurationData;
import net.covers1624.wt.api.dependency.MavenDependency;

import java.nio.file.Path;
import java.util.Objects;

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
    public int hashCode() {
        int i = 0;
        i = 31 * i + getNotation().hashCode();
        i = 31 * i + (getExport() ? 1 : 0);
        i = 31 * i + (getSources() != null ? getSources().hashCode() : 0);
        i = 31 * i + (getJavadoc() != null ? getJavadoc().hashCode() : 0);
        i = 31 * i + (getSources() != null ? getSources().hashCode() : 0);
        return i;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof MavenDependency)) {
            return false;
        }
        MavenDependency other = (MavenDependency) obj;
        return other.getNotation().equals(getNotation())//
                && other.getExport() == getExport()//
                && Objects.equals(other.getClasses(), getClasses())//
                && Objects.equals(other.getJavadoc(), getJavadoc())//
                && Objects.equals(other.getSources(), getSources());
    }

    @Override
    public String toString() {
        return "MavenDependency: " + getNotation().toString();
    }

    @Override
    public MavenDependency copy() {
        return new MavenDependencyImpl(this);
    }
}

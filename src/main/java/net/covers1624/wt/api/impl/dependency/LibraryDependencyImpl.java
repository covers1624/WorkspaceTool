package net.covers1624.wt.api.impl.dependency;

import net.covers1624.wt.api.dependency.LibraryDependency;
import net.covers1624.wt.api.dependency.MavenDependency;

/**
 * Created by covers1624 on 30/6/19.
 */
public class LibraryDependencyImpl extends AbstractDependency implements LibraryDependency {

    private String libraryName;
    private MavenDependency mavenDependency;

    public LibraryDependencyImpl() {
    }

    public LibraryDependencyImpl(LibraryDependency other) {
        this();
        setLibraryName(other.getLibraryName());
        setMavenDependency(other.getMavenDependency());
    }

    @Override
    public LibraryDependency setExport(boolean export) {
        super.setExport(export);
        return this;
    }

    @Override
    public String getLibraryName() {
        return libraryName;
    }

    @Override
    public MavenDependency getMavenDependency() {
        return mavenDependency;
    }

    @Override
    public LibraryDependency setLibraryName(String name) {
        this.libraryName = name;
        return this;
    }

    @Override
    public LibraryDependency setMavenDependency(MavenDependency dependency) {
        this.mavenDependency = dependency;
        return this;
    }

    @Override
    public int hashCode() {
        int i = 0;
        i = 31 * i + getMavenDependency().hashCode();
        i = 31 * i + getLibraryName().hashCode();
        i = 31 * i + (getExport() ? 1 : 0);
        return i;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof LibraryDependency)) {
            return false;
        }
        LibraryDependency other = (LibraryDependency) obj;
        return other.getMavenDependency().equals(getMavenDependency())//
                && other.getLibraryName().equals(getLibraryName())//
                && other.getExport() == getExport();
    }

    @Override
    public String toString() {
        return "LibraryDependency: " + mavenDependency.toString();
    }

    @Override
    public LibraryDependency copy() {
        return new LibraryDependencyImpl(this);
    }
}

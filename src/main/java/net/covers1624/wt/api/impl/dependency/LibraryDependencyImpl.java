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
    public boolean isExport() {
        return super.isExport();
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
    public LibraryDependency copy() {
        return new LibraryDependencyImpl(this);
    }
}

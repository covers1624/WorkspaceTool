/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.dependency;

import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.LibraryDependency;

/**
 * Created by covers1624 on 30/6/19.
 */
public class LibraryDependencyImpl extends AbstractDependency implements LibraryDependency {

    private String libraryName;
    private Dependency dependency;

    public LibraryDependencyImpl() {
    }

    public LibraryDependencyImpl(LibraryDependency other) {
        this();
        setLibraryName(other.getLibraryName());
        setDependency(other.getDependency());
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
    public Dependency getDependency() {
        return dependency;
    }

    @Override
    public LibraryDependency setLibraryName(String name) {
        this.libraryName = name;
        return this;
    }

    @Override
    public LibraryDependency setDependency(Dependency dependency) {
        this.dependency = dependency;
        return this;
    }

    @Override
    public int hashCode() {
        int i = 0;
        i = 31 * i + getDependency().hashCode();
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
        return other.getDependency().equals(getDependency())//
                && other.getLibraryName().equals(getLibraryName())//
                && other.getExport() == getExport();
    }

    @Override
    public String toString() {
        return "LibraryDependency: " + dependency.toString();
    }

    @Override
    public LibraryDependency copy() {
        return new LibraryDependencyImpl(this);
    }
}

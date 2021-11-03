/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.dependency;

import com.google.common.base.MoreObjects;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.dependency.ScalaSdkDependency;
import net.covers1624.wt.util.ScalaVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by covers1624 on 7/11/19.
 */
public class ScalaSdkDependencyImpl extends AbstractDependency implements ScalaSdkDependency {

    private ScalaVersion scalaVersion;
    private String version;
    private MavenDependency scalac;
    private final List<MavenDependency> libraries = new ArrayList<>();

    public ScalaSdkDependencyImpl() {
        super();
    }

    public ScalaSdkDependencyImpl(ScalaSdkDependency other) {
        super(other);
        setScalaVersion(other.getScalaVersion());
        setVersion(other.getVersion());
        setScalac(other.getScalac());
        setLibraries(other.getLibraries());
    }

    @Override
    public ScalaVersion getScalaVersion() {
        return scalaVersion;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public MavenDependency getScalac() {
        return scalac;
    }

    @Override
    public List<MavenDependency> getLibraries() {
        return libraries;
    }

    @Override
    public ScalaSdkDependency setScalaVersion(ScalaVersion version) {
        this.scalaVersion = version;
        return this;
    }

    @Override
    public ScalaSdkDependency setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public ScalaSdkDependency setScalac(MavenDependency scalac) {
        this.scalac = scalac;
        return this;
    }

    @Override
    public ScalaSdkDependency addLibrary(MavenDependency dependency) {
        libraries.add(dependency);
        return this;
    }

    @Override
    public ScalaSdkDependency setLibraries(List<MavenDependency> libraries) {
        this.libraries.clear();
        this.libraries.addAll(libraries);
        return this;
    }

    @Override
    public ScalaSdkDependency setExport(boolean export) {
        super.setExport(export);
        return this;
    }

    @Override
    public int hashCode() {
        int i = 0;
        i = 31 * i + getScalaVersion().ordinal();
        i = 31 * i + getVersion().hashCode();
        i = 31 * i + getScalac().hashCode();
        i = 31 * i + getLibraries().hashCode();
        return i;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof ScalaSdkDependency)) {
            return false;
        }
        ScalaSdkDependency other = (ScalaSdkDependency) obj;
        return other.getScalaVersion().equals(getScalaVersion())
                && other.getVersion().equals(getVersion())
                && other.getScalac().equals(getScalac())
                && other.getLibraries().equals(getLibraries());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("scalaVersion", scalaVersion)
                .add("version", version)
                .add("scalac", scalac)
                .add("libraries", libraries).toString();
    }

    @Override
    public ScalaSdkDependency copy() {
        return new ScalaSdkDependencyImpl(this);
    }

}

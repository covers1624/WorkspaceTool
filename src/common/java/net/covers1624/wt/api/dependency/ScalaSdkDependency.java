package net.covers1624.wt.api.dependency;

import com.google.common.collect.Iterables;
import net.covers1624.wt.util.scala.ScalaVersion;

import java.util.Collections;
import java.util.List;

/**
 * Created by covers1624 on 7/11/19.
 */
public interface ScalaSdkDependency extends Dependency {

    ScalaVersion getScalaVersion();

    String getVersion();

    MavenDependency getScalac();

    List<MavenDependency> getLibraries();

    default Iterable<MavenDependency> getClasspath() {
        return Iterables.concat(Collections.singleton(getScalac()), getLibraries());
    }

    ScalaSdkDependency setScalaVersion(ScalaVersion version);

    ScalaSdkDependency setVersion(String version);

    ScalaSdkDependency setScalac(MavenDependency scalac);

    ScalaSdkDependency addLibrary(MavenDependency dependency);

    ScalaSdkDependency setLibraries(List<MavenDependency> libraries);


    /**
     * {@inheritDoc}
     */
    @Override
    ScalaSdkDependency setExport(boolean value);

    /**
     * {@inheritDoc}
     */
    @Override
    ScalaSdkDependency copy();

}

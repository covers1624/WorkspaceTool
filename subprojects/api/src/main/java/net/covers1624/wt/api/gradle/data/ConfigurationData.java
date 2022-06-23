/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.gradle.data;

import com.google.common.base.MoreObjects;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.event.VersionedClass;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data class for holding Configuration data from Gradle.
 * <p>
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (2)
public class ConfigurationData implements Serializable {

    public final String name;

    public final Set<String> extendsFrom = new HashSet<>();
    public final List<Dependency> dependencies = new ArrayList<>();

    public ConfigurationData(String name) {
        this.name = name;
    }

    @VersionedClass (1)
    public static abstract class Dependency implements Serializable { }

    @VersionedClass (1)
    public static class MavenDependency extends Dependency {

        public final MavenNotation mavenNotation;

        public final File classes;
        @Nullable
        public final File sources;
        @Nullable
        public final File javadoc;

        public MavenDependency(MavenNotation mavenNotation, File classes, @Nullable File sources, @Nullable File javadoc) {
            this.mavenNotation = mavenNotation;
            this.classes = classes;
            this.sources = sources;
            this.javadoc = javadoc;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("maven_notation", mavenNotation)
                    .toString();
        }
    }

    @VersionedClass (1)
    public static class SourceSetDependency extends Dependency {

        public final String name;

        public SourceSetDependency(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("name", name)
                    .toString();
        }
    }

    public static class ProjectDependency extends Dependency {

        public final String project;

        public ProjectDependency(String project) {
            this.project = project;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("project", project)
                    .toString();
        }
    }

}

package net.covers1624.wstool.gradle.api.data;

import net.covers1624.quack.maven.MavenNotation;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 9/9/23.
 */
public class ConfigurationData extends Data {

    public final String name;
    public final List<Dependency> dependencies = new ArrayList<>();

    public ConfigurationData(String name) {
        this.name = name;
    }

    public static abstract class Dependency implements Serializable { }

    public static class MavenDependency extends Dependency {

        public final MavenNotation mavenNotation;

        public final Map<String, File> files = new HashMap<>();
        public final List<MavenDependency> children = new ArrayList<>();

        public MavenDependency(MavenNotation mavenNotation) {
            this.mavenNotation = mavenNotation;
        }

        @Override
        public String toString() {
            return "MavenDependency(" + mavenNotation+ ", files: " + files.size() + ", children: " + children.size() + ")";
        }
    }

    public static class SourceSetDependency extends Dependency {

        public final SourceSetData sourceSet;

        public SourceSetDependency(SourceSetData sourceSet) {
            this.sourceSet = sourceSet;
        }

        @Override
        public String toString() {
            return "SourceSetDependency(" + sourceSet.name + ")";
        }
    }

    public static class ProjectDependency extends Dependency {

        public final ProjectData project;

        public ProjectDependency(ProjectData project) {
            this.project = project;
        }

        @Override
        public String toString() {
            return "ProjectDependency(" + project.name + ")";
        }
    }
}

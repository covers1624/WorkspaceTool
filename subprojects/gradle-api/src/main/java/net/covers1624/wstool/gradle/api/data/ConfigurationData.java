package net.covers1624.wstool.gradle.api.data;

import net.covers1624.quack.maven.MavenNotation;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * Created by covers1624 on 9/9/23.
 */
public class ConfigurationData extends Data {

    public final String name;
    public final Set<Dependency> dependencies = new LinkedHashSet<>();

    public ConfigurationData(String name) {
        this.name = name;
    }

    public static abstract class Dependency implements Serializable { }

    public static class MavenDependency extends Dependency {

        public final MavenNotation mavenNotation;

        public final Map<String, File> files = new HashMap<>();
        public final Set<MavenDependency> children = new LinkedHashSet<>();

        public MavenDependency(MavenNotation mavenNotation) {
            this.mavenNotation = mavenNotation;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            MavenDependency that = (MavenDependency) o;
            return mavenNotation.equals(that.mavenNotation) && files.equals(that.files) && children.equals(that.children);
        }

        @Override
        public int hashCode() {
            int result = mavenNotation.hashCode();
            result = 31 * result + files.hashCode();
            result = 31 * result + children.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MavenDependency(" + mavenNotation + ", files: " + files.size() + ", children: " + children.size() + ")";
        }
    }

    public static class SourceSetDependency extends Dependency {

        public final SourceSetData sourceSet;

        public SourceSetDependency(SourceSetData sourceSet) {
            this.sourceSet = sourceSet;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            SourceSetDependency that = (SourceSetDependency) o;
            return sourceSet.equals(that.sourceSet);
        }

        @Override
        public int hashCode() {
            return sourceSet.hashCode();
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
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            ProjectDependency that = (ProjectDependency) o;
            return project.equals(that.project);
        }

        @Override
        public int hashCode() {
            return project.hashCode();
        }

        @Override
        public String toString() {
            return "ProjectDependency(" + project.name + ")";
        }
    }

    public static class FilesDependency extends Dependency {

        public final List<File> files = new ArrayList<>();

        public FilesDependency(Iterable<File> files) {
            files.forEach(this.files::add);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            FilesDependency that = (FilesDependency) o;
            return files.equals(that.files);
        }

        @Override
        public int hashCode() {
            return files.hashCode();
        }

        @Override
        public String toString() {
            return "FilesDependency(files: " + files.size() + ")";
        }
    }
}

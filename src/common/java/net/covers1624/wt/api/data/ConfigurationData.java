package net.covers1624.wt.api.data;

import com.google.common.base.MoreObjects;
import net.covers1624.wt.event.VersionedClass;
import net.covers1624.wt.util.MavenNotation;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data class for holding Configuration data from Gradle.
 *
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (1)
public class ConfigurationData implements Serializable {

    public String name;
    public boolean transitive;

    public Set<String> extendsFrom = new HashSet<>();
    public List<Dependency> dependencies = new ArrayList<>();

    @VersionedClass (1)
    public static abstract class Dependency implements Serializable {}

    @VersionedClass (1)
    public static class MavenDependency extends Dependency {

        public MavenNotation mavenNotation;

        public File classes;
        public File sources;
        public File javadoc;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)//
                    .add("maven_notation", mavenNotation)//
                    .toString();
        }
    }

    @VersionedClass (1)
    public static class SourceSetDependency extends Dependency {

        public String name;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)//
                    .add("name", name)//
                    .toString();
        }
    }

}

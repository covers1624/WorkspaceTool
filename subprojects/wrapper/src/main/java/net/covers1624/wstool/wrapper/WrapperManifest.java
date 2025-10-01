package net.covers1624.wstool.wrapper;

import com.google.gson.annotations.JsonAdapter;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.quack.gson.MavenNotationAdapter;
import net.covers1624.quack.maven.MavenNotation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 7/5/25.
 */
public final class WrapperManifest {

    private final @Nullable String wrapperVersionFilter;
    private final @Nullable JavaVersion javaVersion;
    private final @Nullable Map<String, List<Dependency>> classPaths;
    private final @Nullable List<String> javaArgs;

    public WrapperManifest(@Nullable String wrapperVersionFilter, @Nullable JavaVersion javaVersion, @Nullable Map<String, List<Dependency>> classPaths, @Nullable List<String> javaArgs) {
        this.wrapperVersionFilter = wrapperVersionFilter;
        this.javaVersion = javaVersion;
        this.classPaths = classPaths;
        this.javaArgs = javaArgs;
    }

    public String wrapperVersionFilter() {
        return requireNonNull(wrapperVersionFilter);
    }

    public JavaVersion javaVersion() {
        return requireNonNull(javaVersion);
    }

    public Map<String, List<Dependency>> classPaths() {
        return classPaths != null ? classPaths : Collections.emptyMap();
    }

    public List<String> javaArgs() {
        return javaArgs != null ? javaArgs : Collections.emptyList();
    }

    public static class Dependency {

        @JsonAdapter (MavenNotationAdapter.class)
        private final @Nullable MavenNotation artifact;
        private final @Nullable String sha256;
        private final @Nullable Integer size;

        public Dependency(@Nullable MavenNotation artifact, @Nullable String sha256, @Nullable Integer size) {
            this.artifact = artifact;
            this.sha256 = sha256;
            this.size = size;
        }

        public MavenNotation artifact() {
            return requireNonNull(artifact);
        }

        public String sha256() {
            return requireNonNull(sha256);
        }

        public int size() {
            return requireNonNull(size);
        }
    }
}

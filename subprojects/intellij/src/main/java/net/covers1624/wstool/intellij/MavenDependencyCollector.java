package net.covers1624.wstool.intellij;

import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.SourceSet;
import net.covers1624.wstool.api.workspace.runs.EvalValue;
import net.covers1624.wstool.api.workspace.runs.RunConfig;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Collects all dependencies in use by the Intellij project.
 * <p>
 * There are 3 primary goals of this.
 * First, to ensure we only use the highest version of each dependency available.</br>
 * Second, to collect them and emit Intellij library xmls.
 * Third, hardlink all dependency paths into the .wstool cache directory of the project.
 * <p>
 * Hard-linking is performed for all libraries (if possible), to mitigate the effects of gradle
 * cache cleanup, breaking workspaces.
 * <p>
 * Created by covers1624 on 5/20/25.
 */
public class MavenDependencyCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDependencyCollector.class);

    private final Map<MavenNotation, CollectedEntry> collectedDependencies = new HashMap<>();
    private final Map<Dependency.MavenDependency, CollectedEntry> depToEntry = new HashMap<>();

    private boolean hardlinkFailed = false;

    public void collectFrom(Module module) {
        for (SourceSet ss : module.sourceSets().values()) {
            collectDeps(ss.runtimeDependencies());
            collectDeps(ss.compileDependencies());
        }
        for (Module subModule : module.subModules().values()) {
            collectFrom(subModule);
        }
    }

    public void collectFrom(RunConfig runConfig) {
        runConfig.vmArgs().toList().forEach(this::collectFrom);
        runConfig.args().toList().forEach(this::collectFrom);
        runConfig.sysProps().toMap().values().forEach(this::collectFrom);
        runConfig.envVars().toMap().values().forEach(this::collectFrom);
    }

    private void collectFrom(EvalValue evalValue) {
        collectDeps(evalValue.collectDependencies());
    }

    private void collectDeps(Iterable<Dependency> deps) {
        for (Dependency dep : deps) {
            if (dep instanceof Dependency.MavenDependency mavenDep) {
                collectDep(mavenDep);
            }
        }
    }

    private void collectDep(Dependency.MavenDependency dep) {
        var ent = collectedDependencies.computeIfAbsent(getWithoutVersion(dep.notation()), CollectedEntry::new);
        ent.consume(dep);
        depToEntry.put(dep, ent);
    }

    public @Nullable CollectedEntry lookup(Dependency.MavenDependency dep) {
        return depToEntry.get(dep);
    }

    private static MavenNotation getWithoutVersion(MavenNotation notation) {
        return notation.withVersion(null);
    }

    public void hardlinkToCacheDir(Path path) {
        Path librariesDir = path.resolve("libraries");

        try {
            for (CollectedEntry dep : collectedDependencies.values()) {
                Path cacheLibDir = librariesDir.resolve(dep.notation.toModulePath()).resolve(requireNonNull(dep.currentVersion));
                dep.classes = linkIntoDir(cacheLibDir, dep.classes);
                dep.javadoc = linkIntoDir(cacheLibDir, dep.javadoc);
                dep.sources = linkIntoDir(cacheLibDir, dep.sources);
            }
        } catch (IOException ex) {
            LOGGER.error("Error whilst linking files.", ex);
        }
    }

    public Iterable<CollectedEntry> collectedEntries() {
        return Collections.unmodifiableCollection(collectedDependencies.values());
    }

    private @Nullable Path linkIntoDir(Path dir, @Nullable Path originalFile) throws IOException {
        if (originalFile == null) return null;

        Path linkedFile = dir.resolve(originalFile.getFileName());
        // The gradle file was deleted from gradle cache.
        if (Files.exists(linkedFile) && Files.notExists(originalFile)) return linkedFile;
        // TODO, we should check if the inode of hardlinkFile and originalFile are identical and bail.
        //       We want to delete it here to restore the hardlink if gradle re-caches the file to de-dupe
        //       and reclaim disk space for the user.
        Files.deleteIfExists(linkedFile);
        if (!hardlinkFailed) {
            try {
                Files.createLink(IOUtils.makeParents(linkedFile), originalFile);
                return linkedFile;
            } catch (IOException ex) {
                LOGGER.warn("Failed to hardlink dependencies into the workspace. You will be vulnerable to Gradle cache expiring in-use dependencies.", ex);
                hardlinkFailed = true;
            }
        }
        // Hard linking failed, lets fallback to a symlink.
        // This mostly exists for the test framework, so we can make class paths and whatnot in run configs stable.
        // TODO perhaps we can have some proper config driven property for this.
        Files.createSymbolicLink(IOUtils.makeParents(linkedFile), originalFile);
        return linkedFile;
    }

    public static class CollectedEntry {

        private final MavenNotation notation;

        private @Nullable String currentVersion;

        private @Nullable Path classes;
        private @Nullable Path javadoc;
        private @Nullable Path sources;

        public CollectedEntry(MavenNotation notation) {
            this.notation = notation;
        }

        public void consume(Dependency.MavenDependency dependency) {
            String newVersion = requireNonNull(dependency.notation().version);
            // Ignore non-upgrades.
            if (!isUpgrade(currentVersion, newVersion)) return;

            // If we are upgrading, nuke the paths we have.
            if (!newVersion.equals(currentVersion)) {
                currentVersion = newVersion;
                classes = null;
                javadoc = null;
                sources = null;
            }

            classes = dependency.files().get("classes");
            javadoc = dependency.files().get("javadoc");
            sources = dependency.files().get("sources");
        }

        private static boolean isUpgrade(@Nullable String a, @Nullable String b) {
            if (a == null && b != null) {
                return true;
            }
            if (a == null || b == null) return false;

            return new DefaultArtifactVersion(a).compareTo(new DefaultArtifactVersion(b)) <= 0;
        }

        public String name() {
            return notation.withVersion(currentVersion).toString();
        }

        public @Nullable Path classes() {
            return classes;
        }

        public Document buildDocument() {
            Element component = new Element("component")
                    .setAttribute("name", "libraryTable");

            Element library = new Element("library")
                    .setAttribute("name", name());

            library.addContent(emitElementForJar("CLASSES", classes));
            library.addContent(emitElementForJar("JAVADOC", javadoc));
            library.addContent(emitElementForJar("SOURCES", sources));
            component.addContent(library);
            return new Document(component);
        }

        private static Element emitElementForJar(String name, @Nullable Path jar) {
            Element element = new Element(name);
            if (jar != null) {
                element.addContent(new Element("root")
                        .setAttribute("url", IJUtils.fileUrl(jar)));
            }
            return element;
        }
    }
}

package net.covers1624.wstool.intellij.workspace;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.intellij.MavenDependencyCollector;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.covers1624.wstool.intellij.IJUtils.*;

/**
 * Created by covers1624 on 5/4/25.
 */
public abstract class IJModule {

    protected final ModulePath path;

    private final List<Path> excludes = new ArrayList<>();

    public IJModule(ModulePath path) {
        this.path = path;
    }

    public String name() {
        return path.name();
    }

    public List<Path> excludes() {
        return excludes;
    }

    public boolean isForTests() {
        return false;
    }

    public List<ContentPath> getContentPaths() {
        return FastStream.of(excludes).map(ContentPath::exclude).toList();
    }

    public List<DependencyEntry> getDependencyEntries() {
        return List.of();
    }

    public final Document buildDocument(Environment env, MavenDependencyCollector collector) {
        Element module = new Element("module")
                .setAttribute("type", "JAVA_MODULE")
                .setAttribute("version", "4");

        Element moduleRootManager = new Element("component")
                .setAttribute("name", "NewModuleRootManager")
                .setAttribute("inherit-compiler-output", "false");

        moduleRootManager.addContent(new Element("exclude-output"));

        Path projectRoot = env.projectRoot();
        Path outDir = env.projectRoot().resolve("out").resolve(path.joinNames("_"));
        moduleRootManager.addContent(
                new Element(isForTests() ? "output-test" : "output")
                        .setAttribute("url", fileUrl(outDir))
        );

        List<ContentRoot> roots = buildContentRoots(getContentPaths());
        for (ContentRoot root : roots) {
            moduleRootManager.addContent(buildContentRootElement(root, projectRoot));
        }

        moduleRootManager.addContent(new Element("orderEntry")
                .setAttribute("type", "inheritedJdk")
        );

        moduleRootManager.addContent(new Element("orderEntry")
                .setAttribute("type", "sourceFolder")
                .setAttribute("forTests", "false")
        );

        for (DependencyEntry depEntry : getDependencyEntries()) {
            Element orderEntry = new Element("orderEntry");
            orderEntry.setAttribute("scope", depEntry.scope().name());
            if (depEntry.exported()) {
                orderEntry.setAttribute("exported", "");
            }
            if (depEntry instanceof MavenDependencyEntry ent) {
                orderEntry.setAttribute("type", "library");
                orderEntry.setAttribute("level", "project");
                var library = collector.lookup(ent.dep);
                if (library == null) {
                    throw new RuntimeException("Unable to find dependency " + ent.dep + " in the library collector.");
                }
                orderEntry.setAttribute("name", library.name());
            } else if (depEntry instanceof ProjectDependencyEntry ent) {
                orderEntry.setAttribute("type", "module");
                orderEntry.setAttribute("module-name", ent.module.path.toString());
            } else {
                throw new RuntimeException("Unhandled type " + depEntry.getClass());
            }
            moduleRootManager.addContent(orderEntry);
        }

        module.addContent(moduleRootManager);
        return new Document(module);
    }

    private static Element buildContentRootElement(ContentRoot root, Path projectRoot) {
        Element content = new Element("content");
        content.setAttribute("url", fileUrl(root.root));

        for (ContentPath path : root.contentRootPaths()) {
            Element element = new Element(path.type == PathType.EXCLUDE ? "excludeFolder" : "sourceFolder");
            element.setAttribute("url", fileUrl(path.path));
            switch (path.type) {
                case CODE -> element.setAttribute("isTestSource", "false");
                case TEST_CODE -> element.setAttribute("isTestSource", "true");
                case RESOURCES -> element.setAttribute("type", "java-resource");
                case TEST_RESOURCES -> element.setAttribute("type", "java-test-resource");
            }
            content.addContent(element);
        }
        return content;
    }

    private List<ContentRoot> buildContentRoots(List<ContentPath> paths) {
        // TODO this is an INCREDIBLY basic content root algorithm, that is sure to collapse under complicated projects..
        String commonParent = StringUtils.getCommonPrefix(FastStream.of(paths).map(e -> e.path.toString()).toArray(new String[0]));
        // We need to get the path for the group.
        if (commonParent.isEmpty()) {
            if (!(this instanceof IJModuleWithPath moduleWithPath)) throw new RuntimeException("Unable to build content root for an empty common parent.");

            return List.of(new ContentRoot(moduleWithPath.rootDir, List.of()));
        }
        if (!commonParent.endsWith("/")) {
            throw new RuntimeException("Unable to build content root. Got partial path for content paths " + paths);
        }
        return List.of(new ContentRoot(Path.of(commonParent), paths));
    }

    public record ContentRoot(Path root, List<ContentPath> contentRootPaths) { }

    public record ContentPath(Path path, PathType type) {

        public static ContentPath exclude(Path path) {
            return new ContentPath(path, PathType.EXCLUDE);
        }

        public static ContentPath code(Path path, boolean forTests) {
            return new ContentPath(path, forTests ? PathType.TEST_CODE : PathType.CODE);
        }

        public static ContentPath resources(Path path, boolean forTests) {
            return new ContentPath(path, forTests ? PathType.TEST_RESOURCES : PathType.RESOURCES);
        }
    }

    public enum PathType {
        EXCLUDE,
        CODE,
        RESOURCES,
        TEST_CODE,
        TEST_RESOURCES,
    }

    public enum DependencyScope {
        PROVIDED,
        COMPILE,
        RUNTIME,
        TEST
    }

    public sealed interface DependencyEntry permits MavenDependencyEntry, ProjectDependencyEntry {

        DependencyScope scope();

        boolean exported();

    }

    public record MavenDependencyEntry(
            Dependency.MavenDependency dep,
            DependencyScope scope,
            boolean exported
    ) implements DependencyEntry { }

    public record ProjectDependencyEntry(
            IJModule module,
            DependencyScope scope,
            boolean exported
    ) implements DependencyEntry { }
}

package net.covers1624.wstool.intellij.module;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.intellij.IJUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public final Document buildDocument(Environment env) {
        Element module = new Element("module")
                .setAttribute("type", "JAVA_MODULE")
                .setAttribute("version", "4");

        Element moduleRootManager = new Element("component")
                .setAttribute("name", "NewModuleRootManager")
                .setAttribute("inherit-compiler-output", "false");

        moduleRootManager.addContent(new Element("exclude-output"));

        Path outDir = env.projectRoot().resolve("out").resolve(path.joinNames("_"));
        moduleRootManager.addContent(
                new Element(isForTests() ? "output-test" : "output")
                        .setAttribute("url", IJUtils.fileUrl(outDir))
        );

        List<ContentRoot> roots = buildContentRoots(getContentPaths());
        for (ContentRoot root : roots) {
            moduleRootManager.addContent(buildContentRootElement(root));
        }

        moduleRootManager.addContent(new Element("orderEntry")
                .setAttribute("type", "inheritedJdk")
        );

        moduleRootManager.addContent(new Element("orderEntry")
                .setAttribute("type", "sourceFolder")
                .setAttribute("forTests", "false")
        );

        module.addContent(moduleRootManager);
        return new Document(module);
    }

    private static Element buildContentRootElement(ContentRoot root) {
        Element content = new Element("content");
        content.setAttribute("url", IJUtils.fileUrl(root.root));

        for (ContentPath path : root.contentRootPaths()) {
            Element element = new Element(path.type == PathType.EXCLUDE ? "excludeFolder" : "sourceFolder");
            element.setAttribute("url", IJUtils.fileUrl(path.path));
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

    private static boolean isAncestor(Path root, Path child) {
        while (child != null) {
            if (root.equals(child)) return true;

            child = child.getParent();
        }
        return false;
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
        TEST_RESOURCES;

    }
}

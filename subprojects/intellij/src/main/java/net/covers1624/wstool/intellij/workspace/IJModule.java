package net.covers1624.wstool.intellij.workspace;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.intellij.MavenDependencyCollector;
import org.jdom2.Document;
import org.jdom2.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.covers1624.wstool.intellij.IJUtils.fileUrl;

/**
 * Created by covers1624 on 5/4/25.
 */
public abstract sealed class IJModule permits IJSourceSetModule, IJModuleWithPath {

    protected final ModulePath path;

    private final List<Path> excludes = new ArrayList<>();

    public IJModule(ModulePath path) {
        this.path = path;
    }

    public String name() {
        return path.name();
    }

    public Path outputDir(Environment env) {
        return env.projectRoot().resolve("out").resolve(path.joinNames("_"));
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

    public final Document buildDocument(Environment env, MavenDependencyCollector collector, ContentRootCollector crCollector) {
        Element module = new Element("module")
                .setAttribute("type", "JAVA_MODULE")
                .setAttribute("version", "4");

        Element moduleRootManager = new Element("component")
                .setAttribute("name", "NewModuleRootManager")
                .setAttribute("inherit-compiler-output", "false");

        moduleRootManager.addContent(new Element("exclude-output"));

        moduleRootManager.addContent(
                new Element(isForTests() ? "output-test" : "output")
                        .setAttribute("url", fileUrl(outputDir(env)))
        );

        List<ContentRoot> roots = crCollector.buildRoots(this);
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

    private static Element buildContentRootElement(ContentRoot root) {
        Element content = new Element("content");
        content.setAttribute("url", fileUrl(root.root()));

        for (ContentPath path : root.contentRootPaths()) {
            Element element = new Element(path.type() == ContentPath.PathType.EXCLUDE ? "excludeFolder" : "sourceFolder");
            element.setAttribute("url", fileUrl(path.path()));
            switch (path.type()) {
                case CODE -> element.setAttribute("isTestSource", "false");
                case TEST_CODE -> element.setAttribute("isTestSource", "true");
                case RESOURCES -> element.setAttribute("type", "java-resource");
                case TEST_RESOURCES -> element.setAttribute("type", "java-test-resource");
            }
            content.addContent(element);
        }
        return content;
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

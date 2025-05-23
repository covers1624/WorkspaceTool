package net.covers1624.wstool.intellij.module;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.module.Module;
import net.covers1624.wstool.api.module.WorkspaceBuilder;
import net.covers1624.wstool.intellij.MavenDependencyCollector;
import net.covers1624.wstool.util.DeletingFileVisitor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.covers1624.wstool.intellij.IJUtils.*;

/**
 * Created by covers1624 on 5/3/25.
 */
public class IJWorkspaceBuilder implements WorkspaceBuilder {

    private final Environment env;
    private final ModulePath rootPath;
    private final Map<ModulePath, IJModule> modules = new LinkedHashMap<>();
    private final Map<String, IJProjectModule> projectModules = new LinkedHashMap<>();

    private int javaVersion = 8;

    public IJWorkspaceBuilder(Environment env) {
        this.env = env;

        rootPath = new ModulePath(List.of(env.projectRoot().getFileName().toString()));
        var rootModule = new RootModule(env.projectRoot(), rootPath);
        rootModule.excludes().add(env.projectRoot().resolve(".idea"));
        rootModule.excludes().add(env.projectRoot().resolve(".wstool"));
        rootModule.excludes().add(env.projectRoot().resolve("out"));
        modules.put(rootPath, rootModule);
    }

    @Override
    public Map<String, ? extends Module> modules() {
        return projectModules;
    }

    @Override
    public Module newModule(Path rootDir, String name) {
        // Figure out which folders exist between the dirs.
        List<String> relPathSegments = FastStream.of(env.projectRoot().relativize(rootDir.getParent()))
                .map(e -> e.getFileName().toString())
                .toList();
        ModulePath path = rootPath;
        // Modules in the root directory don't have any group segments.
        // TODO fix relPathSegments to just not return a single blank path.
        // TODO add additional assertions that no pieces of the path in a ModulePath are empty.
        if (!relPathSegments.equals(List.of(""))) {
            path = path.with(relPathSegments);
        }
        path = path.with(name);

        ensureParentGroupsExist(path);

        IJProjectModule projectModule = new IJProjectModule(this, rootDir, path);
        modules.put(path, projectModule);
        projectModules.put(name, projectModule);
        return projectModule;
    }

    @Override
    public void setJavaVersion(int version) {
        javaVersion = version;
    }

    @Override
    public void writeWorkspace() {
        Path ideaDir = env.projectRoot().resolve(".idea");
        Path modulesDir = ideaDir.resolve("modules");
        try {
            Files.createDirectories(modulesDir);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create directories.", ex);
        }

        MavenDependencyCollector collector = new MavenDependencyCollector();
        modules().values().forEach(collector::collectFrom);

        collector.hardlinkToCacheDir(env.projectCache());

        Path librariesDir = ideaDir.resolve("libraries");
        try {
            if (Files.exists(librariesDir)) {
                Files.walkFileTree(librariesDir, new DeletingFileVisitor(librariesDir));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to clean libraries dir.", ex);
        }
        for (MavenDependencyCollector.CollectedEntry entry : collector.collectedEntries()) {
            Path file = librariesDir.resolve(entry.name().replace(':', '.') + ".xml");
            writeDocument(entry.buildDocument(), file);
        }

        List<Path> moduleFiles = new ArrayList<>();
        for (IJModule module : modules.values()) {
            String name = module.path.joinNames(".");
            Path moduleFile = modulesDir.resolve(name + ".iml");
            writeDocument(module.buildDocument(env, collector), moduleFile);
            moduleFiles.add(moduleFile);
        }

        writeDocument(buildModulesXml(moduleFiles), ideaDir.resolve("modules.xml"));

        emitJavaVersionIntoMisc(ideaDir.resolve("misc.xml"));
    }

    private Document buildModulesXml(List<Path> moduleFiles) {
        Element project = new Element("project")
                .setAttribute("version", "4");

        Element component = new Element("component")
                .setAttribute("name", "ProjectModuleManager");

        Element modules = new Element("modules");
        for (Path file : moduleFiles) {
            modules.addContent(new Element("module")
                    .setAttribute("fileurl", fileUrl(file))
                    .setAttribute("filepath", file.toString())
            );
        }

        component.addContent(modules);
        project.addContent(component);
        return new Document(project);
    }

    private void emitJavaVersionIntoMisc(Path miscFile) {
        Document document;
        if (Files.exists(miscFile)) {
            try (InputStream is = Files.newInputStream(miscFile)) {
                document = new SAXBuilder().build(is);
            } catch (IOException | JDOMException ex) {
                throw new RuntimeException("Failed to parse existing misc.xml file.", ex);
            }
        } else {
            document = new Document(new Element("project").setAttribute("version", "4"));
        }
        Element project = document.getRootElement();

        Element projectRootManager = FastStream.of(project.getChildren("component"))
                .filter(e -> e.getAttributeValue("name", "").equals("ProjectRootManager"))
                .onlyOrDefault();

        if (projectRootManager == null) {
            projectRootManager = new Element("component")
                    .setAttribute("name", "ProjectRootManager")
                    .setAttribute("version", "2")
                    .setAttribute("project-jdk-type", "JavaSDK");
            project.addContent(projectRootManager);
        }

        String langVersion = "JDK_";
        if (javaVersion <= 8) {
            langVersion += "1_" + javaVersion;
        } else {
            langVersion += String.valueOf(javaVersion);
        }

        projectRootManager.setAttribute("languageLevel", langVersion);

        // TODO see if we can emit sdk's as project libraries yet, let the user select to allocate/name one in the config?
        writeDocument(document, miscFile);
    }

    private static void writeDocument(Document doc, Path file) {
        XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());
        try (OutputStream os = Files.newOutputStream(IOUtils.makeParents(file))) {
            output.output(doc, os);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write xml file to " + file, ex);
        }
    }

    void addModule(IJModule module) {
        if (modules.containsKey(module.path)) throw new RuntimeException("Module with name " + module.path + " already exists.");

        modules.put(module.path, module);
    }

    private void ensureParentGroupsExist(ModulePath path) {
        path = path.parent();
        while (path != null) {
            IJModule module = modules.get(path);
            if (module == null) {
                modules.put(path, new GroupModule(env.projectRoot().resolve(path.tail().joinNames("/")), path));
            } else if (!(module instanceof RootModule)) {
                throw new RuntimeException("Somehow tried to make a group through a project? Sub-Projects should be made directly from their parent?");
            }

            path = path.parent();
        }
    }

    public static class RootModule extends IJModuleWithPath {

        public RootModule(Path rootDir, ModulePath path) {
            super(rootDir, path);
        }
    }

    // TODO we may be able to flatten this. Depends if the root module requires any direct and specific configuration.
    public static class GroupModule extends RootModule {

        public GroupModule(Path rootDir, ModulePath path) {
            super(rootDir, path);
        }
    }
}

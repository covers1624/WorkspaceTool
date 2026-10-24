package net.covers1624.wstool.intellij.workspace;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.workspace.*;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.runs.RunConfig;
import net.covers1624.wstool.intellij.IntellijWorkspaceType;
import net.covers1624.wstool.intellij.MavenDependencyCollector;
import net.covers1624.wstool.util.DeletingFileVisitor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static net.covers1624.wstool.intellij.IJUtils.fileUrl;

/**
 * Created by covers1624 on 5/3/25.
 */
public class IJWorkspace implements Workspace {

    private final Environment env;
    private final IntellijWorkspaceType.LinkingMode linkingMode;
    @Nullable private final Integer compilerHeapMemory;

    private final ModulePath rootPath;
    private final Map<ModulePath, IJModule> modules = new LinkedHashMap<>();
    private final Map<String, IJProjectModule> projectModules = new LinkedHashMap<>();

    private final Map<String, IJRunConfig> runConfigs = new LinkedHashMap<>();

    private int javaVersion = 8;

    public IJWorkspace(Environment env, IntellijWorkspaceType.LinkingMode linkingMode, @Nullable Integer compilerHeapMemory) {
        this.env = env;
        this.linkingMode = linkingMode;
        this.compilerHeapMemory = compilerHeapMemory;

        rootPath = new ModulePath(List.of(env.projectRoot().getFileName().toString()));
        var rootModule = new RootModule(env.projectRoot(), rootPath);
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
    public Map<String, ? extends RunConfig> runConfigs() {
        return runConfigs;
    }

    @Override
    public RunConfig newRunConfig(String name) {
        if (runConfigs.containsKey(name)) throw new RuntimeException("A run config with the name " + name + " already exists.");

        IJRunConfig runConfig = new IJRunConfig(name);
        runConfigs.put(name, runConfig);
        return runConfig;
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

        var depResolver = resolveDependencies(ideaDir);

        ContentRootCollector crCollector = new ContentRootCollector();
        for (IJModule module : modules.values()) {
            crCollector.processModule(module);
        }

        List<Path> moduleFiles = new ArrayList<>();
        for (IJModule module : modules.values()) {
            String name = module.path.joinNames(".");
            Path moduleFile = modulesDir.resolve(name + ".iml");
            writeDocument(module.buildDocument(env, depResolver, crCollector), moduleFile);
            moduleFiles.add(moduleFile);
        }

        writeDocument(buildModulesXml(moduleFiles), ideaDir.resolve("modules.xml"));

        emitJavaVersionIntoMisc(ideaDir.resolve("misc.xml"));
        insertIntoCompiler(ideaDir.resolve("compiler.xml"), depResolver);

        Path runsDir = ideaDir.resolve("runConfigurations");
        for (IJRunConfig runConfig : runConfigs.values()) {
            Path dest = runsDir.resolve(runConfig.name().replace("/", "_") + ".xml");
            writeDocument(runConfig.writeDocument(env, depResolver), dest);
            runConfig.createWorkingDir();
        }
    }

    private DependencyResolver resolveDependencies(Path ideaDir) {
        MavenDependencyCollector depCollector = new MavenDependencyCollector();
        modules().values().forEach(depCollector::collectFrom);

        runConfigs().values().forEach(depCollector::collectFrom);

        depCollector.linkLibrariesToCache(linkingMode, env.projectCache());

        Path librariesDir = ideaDir.resolve("libraries");
        try {
            if (Files.exists(librariesDir)) {
                Files.walkFileTree(librariesDir, new DeletingFileVisitor(librariesDir));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to clean libraries dir.", ex);
        }
        for (MavenDependencyCollector.CollectedEntry entry : depCollector.collectedEntries()) {
            Path file = librariesDir.resolve(entry.name().replace(':', '.') + ".xml");
            writeDocument(entry.buildDocument(), file);
        }
        return resolver(env, depCollector);
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
        var document = loadOrCreateDocument(miscFile, () ->
                new Document(new Element("project").setAttribute("version", "4"))
        );
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

    private void insertIntoCompiler(Path compilerFile, DependencyResolver depResolver) {
        var document = loadOrCreateDocument(compilerFile, () ->
                new Document(new Element("project").setAttribute("version", "4"))
        );
        Element project = document.getRootElement();

        var compilerConfiguration = FastStream.of(project.getChildren("component"))
                .filter(e -> e.getAttributeValue("name", "").equals("CompilerConfiguration"))
                .onlyOrDefault();
        if (compilerConfiguration == null) {
            compilerConfiguration = new Element("component")
                    .setAttribute("name", "CompilerConfiguration");
            project.addContent(compilerConfiguration);
        }

        if (compilerHeapMemory != null) {
            var heapSizeOpt = FastStream.of(project.getChildren("option"))
                    .filter(e -> e.getAttributeValue("name", "").equals("BUILD_PROCESS_HEAP_SIZE"))
                    .firstOrDefault();
            if (heapSizeOpt == null) {
                heapSizeOpt = new Element("option")
                        .setAttribute("name", "BUILD_PROCESS_HEAP_SIZE");
                compilerConfiguration.addContent(heapSizeOpt);
            }
            heapSizeOpt.setAttribute("value", compilerHeapMemory.toString());
        }

        var modulesWithAP = FastStream.of(modules.values())
                .mapNonNull(e -> e instanceof IJSourceSetModule m ? m : null)
                .filterNot(e -> e.annotationProcessorDependencies().isEmpty())
                .toList();

        if (!modulesWithAP.isEmpty()) {
            var ap = compilerConfiguration.getChild("annotationProcessing");
            if (ap == null) {
                ap = new Element("annotationProcessing");
                compilerConfiguration.addContent(ap);
            }
            FastStream.of(ap.getChildren("profile"))
                    .filter(e -> e.getAttributeValue("name", "").equals("WorkspaceTool Imported"))
                    .toList()
                    .forEach(ap::removeContent);

            for (IJSourceSetModule module : modulesWithAP) {
                var profile = new Element("profile")
                        .setAttribute("name", "WorkspaceTool Imported")
                        .setAttribute("enabled", "true");

                profile.addContent(new Element("outputRelativeToContentRoot").setAttribute("value", "true"));
                profile.addContent(new Element("module").setAttribute("name", module.path.toString()));
                var processorPath = new Element("processorPath")
                        .setAttribute("useClasspath", "false");
                for (var dep : module.annotationProcessorDependencies()) {
                    var entry = new Element("entry")
                            .setAttribute("name", requireNonNull(depResolver.resolvePath(dep)).toAbsolutePath().toString());
                    processorPath.addContent(entry);
                }
                profile.addContent(processorPath);

                ap.addContent(profile);
            }
        }

        writeDocument(document, compilerFile);
    }

    private static Document loadOrCreateDocument(Path file, Supplier<Document> factory) {
        if (Files.notExists(file)) return factory.get();

        try (InputStream is = Files.newInputStream(file)) {
            return new SAXBuilder().build(is);
        } catch (IOException | JDOMException ex) {
            throw new RuntimeException("Failed to parse document: " + file, ex);
        }
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

    private static DependencyResolver resolver(Environment env, MavenDependencyCollector collector) {
        return new DependencyResolver() {
            @Override
            public @Nullable String lookupName(Dependency.MavenDependency dep) {
                var ent = collector.lookup(dep);
                if (ent == null) return null;

                return ent.name();
            }

            @Override
            public @Nullable Path resolvePath(Dependency dep) {
                return switch (dep) {
                    case Dependency.MavenDependency mavenDep -> {
                        var found = collector.lookup(mavenDep);
                        if (found == null) {
                            throw new RuntimeException("Unable to find dependency " + mavenDep + " in the library collector.");
                        }
                        yield found.classes();
                    }
                    case Dependency.SourceSetDependency(SourceSet sourceSet, boolean exported) -> ((IJSourceSetModule) sourceSet).outputDir(env);
                    case Dependency.FileDependency(Path file, boolean exported) -> file;
                };
            }
        };
    }

    public static sealed class RootModule extends IJModuleWithPath permits IJWorkspace.GroupModule {

        public RootModule(Path rootDir, ModulePath path) {
            super(rootDir, path);
        }
    }

    // TODO we may be able to flatten this. Depends if the root module requires any direct and specific configuration.
    public final static class GroupModule extends RootModule {

        public GroupModule(Path rootDir, ModulePath path) {
            super(rootDir, path);
        }
    }
}

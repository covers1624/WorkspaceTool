package net.covers1624.wt.intellij

import groovy.xml.MarkupBuilder
import net.covers1624.wt.api.dependency.DependencyLibrary
import net.covers1624.wt.api.dependency.LibraryDependency
import net.covers1624.wt.api.dependency.MavenDependency
import net.covers1624.wt.api.dependency.SourceSetDependency
import net.covers1624.wt.api.impl.module.ConfigurationImpl
import net.covers1624.wt.api.module.Configuration
import net.covers1624.wt.api.module.Module
import net.covers1624.wt.api.module.ModuleList
import net.covers1624.wt.api.script.runconfig.RunConfig
import net.covers1624.wt.api.workspace.WorkspaceWriter
import net.covers1624.wt.event.RunConfigModuleEvent
import net.covers1624.wt.intellij.api.script.Intellij
import net.covers1624.wt.util.Utils
import net.covers1624.wt.util.scala.ScalaSdk
import org.apache.commons.lang3.StringUtils

import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by covers1624 on 20/7/19.
 */
class IntellijFolderWorkspaceWriter implements WorkspaceWriter<Intellij> {

    private final ScalaSdk scalaSdk
    private final DependencyLibrary dependencyLibrary
    private final Path projectDir

    IntellijFolderWorkspaceWriter(Path projectDir, DependencyLibrary dependencyLibrary, ScalaSdk scalaSdk) {
        this.projectDir = projectDir
        this.dependencyLibrary = dependencyLibrary
        this.scalaSdk = scalaSdk
    }


    @Override
    void write(Intellij frameworkImpl, ModuleList moduleList, Map<String, RunConfig> runConfigs) {
        Path dotIdea = projectDir.resolve(".idea")
        //Extract some stuff.
        Utils.maybeExtractResource("/templates/idea/encodings.xml", dotIdea.resolve("encodings.xml"))
        Utils.maybeExtractResource("/templates/idea/inspections.xml", dotIdea.resolve("inspections.xml"))

        //Write misc.xml
        block {
            Path misc = dotIdea.resolve("misc.xml")
            def node = misc.exists ? misc.parseXml() : new Node(null, 'project', ['version': '4'])
            def prm = node.children().find { it instanceof Node && it.get("@name") == "ProjectRootManager" }
            if (prm != null) {
                node.remove(prm)
            }
            def xml = markupXml {
                component(name: 'ProjectRootManager', version: '2', languageLevel: 'JDK_1_8', 'project-jdk-name': '1.8', 'project-jdk-type': 'JavaSDK') {
                    output(url: "file://${projectDir.resolve("out").normalize().toString()}")
                }
            }
            node.append(xml.parseXml())
            misc.write(node)
        }
        //Write libraries.
        def libraries = dotIdea.resolve("libraries")
        dependencyLibrary.dependencies.values().each { lib ->
            def dep = lib.mavenDependency
            def xml = markupXml {
                component(name: 'libraryTable') {
                    library(name: dep.notation.toString()) {
                        CLASSES() { if (dep.classes != null) root(url: toIdeaURL(dep.classes)) }
                        JAVADOC() { if (dep.javadoc != null) root(url: toIdeaURL(dep.javadoc)) }
                        SOURCES() { if (dep.sources != null) root(url: toIdeaURL(dep.sources)) }
                    }
                }
            }
            libraries.resolve("${lib.libraryFileName}.xml").write(xml)
        }
        //Write scalaSdk.
        block {
            def xml = markupXml {
                component(name: "libraryTable") {
                    library(name: scalaSdk.sdkName, type: "Scala") {
                        'properties' {
                            'language-level'(scalaSdk.scalaVersion.name())
                            'compiler-classpath' {
                                scalaSdk.classpath.each {
                                    if (it.classes != null) {
                                        root(url: "file://${it.classes.absolutePath}")
                                    }
                                }
                            }
                        }
                        CLASSES {
                            scalaSdk.libraries.each {
                                if (it.classes != null) root(url: toIdeaURL(it.classes))
                            }
                        }
                        JAVADOC {
                            scalaSdk.libraries.each {
                                if (it.javadoc != null) root(url: toIdeaURL(it.javadoc))
                            }
                        }
                        SOURCES {
                            scalaSdk.libraries.each {
                                if (it.sources != null) root(url: toIdeaURL(it.sources))
                            }
                        }
                    }
                }
            }
            libraries.resolve("${scalaSdk.sdkName.replaceAll("[.-]", "_")}.xml").write(xml)
        }
        //WriteModules
        block {
            def modulesDir = dotIdea.resolve("modules")
            def generatedModules = [] as List<GeneratedModule>
            moduleList.allModules.each {
                generatedModules += writeModule(modulesDir, it)
            }
            def modulesXml = markupXml {
                project(version: '4') {
                    component(name: 'ProjectModuleManager') {
                        modules {
                            generatedModules.each {
                                def attribs = [fileurl: "file://${it.moduleFile.absolutePath}", filepath: it.moduleFile.absolutePath]
                                if (!StringUtils.isEmpty(it.group)) {
                                    attribs += [group: it.group]
                                }
                                module(attribs)
                            }
                        }
                    }
                }
            }
            dotIdea.resolve("modules.xml").write(modulesXml)
        }

        //Write RunConfigs
        block {
            def classpathModule = RunConfigModuleEvent.REGISTRY.fireEvent(new RunConfigModuleEvent(moduleList)).getResult()
            if (classpathModule == null) {
                throw new RuntimeException("No Classpath module selected by framework.")
            }
            def runConfigsPath = dotIdea.resolve("runConfigurations")
            runConfigs.each {
                def name = it.key
                def config = it.value
                def escape = { it.contains(" ") ? "\"$it\"" : it }
                def vmArgs = []
                vmArgs += config.vmArgs
                vmArgs += config.sysProps.collect { "-D${it.key}=${it.value}" }
                if (!config.runDir.exists) {
                    Files.createDirectories(config.runDir)
                }
                def xml = markupXml {
                    component(name: 'ProjectRunConfigurationManager') {
                        configuration(name: name, type: 'Application', facroryName: 'Application') {
                            option(name: 'MAIN_CLASS_NAME', value: config.mainClass)
                            module(name: classpathModule.name)
                            option(name: 'PROGRAM_PARAMETERS', value: config.progArgs.collect(escape).join(" "))
                            option(name: 'VM_PARAMETERS', value: vmArgs.collect(escape).join(" "))
                            option(name: 'WORKING_DIRECTORY', value: config.runDir.absolutePath)
                            if (!config.envVars.isEmpty()) {
                                envs {
                                    config.envVars.each {
                                        env(name: it.key, value: it.value)
                                    }
                                }
                            }
                        }
                    }
                }
                runConfigsPath.resolve(name + ".xml").write(xml)
            }
        }
    }

    def writeModule(Path modulesDir, Module moduleToWrite) {
        return writeMegaModule(modulesDir, moduleToWrite)
    }

    def writeMegaModule(Path modulesDir, Module moduleToWrite) {
        def genModule = new GeneratedModule()
        genModule.name = moduleToWrite.name
        genModule.group = moduleToWrite.group
        genModule.moduleFile = modulesDir.resolve(genModule.name + ".iml")

        List<Path> allSource = []
        List<Path> resources = []
        Configuration compile = new ConfigurationImpl("ideaWriteCompile", true)
        Configuration runtime = new ConfigurationImpl("ideaWriteRuntime", true)
        Configuration compileOnly = new ConfigurationImpl("ideaWriteCompileOnly", true)
        moduleToWrite.sourceSets.values().each {
            allSource += it.allSource
            resources += it.resources
            if (it.compileConfiguration != null) compile.addExtendsFrom(it.compileConfiguration)
            if (it.runtimeConfiguration != null) runtime.addExtendsFrom(it.runtimeConfiguration)
            if (it.compileOnlyConfiguration != null) compileOnly.addExtendsFrom(it.compileOnlyConfiguration)
        }

        def genXml = markupModule(
                moduleToWrite.path,
                allSource,
                resources,
                "main",
                compile,
                runtime,
                compileOnly
        )
        genModule.moduleFile.write(genXml)

        return [genModule]
    }

//    def writeModuleSeparateSourceSets(Path modulesDir, Module moduleToWrite) {
//        def generatedModules = []
//        def master = new GeneratedModule()
//        master.name = getModuleName(moduleToWrite)
//        master.group = getModuleName(moduleToWrite)
//        master.moduleFile = modulesDir.resolve(master.name + ".iml")
//        generatedModules << master
//        def masterXml = markupXml {
//            module(type: 'JAVA_MODULE', version: '4') {
//                component(name: 'NewModuleRootManager', 'inherit-compiler-output': 'true') {
//                    'exclude-output'()
//                    content(url: "file://${moduleToWrite.path.absolutePath}") {
//                        excludeFolder(url: "file://${moduleToWrite.path.resolve(".gradle").absolutePath}")
//                        excludeFolder(url: "file://${moduleToWrite.path.resolve("build").absolutePath}")
//                    }
//                }
//            }
//        }
//        master.moduleFile.write(masterXml)
//
//        moduleToWrite.sourceSets.values().each { ss ->
//            def genModule = new GeneratedModule()
//            genModule.name = master.name + "_" + ss.name
//            genModule.moduleFile = modulesDir.resolve(genModule.name + ".iml")
//            genModule.group = master.group
//            generatedModules << genModule
//            genModule.moduleFile.write(markupModule(//
//                    moduleToWrite.path,//
//                    ss.allSource.toList(),//
//                    ss.resources,//
//                    ss.name,//
//                    ss.compileConfiguration,//
//                    ss.runtimeConfiguration,//
//                    ss.compileOnlyConfiguration//
//            ))
//        }
//        generatedModules
//    }

    def markupModule(Path modulePath, List<Path> allSource, List<Path> resources, String ssName, Configuration compile, Configuration runtime, Configuration compileOnly) {
        markupXml {
            module(type: 'JAVA_MODULE', version: '4') {
                component(name: 'NewModuleRootManager', 'inherit-compiler-output': 'true') {
                    'exclude-output'()
                    //TODO, This module root might not be correct in all cases.
                    // This should be changed to find the common root directory of all sources and resources in the SS.
                    def all = []
                    all += allSource
                    all += resources
                    //Utils.commonRootPath(all, modulePath.resolve("src").resolve(ssName))
                    content(url: "file://${modulePath.absolutePath}") {
                        allSource.each { sourceFolder(url: "file://${it.absolutePath}", isTestSource: ssName == "test") }
                        resources.each { sourceFolder(url: "file://${it.absolutePath}", type: 'java-resource') }
                    }
                    orderEntry(type: 'inheritedJdk')
                    orderEntry(type: 'sourceFolder', forTests: 'false')
                    def configs = [
                            COMPILE : compile,
                            RUNTIME : runtime,
                            PROVIDED: compileOnly
                    ]
                    configs.each {
                        def scope = it.key
                        def config = it.value
                        if (config != null) {
                            config.allDependencies.each {
                                def attribs = [scope: scope]
                                if (it.export) {
                                    attribs += [exported: '']
                                }
                                if (it instanceof MavenDependency) {
                                    attribs += [type: 'module-library']
                                    orderEntry(attribs) {
                                        library() {
                                            CLASSES() { if (it.classes != null) root(url: toIdeaURL(it.classes)) }
                                            JAVADOC() { if (it.javadoc != null) root(url: toIdeaURL(it.javadoc)) }
                                            SOURCES() { if (it.sources != null) root(url: toIdeaURL(it.sources)) }
                                        }
                                    }
                                } else if (it instanceof SourceSetDependency) {
                                    attribs += [type: 'module', 'module-name': it.module.name]
                                    orderEntry(attribs)
                                } else if (it instanceof LibraryDependency) {
                                    attribs += [type: 'library', name: it.libraryName, level: 'project']
                                    orderEntry(attribs)
                                } else {
                                    throw new RuntimeException("Unhandled Type: " + dep)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private class GeneratedModule {
        Path moduleFile
        String name
        String group
    }

//    private static String getModuleName(Module module) {
//        if (StringUtils.isEmpty(module.group)) {
//            return module.name
//        }
//        return module.group + "_" + module.name
//    }
//
//    private static String getModuleSSDep(Module module, String ssName) {
//        if (module.modulePerSourceSet) {
//            return getModuleName(module) + "_" + ssName
//        }
//        return getModuleName(module)
//    }


    //Just allows me to separate scopes.
    private static void block(Closure closure) {
        closure.call()
    }

    //See org.gradle.plugins.ide.idea.model.PathFactory#path(File)
    def toIdeaURL(Path file) {
        def path = file.absolutePath
        if (path.endsWith(".jar")) {
            return "jar://${path}!/"
        }
        return "file://${path}"
    }

    private static String markupXml(Closure closure) {
        def xml = new StringWriter()
        def b = new MarkupBuilder(xml)
        Closure copy = closure.clone()
        copy.setResolveStrategy(Closure.DELEGATE_FIRST)
        copy.setDelegate(b)
        if (copy.getMaximumNumberOfParameters() == 0) {
            copy.call()
        } else {
            copy.call(b)
        }
        return xml.toString()
    }
}

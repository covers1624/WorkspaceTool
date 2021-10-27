package net.covers1624.wt.intellij

import net.covers1624.wt.api.WorkspaceToolContext
import net.covers1624.wt.api.dependency.DependencyScope
import net.covers1624.wt.api.dependency.SourceSetDependency
import net.covers1624.wt.api.impl.dependency.WorkspaceModuleDependencyImpl
import net.covers1624.wt.api.module.Configuration
import net.covers1624.wt.api.workspace.WorkspaceHandler
import net.covers1624.wt.intellij.api.impl.IJWorkspaceModuleImpl
import net.covers1624.wt.intellij.api.script.Intellij

/**
 * Created by covers1624 on 7/9/19.
 */
class IntellijWorkspaceHandler implements WorkspaceHandler<Intellij> {

    @Override
    void buildWorkspaceModules(Intellij workspace, WorkspaceToolContext context) {
        def modules = [:] as Map<String, IJWorkspaceModuleImpl>
        def outFolder = context.projectDir.resolve("out")
        context.allModules.each { module ->
            def wModule = new IJWorkspaceModuleImpl()
            wModule.path = module.path
            wModule.isGroup = true
            wModule.name = module.name.replace("/", ".")
            wModule.output = outFolder.resolve(wModule.name.replace(".", "_"))
            wModule.excludes = module.excludes
            modules[wModule.name] = wModule


            def groupSplit = module.name.split("/")
            def gName = ""
            if (groupSplit.length > 1) {
                def lastGroup = null
                for (int i = 0; i <= groupSplit.length - 2; i++) {
                    def name = groupSplit[i]
                    gName = gName.empty ? name : "$gName/$name"
                    def newGroup = modules[gName.replace("/", ".")]
                    if (newGroup == null) {
                        newGroup = new IJWorkspaceModuleImpl()
                        newGroup.isGroup = true
                        newGroup.name = gName.replace("/", ".")
                        newGroup.path = context.projectDir.resolve(gName)
                        newGroup.output = outFolder.resolve(newGroup.name.replace(".", "_"))
                        modules[gName.replace("/", ".")] = newGroup
                        if (lastGroup != null) {
                            newGroup.parent = lastGroup
                            lastGroup.children << newGroup
                        }
                    }
                    lastGroup = newGroup
                }
                wModule.parent = lastGroup
                lastGroup.children << wModule
            }

            module.sourceSets.values().each { ss ->
                def ssModule = new IJWorkspaceModuleImpl()
                ssModule.name = wModule.name + "." + ss.name
                ssModule.parent = wModule
                wModule.children << ssModule
                modules[ssModule.name] = ssModule
                ssModule.resources.addAll(ss.resources)
                ssModule.sourceMap.putAll(ss.sourceMap)
                ssModule.sourceSetName = ss.name
                ssModule.output = outFolder.resolve(ssModule.name.replace(".", "_"))
            }
        }
        context.allModules.each { module ->
            def wModule = modules[module.name]
            module.sourceSets.values().each { ss ->
                def ssModule = modules[module.name.replace("/", ".") + "." + ss.name]
                def configs = [:] as Map<DependencyScope, Configuration>
                configs[DependencyScope.COMPILE] = ss.compileConfiguration
                configs[DependencyScope.RUNTIME] = ss.runtimeConfiguration
                configs[DependencyScope.PROVIDED] = ss.compileOnlyConfiguration
                configs.each {
                    def scope = it.key
                    def config = it.value
                    def dependencies = ssModule.dependencies.computeIfAbsent(scope, { new LinkedHashSet<>() })
                    if (config != null) {
                        config.allDependencies.each {
                            if (it instanceof SourceSetDependency) {
                                def depMod = modules[it.module.name.replace("/", ".") + "." + it.sourceSet]
                                if (depMod != null) {
                                    dependencies << new WorkspaceModuleDependencyImpl().setModule(depMod).setExport(it.export)
                                }
                            } else {
                                dependencies << it
                            }
                        }
                    }
                }
            }
        }
        context.workspaceModules.addAll(modules.values())
    }
}

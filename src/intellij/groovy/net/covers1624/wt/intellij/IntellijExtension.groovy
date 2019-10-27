package net.covers1624.wt.intellij

import net.covers1624.wt.api.Extension
import net.covers1624.wt.api.ExtensionDetails
import net.covers1624.wt.api.script.runconfig.RunConfig
import net.covers1624.wt.event.*
import net.covers1624.wt.intellij.api.impl.IntellijImpl
import net.covers1624.wt.intellij.api.impl.IntellijRunConfigTemplate
import net.covers1624.wt.intellij.api.script.Intellij
import net.covers1624.wt.intellij.api.script.IntellijRunConfig
import net.covers1624.wt.intellij.writer.FolderWorkspaceWriter
import org.codehaus.groovy.control.customizers.ImportCustomizer

import java.nio.file.Files

/**
 * Created by covers1624 on 23/7/19.
 */
@ExtensionDetails(name = "Intellij", desc = "Provides integration for outputting Intellij Workspaces.")
class IntellijExtension implements Extension {

    @Override
    void load() {
        InitializationEvent.REGISTRY.register {
            def workspaceRegistry = it.context.workspaceRegistry
            workspaceRegistry.registerScriptImpl(Intellij) {
                new IntellijImpl(it)
            }
            workspaceRegistry.registerWorkspaceHandler(Intellij) {
                new IntellijWorkspaceHandler()
            }
            workspaceRegistry.registerWorkspaceWriter(Intellij) { context ->
                new FolderWorkspaceWriter(context)
            }
        }
        PrepareScriptEvent.REGISTRY.register {
            def importCustomizer = new ImportCustomizer()
            importCustomizer.addStarImports("net.covers1624.wt.intellij.api.script")
            it.compilerConfiguration.addCompilationCustomizers(importCustomizer)
        }
        ScriptWorkspaceEvalEvent.REGISTRY.register {
            if (it.script.workspaceType == Intellij) {
                it.mixinInstantiator.addMixinClass(RunConfig, IntellijRunConfig, IntellijRunConfigTemplate)
            }
        }
        ProcessModulesEvent.REGISTRY.register(Event.Priority.FIRST) {
            if (it.context.workspaceScript.workspaceType == Intellij) {
                def root = it.context.projectDir
                def outDir = root.resolve("out/production")
                it.context.allModules.each {
                    def mOut = outDir.resolve(it.name)
                    if (!mOut.exists) {
                        Files.createDirectories(mOut)
                    }
                    it.compileOutput = mOut
                }
            }
        }
    }
}

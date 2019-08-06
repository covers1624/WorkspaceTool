package net.covers1624.wt.intellij

import net.covers1624.wt.api.Extension
import net.covers1624.wt.api.ExtensionDetails
import net.covers1624.wt.event.InitializationEvent
import net.covers1624.wt.event.PrepareScriptEvent
import net.covers1624.wt.intellij.api.impl.IntellijImpl
import net.covers1624.wt.intellij.api.script.Intellij
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * Created by covers1624 on 23/7/19.
 */
@ExtensionDetails(name = "Intellij", desc = "Provides integration for outputting Intellij Workspaces.")
class IntellijExtension implements Extension {

    @Override
    void load() {
        InitializationEvent.REGISTRY.register {
            def workspaceRegistry = it.workspaceRegistry
            workspaceRegistry.registerScriptImpl(Intellij) {
                new IntellijImpl()
            }
            workspaceRegistry.registerWorkspaceWriter(Intellij) { path, library, scalaSdk ->
                new IntellijFolderWorkspaceWriter(path, library, scalaSdk)
            }
        }
        PrepareScriptEvent.REGISTRY.register {
            def importCustomizer = new ImportCustomizer()
            importCustomizer.addStarImports("net.covers1624.wt.intellij.api.script")
            it.compilerConfiguration.addCompilationCustomizers(importCustomizer)
        }
    }
}

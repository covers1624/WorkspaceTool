/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.intellij.model

import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by covers1624 on 26/10/19.
 */
class IJRunConfig {
    String name
    String mainClass
    String classpathModule
    List<String> progArgs = []
    List<String> vmArgs = []
    Map<String, String> sysProps = [:]
    Map<String, String> envVars = [:]
    Path runDir

    def write(Path runConfigFolder) {
        def escape = { it.contains(" ") ? "\"$it\"" : it }

        def vmArgs = []
        vmArgs += this.vmArgs
        vmArgs += sysProps.collect { "-D${it.key}=${it.value}" }

        def component = new Node(null, 'component', [name: 'ProjectRunConfigurationManager'])
        def configuration = component.appendNode('configuration', [name: name, type: 'Application', factoryName: 'Application'])
        configuration.appendNode('option', [name: 'MAIN_CLASS_NAME', value: mainClass])
        configuration.appendNode('module', [name: classpathModule])
        configuration.appendNode('option', [name: 'PROGRAM_PARAMETERS', value: progArgs.collect(escape).join(" ")])
        configuration.appendNode('option', [name: 'VM_PARAMETERS', value: vmArgs.collect(escape).join(" ")])
        configuration.appendNode('option', [name: 'WORKING_DIRECTORY', value: runDir.absolutePath])
        if (!envVars.isEmpty()) {
            def envs = configuration.appendNode('envs')
            envVars.each {
                envs.appendNode('env', [name: it.key, value: it.value])
            }
        }
        def methodv2 = configuration.appendNode('method', [v: '2'])
        methodv2.appendNode('option', [name: 'MakeProject', enabled: 'true'])
        def path = runConfigFolder.resolve(name + ".xml")
        Files.deleteIfExists(path)
        path.write(component)
    }

}

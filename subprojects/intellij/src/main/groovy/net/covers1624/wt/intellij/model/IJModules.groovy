/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.intellij.model

import org.apache.commons.lang3.StringUtils

import java.nio.file.Path

/**
 * Created by covers1624 on 14/8/19.
 */
class IJModules {

    Map<Path, String> modules = [:]

    def write(Path dotIdea) {
        def node = new Node(null, 'project', [version: '4'])
        def componentNode = node.appendNode("component", [name: 'ProjectModuleManager'])
        def modulesNode = componentNode.appendNode('modules')
        modules.each {
            def attribs = [fileurl: it.key.fileURL, filepath: it.key.absolutePath]
            if (StringUtils.isNotEmpty(it.value)) {
                attribs += [group: it.value]
            }
            modulesNode.appendNode("module", attribs)
        }
        dotIdea.resolve("modules.xml").write(node)
    }
}

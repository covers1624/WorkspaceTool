package net.covers1624.wt.intellij.model

import org.apache.commons.lang3.StringUtils

import java.nio.file.Path

/**
 * Created by covers1624 on 14/8/19.
 */
class IJModule {

    String name
    String group

    Path output
    List<IJModuleContent> content = []
    List<IJOrderEntry> entries = []

    boolean isTest
    String productionModule

    def write(Path file) {
        def moduleNode = new Node(null, 'module', [type: 'JAVA_MODULE', version: '4'])
        def componentNode = moduleNode.appendNode('component', [name: 'NewModuleRootManager', 'inherit-compiler-output': 'false'])
        componentNode.appendNode('exclude-output')
        componentNode.appendNode(isTest ? 'output-test' : 'output', [url: output.fileURL])
        content.each { it.write(componentNode) }
        entries.each { it.write(componentNode) }
        if (isTest && StringUtils.isNotEmpty(productionModule)) {
            moduleNode.appendNode('component', [name: 'TestModuleProperties', 'production-module': productionModule])
        }
        file.write(moduleNode)
    }
}

class IJModuleContent {
    Path contentRoot
    //Source to Attribute map.
    Map<Path, Map> sources = [:]

    def write(Node rootNode) {
        def content = rootNode.appendNode("content", [url: contentRoot.fileURL])
        sources.each {
            def attribs = [url: it.key.fileURL]
            attribs += it.value
            content.appendNode("sourceFolder", attribs)
        }
    }
}

class IJOrderEntry {
    Map attributes = [:]

    def write(Node root) {
        root.appendNode("orderEntry", attributes)
    }
}

class IJLibraryOrderEntry extends IJOrderEntry {

    IJLibrary library

    @Override
    def write(Node root) {
        Node node = super.write(root)
        library.write(node)
    }
}

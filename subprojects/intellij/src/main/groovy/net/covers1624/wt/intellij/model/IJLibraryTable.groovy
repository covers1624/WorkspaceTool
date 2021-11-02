package net.covers1624.wt.intellij.model


import java.nio.file.Path

/**
 * Created by covers1624 on 14/8/19.
 */
class IJLibraryTable {

    Map<String, IJLibrary> libraries = [:]

    def write(Path libraryFolder) {
        libraries.each {
            def node = new Node(null, 'component', [name: 'libraryTable'])
            it.value.write(node)
            libraryFolder.resolve("${it.key}.xml").write(node)
        }
    }
}

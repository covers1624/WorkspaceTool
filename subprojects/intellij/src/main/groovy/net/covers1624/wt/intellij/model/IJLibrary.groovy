/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.intellij.model

import java.nio.file.Path

/**
 * Created by covers1624 on 14/8/19.
 */
class IJLibrary {

    String libraryName
    Map attributes = [:]

    def write(Node root) {
        def attribs = [:]
        if (libraryName != null) {
            attribs += [name: libraryName]
        }
        root.appendNode("library", attribs + attributes)
    }
}

class IJMavenLibrary extends IJLibrary {

    List<Path> classes = []
    List<Path> javadoc = []
    List<Path> sources = []

    @Override
    def write(Node root) {
        def node = super.write(root)
        append(node.appendNode("CLASSES"), classes)
        append(node.appendNode("JAVADOC"), javadoc)
        append(node.appendNode("SOURCES"), sources)
        return node
    }

    def append(Node node, List<Path> paths) {
        paths.each {
            node.appendNode("root", [url: it.ideaURL])
        }
    }
}

class IJScalaLibrary extends IJMavenLibrary {

    String languageLevel
    List<Path> classpath = []

    @Override
    def write(Node root) {
        def node = super.write(root)
        def props = node.appendNode("properties")
        props.appendNode("language-level", languageLevel)
        def classpath = props.appendNode("compiler-classpath")
        this.classpath.each {
            classpath.appendNode("root", [url: it.fileURL])
        }
    }
}

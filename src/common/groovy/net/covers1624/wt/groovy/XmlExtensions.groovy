package net.covers1624.wt.groovy

import groovy.xml.XmlUtil

import java.nio.file.Files
import java.nio.file.Path

/**
 * Some extensions for groovy's Node Xml stuff.
 *
 * Created by covers1624 on 20/7/19.
 */
class XmlExtensions {

    static Node parseXml(Path self) {
        Files.newInputStream(self).withCloseable {
            new XmlParser().parse(it)
        }
    }

    static Node parseXml(String self) {
        new XmlParser().parseText(self)
    }

    static void write(Path self, Node node) {
        self.write(XmlUtil.serialize(node))
    }
}

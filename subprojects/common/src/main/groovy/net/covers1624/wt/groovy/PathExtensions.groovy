/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.groovy

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Some fancy extensions for java.nio.Path
 *
 * Created by covers1624 on 20/7/19.
 */
class PathExtensions {

    static void write(Path self, String data) {
        write(self, data, StandardCharsets.UTF_8)
    }

    static void write(Path self, String data, Charset charset) {
        if (!self.exists) {
            Files.createDirectories(self.getParent())
        } else {
            Files.delete(self)
        }
        Files.write(self, data.getBytes(charset))
    }

    static String getAbsolutePath(Path self) {
        self.toAbsolutePath().normalize().toString()
    }

    static boolean isExists(Path self) {
        Files.exists(self)
    }

    //See org.gradle.plugins.ide.idea.model.PathFactory#path(File)
    static String getIdeaURL(Path self) {
        if (self.fileName.toString().endsWith('.jar')) {
            return "jar://${self.absolutePath.toString()}!/"
        }
        return self.fileURL
    }

    static String getFileURL(Path self) {
        return "file://${self.absolutePath.toString()}"
    }

}

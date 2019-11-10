/*
 * Copyright (c) 2018-2019 covers1624
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package net.covers1624.wt.util;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Created by covers1624 on 30/01/19.
 */
public enum ScalaVersion implements Comparable<ScalaVersion> {
    Scala_2_9("2.9"),
    Scala_2_10("2.10"),
    Scala_2_11("2.11"),
    Scala_2_12("2.12"),
    Scala_2_13("2.13"),
    Scala_2_14("2.14"),
    Scala_3_0("0.15", "3.0");

    @NotNull
    private final String version;
    @NotNull
    private final String name;

    ScalaVersion(String version) {
        this(version, version);
    }

    ScalaVersion(String version, String name) {
        this.version = version;
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    @NotNull
    public static Optional<ScalaVersion> findByVersion(@NotNull String version) {
        for (ScalaVersion languageLevel : values()) {
            if (version.startsWith(languageLevel.version)) {
                return Optional.of(languageLevel);
            }
        }

        return Optional.empty();
    }

}

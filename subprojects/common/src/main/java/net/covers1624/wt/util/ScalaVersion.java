/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

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

    private final String version;

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

    public static Optional<ScalaVersion> findByVersion(String version) {
        for (ScalaVersion languageLevel : values()) {
            if (version.startsWith(languageLevel.version)) {
                return Optional.of(languageLevel);
            }
        }

        return Optional.empty();
    }

}

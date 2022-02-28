/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

/**
 * Created by covers1624 on 13/6/21.
 */
public enum JavaVersion {
    JAVA_5("1.5"),
    JAVA_6("1.6"),
    JAVA_7("1.7"),
    JAVA_8("1.8"),
    JAVA_9("9"),
    JAVA_10("10"),
    JAVA_11("11"),
    JAVA_12("12"),
    JAVA_13("13"),
    JAVA_14("14"),
    JAVA_15("15"),
    JAVA_16("16"),
    JAVA_17("17"),
    ;

    public final String version;

    JavaVersion(String version) {
        this.version = version;
    }
}

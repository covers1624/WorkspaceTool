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
    JAVA_18("18"),
    JAVA_19("19"),
    JAVA_20("20"),
    JAVA_21("21"),
    JAVA_22("22"),
    JAVA_23("23"),
    JAVA_24("24"),
    JAVA_25("25"),
    JAVA_26("26"),
    JAVA_27("27"),
    JAVA_28("28"),
    JAVA_29("29"),
    JAVA_30("30"),
    ;

    public final String version;

    JavaVersion(String version) {
        this.version = version;
    }
}

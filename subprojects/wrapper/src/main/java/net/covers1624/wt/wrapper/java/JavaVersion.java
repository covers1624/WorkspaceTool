/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper.java;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by covers1624 on 28/10/21.
 */
public enum JavaVersion {
    JAVA_1_1("1.1"),
    JAVA_1_2("1.2"),
    JAVA_1_3("1.3"),
    JAVA_1_4("1.4"),
    JAVA_1_5("1.5"),
    JAVA_1_6("6"),
    JAVA_1_7("7"),
    JAVA_1_8("8"),
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
    UNKNOWN("Unknown");

    private static final Pattern VERSION_SUBSTRING = Pattern.compile("^([0-9.]*)");
    private static final JavaVersion[] VALUES = values();

    public final String shortString;

    JavaVersion(String shortString) {
        this.shortString = shortString;
    }

    /**
     * Parses a Java version string.
     *
     * @param str The version string.
     * @return The JavaVersion, or <code>null</code> if one could not be found.
     */
    @Nullable
    public static JavaVersion parse(String str) {
        Matcher matcher = VERSION_SUBSTRING.matcher(str);
        if (!matcher.find()) {
            return null;
        }
        String version = matcher.group(1);
        int[] vSplit = vSplit(version);

        if (vSplit.length > 1 && vSplit[0] == 1) {
            return parseMajorVersion(vSplit[1]);
        }
        return parseMajorVersion(vSplit[0]);
    }

    public static JavaVersion parseFromClass(byte[] bytes) {
        if (bytes.length < 8) throw new IllegalArgumentException("Invalid class file. Must be at least 8 bytes.");
        return parseMajorVersion((bytes[7] & 0xFF) - 44);
    }

    private static JavaVersion parseMajorVersion(int major) {
        return major >= VALUES.length ? JavaVersion.UNKNOWN : VALUES[major - 1];
    }

    private static int[] vSplit(String version) {
        String[] vSplitStrings = version.split("\\.");
        int[] vSplit = new int[vSplitStrings.length];
        for (int i = 0; i < vSplitStrings.length; i++) {
            String str = vSplitStrings[i];
            try {
                vSplit[i] = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                // This would only happen if you somehow get past the regex match.
                throw new IllegalArgumentException("Failed to parse version string: " + version, e);
            }
        }
        return vSplit;
    }
}

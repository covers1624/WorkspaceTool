/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper.java;

/**
 * Created by covers1624 on 10/30/21.
 */
public enum Architecture {
    X86,
    X64,
    AARCH64;

    private static final Architecture CURRENT = compute();

    public static Architecture current() {
        return CURRENT;
    }

    private static Architecture compute() {
        String arch = System.getProperty("os.arch");
        switch (arch) {
            case "i386":
            case "x86":
                return X86;
            case "x64":
            case "x86_64":
            case "amd64":
            case "universal":
                return X64;
            case "aarch64":
                return AARCH64;
            default:
                throw new UnsupportedOperationException("Unsupported architecture. " + arch);
        }
    }
}

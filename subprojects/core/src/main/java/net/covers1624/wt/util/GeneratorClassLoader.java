/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

/**
 * Created by covers1624 on 22/04/19.
 */
public class GeneratorClassLoader extends ClassLoader {

    public GeneratorClassLoader() {
        super(GeneratorClassLoader.class.getClassLoader());
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}

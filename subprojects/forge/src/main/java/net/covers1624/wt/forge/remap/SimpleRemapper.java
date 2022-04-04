/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.remap;

import org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 1/11/19.
 */
public class SimpleRemapper extends Remapper {

    private final Map<String, String> map = new HashMap<>();

    protected void addMapping(String from, String to) {
        String existing = map.put(from, to);
        if (existing != null) {
            throw new IllegalStateException("Remapper only supports Unique from names, Tried to overwrite " + from + " -> " + existing + " with " + to);
        }
    }

    @Override
    public String mapRecordComponentName(String owner, String name, String descriptor) {
        return mapFieldName(owner, name, descriptor);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        String n = map.get(name);
        return n != null ? n : name;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        String n = map.get(name);
        return n != null ? n : name;
    }

    @Override
    public String mapInvokeDynamicMethodName(String name, String desc) {
        String n = map.get(name);
        return n != null ? n : name;
    }

}

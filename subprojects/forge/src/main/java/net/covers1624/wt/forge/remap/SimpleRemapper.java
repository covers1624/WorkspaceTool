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

    protected Map<String, String> fieldMap = new HashMap<>();
    protected Map<String, String> methodMap = new HashMap<>();

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        String n = fieldMap.get(name);
        return n != null ? n : name;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        String n = methodMap.get(name);
        return n != null ? n : name;
    }

    @Override
    public String mapInvokeDynamicMethodName(String name, String desc) {
        String n = methodMap.get(name);
        return n != null ? n : name;
    }

}

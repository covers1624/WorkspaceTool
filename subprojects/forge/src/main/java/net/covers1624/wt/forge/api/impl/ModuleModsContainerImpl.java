/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.api.impl;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import net.covers1624.wt.forge.api.script.ModuleModsContainer;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 26/10/19.
 */
public class ModuleModsContainerImpl extends GroovyObjectSupport implements ModuleModsContainer {

    private Map<String, String> modSourceSets = new HashMap<>();

    @Override
    public Object invokeMethod(String name, Object args) {
        List list = InvokerHelper.asList(args);
        if (list.size() != 1) {
            throw new MissingMethodException(name, getClass(), list.toArray(), false);
        }
        Object arg0 = list.get(0);
        if (!(arg0 instanceof CharSequence)) {
            throw new MissingMethodException(name, getClass(), list.toArray(), false);
        }
        modSourceSets.put(name, String.valueOf(arg0));
        return null;
    }

    @Override
    public Map<String, String> getModSourceSets() {
        return modSourceSets;
    }
}

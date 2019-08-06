package net.covers1624.wt.api.impl.script.module.dependency;

import net.covers1624.wt.api.script.module.dependency.IModuleDependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by covers1624 on 29/05/19.
 */
public class ModuleDependencyImpl implements IModuleDependency {

    private final String group;
    private final String moduleName;
    private final List<String> sourceSets;

    public ModuleDependencyImpl(String group, String moduleName, List<String> sourceSets) {
        this.group = group;
        this.moduleName = moduleName;
        this.sourceSets = Collections.unmodifiableList(new ArrayList<>(sourceSets));
    }

    public static ModuleDependencyImpl parse(String from) {
        String group = "";
        String moduleName;

        String[] segs = from.split(":");
        if (segs.length < 1) {
            throw new IllegalArgumentException("Invalid module notation, no source sets. 'group/name:sourceset1:sourceset2'");
        }

        if (segs[0].contains("/")) {
            String[] seg2 = segs[0].split("/");
            if (seg2.length != 2) {
                throw new IllegalArgumentException("Invalid module notation, more than one slash. 'group/name:sourceset1:sourceset2'");
            }
            group = seg2[0];
            moduleName = seg2[1];
        } else {
            moduleName = segs[0];
        }

        List<String> sourceSets;
        if (segs.length == 1) {
            sourceSets = Collections.singletonList("main");
        } else {
            sourceSets = Arrays.asList(segs).subList(1, segs.length);
        }

        return new ModuleDependencyImpl(group, moduleName, sourceSets);
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public List<String> getSourceSets() {
        return sourceSets;
    }
}

package net.covers1624.wt.api.module;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple Holder Object for Modules.
 *
 * Created by covers1624 on 20/7/19.
 */
public class ModuleList {

    /**
     * Any Non-Framework related Modules.
     */
    public List<Module> modules = new ArrayList<>();
    /**
     * Any Framework related Modules.
     */
    public List<Module> frameworkModules = new ArrayList<>();

    /**
     * @return An iterator with both Non-Framework and Framework modules.
     */
    public Iterable<Module> getAllModules() {
        return Iterables.concat(frameworkModules, modules);
    }

}

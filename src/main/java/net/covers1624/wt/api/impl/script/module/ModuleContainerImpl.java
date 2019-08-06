package net.covers1624.wt.api.impl.script.module;

import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.script.module.ModuleGroupSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 23/05/19.
 */
public class ModuleContainerImpl implements ModuleContainerSpec {

    private Map<String, ModuleGroupImpl> groups = new HashMap<>();

    @Override
    public void group(String name, Consumer<ModuleGroupSpec> consumer) {
        consumer.accept(groups.computeIfAbsent(name, ModuleGroupImpl::new));
    }

    public Map<String, ModuleGroupImpl> getGroups() {
        return groups;
    }
}

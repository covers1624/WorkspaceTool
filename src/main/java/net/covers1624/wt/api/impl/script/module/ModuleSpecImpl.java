package net.covers1624.wt.api.impl.script.module;

import net.covers1624.wt.api.script.module.ModuleSpec;
import net.covers1624.wt.api.script.module.SourceSetSpec;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 27/05/19.
 */
public class ModuleSpecImpl implements ModuleSpec {

    private Path path;
    private Map<String, SourceSetSpecImpl> sourceSets = new HashMap<>();

    @Override
    public void setDir(Path path) {
        this.path = path;
    }

    @Override
    public Path getDir() {
        return path;
    }

    @Override
    public void sourceSet(String name, Consumer<SourceSetSpec> consumer) {
        consumer.accept(sourceSets.computeIfAbsent(name, e -> new SourceSetSpecImpl()));
    }
}

package net.covers1624.wstool.intellij.workspace;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A module path is a list of segments, each segment is usually part of its path.
 * <p>
 * Created by covers1624 on 5/7/25.
 */
public record ModulePath(List<String> names) {

    public ModulePath {
        if (names.isEmpty()) throw new RuntimeException("Path can't be empty.");

        // Ensure names is always immutable.
        names = List.copyOf(names);
    }

    /**
     * @return The last segment. The module's name.
     */
    public String name() {
        return names.get(names.size() - 1);
    }

    /**
     * Append another name to the path.
     *
     * @param name The name to append.
     * @return The new path.
     */
    public ModulePath with(String name) {
        List<String> strings = new ArrayList<>(names);
        strings.add(name);
        return new ModulePath(strings);
    }

    /**
     * Append multiple names to the path.
     *
     * @param names The nams to append.
     * @return The new path.
     */
    public ModulePath with(List<String> names) {
        List<String> strings = new ArrayList<>(this.names);
        strings.addAll(names);
        return new ModulePath(strings);
    }

    /**
     * @return A path representing the parent module.
     */
    public @Nullable ModulePath parent() {
        if (names.size() == 1) return null;

        return new ModulePath(List.copyOf(names.subList(0, names.size() - 1)));
    }

    /**
     * @return A relative path from within the root path.
     */
    // TODO this is a mega hack specifically for calculating the folders of groups.
    //      We should probably remove this, or improve group adding.
    public @Nullable ModulePath tail() {
        if (names.size() == 1) return null;

        return new ModulePath(List.copyOf(names.subList(1, names.size())));
    }

    /**
     * Join all the parts of this module together with some seperator.
     *
     * @param seperator The seperator to use.
     * @return The joined string.
     */
    public String joinNames(String seperator) {
        return String.join(seperator, names);
    }

    @Override
    public String toString() {
        return joinNames(".");
    }
}

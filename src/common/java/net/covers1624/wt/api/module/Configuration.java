package net.covers1624.wt.api.module;

import com.google.common.collect.Iterables;
import net.covers1624.wt.api.dependency.Dependency;

import java.util.*;
import java.util.function.Consumer;

/**
 * Represents a Gradle Configuration.
 *
 * Created by covers1624 on 30/6/19.
 */
public interface Configuration {

    /**
     * @return The name of the Configuration.
     */
    String getName();

    /**
     * @return True if this Configuration exports any configurations provided by {@link #getExtendsFrom()}
     */
    boolean isTransitive();

    /**
     * Any Configurations that this Configuration extends from.
     *
     * @return The Extended Configurations
     */
    Set<Configuration> getExtendsFrom();

    /**
     * Add a Configuration to the Extended-From set.
     *
     * @param configuration The Configuration.
     */
    void addExtendsFrom(Configuration configuration);

    /**
     * Replaces all Configurations in the Extended-From Set with the provided Set.
     *
     * @param extendsFrom The set.
     */
    void setExtendsFrom(Set<Configuration> extendsFrom);

    /**
     * Walks the Configuration hierarchy ignoring duplicates.
     *
     * @param consumer The Consumer.
     */
    default void walkHierarchy(Consumer<Configuration> consumer) {
        Set<String> used = new HashSet<>();
        used.add(getName());
        consumer.accept(this);
        if (isTransitive()) {
            Deque<Configuration> deque = new ArrayDeque<>(getExtendsFrom());
            while (!deque.isEmpty()) {
                Configuration other = deque.pop();
                if (used.add(other.getName())) {
                    consumer.accept(other);
                    if (other.isTransitive()) {
                        deque.addAll(other.getExtendsFrom());
                    }
                }
            }
        }
    }

    /**
     * Gets any Dependencies provided by this Configuration.
     *
     * @return The Dependencies.
     */
    List<Dependency> getDependencies();

    /**
     * Adds a {@link Dependency} to this Configuration.
     *
     * @param dependency The {@link Dependency}.
     */
    void addDependency(Dependency dependency);

    /**
     * Replaces all Dependencies provided by this Configuration with the provided list.
     *
     * @param dependencies The Dependencies.
     */
    void setDependencies(List<Dependency> dependencies);

    /**
     * Gets all transitive Dependencies provided by this Configuration.
     *
     * @return The Dependencies.
     */
    default Iterable<Dependency> getAllDependencies() {
        List<Iterable<Dependency>> toJoin = new ArrayList<>();
        walkHierarchy(e -> toJoin.add(e.getDependencies()));
        return Iterables.concat(toJoin);
    }
}

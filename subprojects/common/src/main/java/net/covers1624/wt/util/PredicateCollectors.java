/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import java.util.function.Predicate;
import java.util.stream.Collector;

/**
 * Operations for collecting a stream of Predicates together.
 * <p>
 * Created by covers1624 on 23/05/19.
 */
public class PredicateCollectors {

    /**
     * Ands together all Predicates in the stream.
     * Gradle method naming, for sanity.
     *
     * @return The collector.
     */
    public static <T> Collector<Predicate<T>, PredicateMerger<T>, Predicate<T>> intersects() {
        return and();
    }

    /**
     * Ands together all Predicates in the stream.
     *
     * @return The collector.
     */
    public static <T> Collector<Predicate<T>, PredicateMerger<T>, Predicate<T>> and() {
        return Collector.of(PredicateMerger::new, PredicateMerger::hold, PredicateMerger::and, PredicateMerger::get);
    }

    /**
     * Ors together all Predicates in the stream.
     * Gradle method naming, for sanity.
     *
     * @return The collector.
     */
    public static <T> Collector<Predicate<T>, PredicateMerger<T>, Predicate<T>> union() {
        return or();
    }

    /**
     * Ors together all Predicates in the stream.
     *
     * @return The collector.
     */
    public static <T> Collector<Predicate<T>, PredicateMerger<T>, Predicate<T>> or() {
        return Collector.of(PredicateMerger::new, PredicateMerger::hold, PredicateMerger::or, PredicateMerger::get);
    }

    //Eww ThrowAway object because the collectors system is weird.
    //We have to fold into a container, then merge the containers..
    private static class PredicateMerger<T> {

        public Predicate<T> predicate;

        public PredicateMerger() {
        }

        public PredicateMerger(Predicate<T> predicate) {
            this.predicate = predicate;
        }

        public void hold(Predicate<T> predicate) {
            this.predicate = predicate;
        }

        public Predicate<T> get() {
            return predicate;
        }

        public PredicateMerger<T> or(PredicateMerger<T> other) {
            return new PredicateMerger<>(predicate.or(other.predicate));
        }

        public PredicateMerger<T> and(PredicateMerger<T> other) {
            return new PredicateMerger<>(predicate.or(other.predicate));
        }
    }
}

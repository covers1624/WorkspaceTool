/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import groovy.lang.Closure;

import java.util.function.Consumer;

/**
 * Simple Consumer that delegates to a Closure.
 * Force's the Closure to resolve {@link Closure#DELEGATE_FIRST}
 * Created by covers1624 on 17/05/19.
 */
public class ClosureBackedConsumer<T> implements Consumer<T> {

    private final Closure<T> closure;
    private final int resolveStrategy;

    public ClosureBackedConsumer(Closure<T> closure) {
        this(closure, Closure.DELEGATE_FIRST);
    }

    public ClosureBackedConsumer(Closure<T> closure, int resolveStrategy) {
        this.closure = closure;
        this.resolveStrategy = resolveStrategy;
    }

    @Override
    public void accept(T t) {
        Closure copy = (Closure) closure.clone();
        copy.setResolveStrategy(resolveStrategy);
        copy.setDelegate(t);
        if (copy.getMaximumNumberOfParameters() == 0) {
            copy.call();
        } else {
            copy.call(t);
        }
    }
}

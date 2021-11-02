/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.event;

/**
 * Represents an event that has a result.
 *
 * Created by covers1624 on 30/6/19.
 */
public class ResultEvent<T> extends Event {

    private final boolean isGreedy;
    private boolean hasResult;
    private T thing;

    protected ResultEvent(boolean isGreedy) {
        this.isGreedy = isGreedy;
    }

    public void setResult(T thing) {
        this.thing = thing;
        this.hasResult = true;
    }

    public T getResult() {
        return thing;
    }

    public boolean hasResult() {
        return hasResult;
    }

    public boolean isGreedy() {
        return isGreedy;
    }

}

/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

/**
 * Created by covers1624 on 18/11/19.
 */
public interface ThrowingFunction<T, R, E extends Throwable> {

    R apply(T thing) throws E;

}

/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

/**
 * Created by covers1624 on 12/01/19.
 */
public interface ThrowingRunnable<E extends Throwable> {

    void run() throws E;

}

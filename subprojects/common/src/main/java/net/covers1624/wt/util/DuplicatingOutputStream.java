/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by covers1624 on 8/8/19.
 */
public class DuplicatingOutputStream extends OutputStream {

    private final List<OutputStream> sinks = new ArrayList<>();

    public DuplicatingOutputStream(OutputStream... sinks) {
        Collections.addAll(this.sinks, sinks);
    }

    public DuplicatingOutputStream(List<OutputStream> sinks) {
        this.sinks.addAll(sinks);
    }

    @Override
    public void write(int b) throws IOException {
        for (OutputStream s : sinks) {
            s.write(b);
        }
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        for (OutputStream s : sinks) {
            s.write(b);
        }
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        for (OutputStream s : sinks) {
            s.write(b, off, len);
        }
    }
}

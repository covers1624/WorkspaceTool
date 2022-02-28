/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import net.covers1624.tconsole.AbstractTail;

/**
 * Created by covers1624 on 10/8/19.
 */
public class OverallProgressTail extends AbstractTail {

    private final long start = System.currentTimeMillis();

    public OverallProgressTail() {
        super(2);
    }

    @Override
    public void onInitialized() {
        setLine(0, "===============================");
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        setLine(1, "Elapsed: [" + formatDurationTerse(now - start) + "]");
    }

    public static String formatDurationTerse(long elapsedTimeInMs) {
        StringBuilder result = new StringBuilder();
        if (elapsedTimeInMs > 3600000L) {
            result.append(elapsedTimeInMs / 3600000L).append("h ");
        }

        if (elapsedTimeInMs > 60000L) {
            result.append(elapsedTimeInMs % 3600000L / 60000L).append("m ");
        }

        if (elapsedTimeInMs >= 1000L) {
            result.append(elapsedTimeInMs % 60000L / 1000L).append("s");
        } else {
            result.append(elapsedTimeInMs).append("ms");
        }

        return result.toString();
    }
}

/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.event;

/**
 * An Event!
 * Events are fired on each Event's EventRegistry.
 * <p>
 * Created by covers1624 on 30/6/19.
 */
public class Event {

    public enum Priority {
        FIRST,
        HIGH,
        NORMAL,
        LOW,
        LAST
    }
}

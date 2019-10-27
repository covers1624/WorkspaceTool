package net.covers1624.wt.api.tail;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by covers1624 on 10/8/19.
 */
public interface AnsiTailConsole {

    boolean isSupported();

    void install();

    void uninstall();

    void redrawTails();

    List<TailGroup> getTailGroups();

    TailGroup newGroup();

    TailGroup newGroupBefore(TailGroup other);

    TailGroup newGroupAfter(TailGroup other);

    void removeGroup(TailGroup group);

    void setUpdateFixedRate(long interval, TimeUnit unit);

    void schedule(Runnable task);
}

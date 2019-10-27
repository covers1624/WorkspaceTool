package net.covers1624.wt.tail;

import net.covers1624.wt.api.tail.AnsiTailConsole;

/**
 * Created by covers1624 on 10/8/19.
 */
public interface AnsiTailConsoleInternal extends AnsiTailConsole {

    boolean isInitialized();

    void clearTails();

    void drawTails();
}

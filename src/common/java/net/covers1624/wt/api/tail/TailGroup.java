package net.covers1624.wt.api.tail;

import java.util.List;

/**
 * Created by covers1624 on 10/8/19.
 */
public interface TailGroup {

    List<Tail> getTails();

    AnsiTailConsole getConsole();

    <T extends Tail> T addBefore(Tail existing, T toAdd);

    <T extends Tail> T addAfter(Tail existing, T toAdd);

    <T extends Tail> T add(T toAdd);
}

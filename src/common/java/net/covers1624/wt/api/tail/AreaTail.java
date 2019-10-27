package net.covers1624.wt.api.tail;

import net.covers1624.wt.api.tail.AbstractTail;
import net.covers1624.wt.api.tail.AnsiTailConsole;
import net.covers1624.wt.api.tail.Tail;
import net.covers1624.wt.api.tail.TailGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by covers1624 on 10/8/19.
 */
public class AreaTail extends AbstractTail{

    private String[] lines = new String[0];

    public void setSize(int size) {
        lines = Arrays.copyOf(lines, size);
    }

    public String[] getLines() {
        return lines;
    }

    @Override

    public void buildLines(List<String> lines) {
        Collections.addAll(lines, this.lines);
    }
}

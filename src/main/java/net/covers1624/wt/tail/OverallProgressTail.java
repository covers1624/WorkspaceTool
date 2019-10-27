package net.covers1624.wt.tail;

import net.covers1624.wt.api.tail.AbstractTail;
import net.covers1624.wt.api.tail.AnsiTailConsole;
import net.covers1624.wt.api.tail.Tail;
import net.covers1624.wt.api.tail.TailGroup;
import org.gradle.internal.logging.format.TersePrettyDurationFormatter;

import java.util.List;

/**
 * Created by covers1624 on 10/8/19.
 */
public class OverallProgressTail extends AbstractTail implements Tail {

    private final TersePrettyDurationFormatter formatter = new TersePrettyDurationFormatter();
    private final long start = System.currentTimeMillis();

    @Override
    public void buildLines(List<String> lines) {
        long now = System.currentTimeMillis();
        lines.add("===============================");
        lines.add("Elapsed: [" + formatter.format(now - start) + "]");
    }
}

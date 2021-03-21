package net.covers1624.wt.util;

import net.covers1624.tconsole.AbstractTail;
import org.gradle.internal.logging.format.TersePrettyDurationFormatter;

/**
 * Created by covers1624 on 10/8/19.
 */
public class OverallProgressTail extends AbstractTail {

    private final TersePrettyDurationFormatter formatter = new TersePrettyDurationFormatter();
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
        setLine(1, "Elapsed: [" + formatter.format(now - start) + "]");
    }
}

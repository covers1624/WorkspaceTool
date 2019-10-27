package net.covers1624.wt.tail;

import net.covers1624.wt.api.tail.AnsiTailConsole;
import net.covers1624.wt.api.tail.Tail;
import net.covers1624.wt.api.tail.TailGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by covers1624 on 10/8/19.
 */
public class TailGroupImpl implements TailGroup {

    private final AnsiTailConsole console;

    private final List<Tail> tails = Collections.synchronizedList(new ArrayList<>());

    public TailGroupImpl(AnsiTailConsole console) {
        this.console = console;
    }

    @Override
    public List<Tail> getTails() {
        return Collections.unmodifiableList(tails);
    }

    @Override
    public AnsiTailConsole getConsole() {
        return console;
    }

    @Override
    public <T extends Tail> T addBefore(Tail existing, T toAdd) {
        int idx = tails.indexOf(existing);
        if (idx == -1) {
            throw new RuntimeException("Tail doesn't exist in group.");
        }
        toAdd.__setGroup(this);
        tails.add(idx, toAdd);
        return toAdd;
    }

    @Override
    public <T extends Tail> T addAfter(Tail existing, T toAdd) {
        int idx = tails.indexOf(existing);
        if (idx == -1) {
            throw new RuntimeException("Tail doesn't exist in group.");
        }
        toAdd.__setGroup(this);
        if (idx == tails.size() - 1) {
            tails.add(toAdd);
        } else {
            tails.add(idx + 1, toAdd);
        }
        return toAdd;
    }

    @Override
    public <T extends Tail> T add(T toAdd) {
        toAdd.__setGroup(this);
        tails.add(toAdd);
        return toAdd;
    }
}

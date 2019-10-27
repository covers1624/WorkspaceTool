package net.covers1624.wt.api.tail;

/**
 * Created by covers1624 on 10/8/19.
 */
public abstract class AbstractTail implements Tail {

    private TailGroup group;

    @Override
    public TailGroup getGroup() {
        return group;
    }

    @Override
    public void __setGroup(TailGroup group) {
        this.group = group;
    }

    @Override
    public <T extends Tail> T addBefore(T toAdd) {
        return group.addBefore(this, toAdd);
    }

    @Override
    public <T extends Tail> T addAfter(T toAdd) {
        return group.addAfter(this, toAdd);
    }

    @Override
    public void scheduleUpdate() {
        group.getConsole().redrawTails();
    }
}

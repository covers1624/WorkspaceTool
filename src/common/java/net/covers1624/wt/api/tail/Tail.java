package net.covers1624.wt.api.tail;

import java.util.List;

/**
 * Created by covers1624 on 10/8/19.
 */
public interface Tail {

    /**
     * @return Gets the group this tail belongs to.
     */
    TailGroup getGroup();

    void __setGroup(TailGroup group);

    /**
     * Adds a new tail before this tail.
     *
     * @param toAdd The tail to add.
     * @return The added tail.
     */
    <T extends Tail> T addBefore(T toAdd);

    /**
     * Adds a new tail after this tail.
     *
     * @param toAdd The tail to add.
     * @return The added tail.
     */
    <T extends Tail> T addAfter(T toAdd);

    /**
     * Add all lines for this Tail to the list.
     *
     * @param lines The list to add to.
     */
    void buildLines(List<String> lines);

    /**
     * Triggers a re-draw of all Tails.
     * Custom Tails can force an update on data changes.
     */
    void scheduleUpdate();

}

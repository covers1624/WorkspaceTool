package net.covers1624.wt.api.tail;

import java.util.List;

/**
 * Created by covers1624 on 10/8/19.
 */
public class TextTail extends AbstractTail {

    private String text = "";

    @Override
    public void buildLines(List<String> lines) {
        lines.add(text);
    }

    //@formatter:off
    public String getText() { return text; }
    public void setText(String text) { this.text = text; scheduleUpdate(); }
    //@formatter:on
}

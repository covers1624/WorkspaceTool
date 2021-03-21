package net.covers1624.wt.gradle;

import net.covers1624.tconsole.api.TailConsole;
import net.covers1624.tconsole.api.TailGroup;
import net.covers1624.tconsole.tails.TextTail;
import net.covers1624.wt.api.WorkspaceToolContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gradle.tooling.events.*;

/**
 * Created by covers1624 on 21/3/21.
 */
public class GradleProgressListener implements ProgressListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private final WorkspaceToolContext context;
    private final TailGroup group;
    private final TextTail tail;

    public GradleProgressListener(WorkspaceToolContext context, TailGroup group) {
        this.context = context;
        this.group = group;
        this.tail = group.add(new TextTail(2));
        tail.setLine(0, "===============================");
        tail.setLine(1, "IDLE...");
    }

    @Override
    public void statusChanged(ProgressEvent event) {
        //TODO dont add this listener instead of this.
        if (!context.console.isSupported(TailConsole.Output.STDOUT)) {
            return;
        }
        String line = null;
        if (event instanceof StartEvent) {
            OperationDescriptor parent = event.getDescriptor().getParent();
            line = parent == null ? "Idle..." : resolve(parent);
        } else if (event instanceof FinishEvent) {
            line = resolve(event.getDescriptor());
        }
        if (line == null) {
            return;
        }
        String header = "Exporting data: ";
        int maxLen = tail.getTerminalWidth() - header.length();
        if (line.length() >= maxLen) {
            int trimLen = line.length() - maxLen + 5;
            line = line.substring(trimLen);
            line = " ..." + line;
        }
        tail.setLine(1, header + line);
    }

    public String resolve(OperationDescriptor descriptor) {
        StringBuilder builder = new StringBuilder();
        if (descriptor.getParent() != null) {
            builder.append(resolve(descriptor.getParent())).append(" > ");
        }
        String name = descriptor.getDisplayName();
        if (name.contains("Build parameterized model") && name.contains("for root project")) {
            String project = name.substring(name.lastIndexOf("root project") + 13);
            builder.append("Extract WT data for ").append(project);
        } else {
            builder.append(descriptor.getDisplayName());
        }
        return builder.toString();
    }
}

package net.covers1624.wt.tail;

import net.covers1624.wt.api.tail.AnsiTailConsole;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.PrintStream;
import java.io.Serializable;

/**
 * Created by covers1624 on 9/8/19.
 */
@Plugin (name = "AnsiTailConsoleAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class AnsiTailConsoleAppender extends AbstractAppender {

    private static final PrintStream sysOut = System.out;
    private static boolean initialized;
    private static AnsiTailConsoleInternal tailConsole;

    protected AnsiTailConsoleAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        if (!initialized) {
            initialized = true;
            tailConsole = new AnsiTailConsoleImpl();
            tailConsole.install();
        }
    }

    public static AnsiTailConsole getTailConsole() {
        return tailConsole;
    }

    @Override
    public void append(LogEvent event) {
        String str = getLayout().toSerializable(event).toString();
        if (tailConsole != null && tailConsole.isSupported() && tailConsole.isInitialized()) {
            tailConsole.schedule(() -> {
                tailConsole.clearTails();
                sysOut.print(str);
                tailConsole.drawTails();
            });
        } else {
            sysOut.print(str);
        }
    }

    @PluginFactory
    public static AnsiTailConsoleAppender createAppender(//
            @Required (message = "No name provided for AnsiTailConsoleAppender")//
            @PluginAttribute ("name")//
                    String name,//
            @PluginElement ("Filter")//
                    Filter filter,//l
            @PluginElement ("Layout")//
            @Nullable//
                    Layout<? extends Serializable> layout,//
            @PluginAttribute (value = "ignoreExceptions", defaultBoolean = true)//
                    boolean ignoreExceptions) {
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        return new AnsiTailConsoleAppender(name, filter, layout, ignoreExceptions);
    }
}

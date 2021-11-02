package net.covers1624.wt.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by covers1624 on 8/8/19.
 */
public class LoggingOutputStream extends OutputStream {

    private final Logger logger;
    private final Level level;
    private StringBuilder buffer = new StringBuilder();

    public LoggingOutputStream(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    @Override
    public void write(int b) throws IOException {
        char ch = (char) (b & 0xFF);
        buffer.append(ch);
        if (ch == '\n' || ch == '\r') {
            flush();
        }
    }

    @Override
    public void flush() throws IOException {
        if (buffer.length() == 0) {
            return;
        }
        char end = buffer.charAt(buffer.length() - 1);
        if (end == '\n' || end == '\r') {
            logger.log(level, buffer.toString().trim());
            buffer.setLength(0);
        }
    }
}

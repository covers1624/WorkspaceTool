package net.covers1624.wt.api.tail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.Ansi;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import static java.text.MessageFormat.format;

/**
 * Created by covers1624 on 10/8/19.
 */
public class DownloadProgressTail extends AbstractTail {

    private static final Logger logger = LogManager.getLogger("DownloadProgressTail");

    private Status status = Status.IDLE;
    private String fileName;
    private long totalLen;
    private long progress;
    private long startTime;

    private boolean newData = false;

    private List<String> lineCache = new ArrayList<>();

    private Terminal terminal;

    public DownloadProgressTail() {
        try {
            terminal = TerminalBuilder.terminal();
        } catch (IOException e) {
            logger.debug("Failed to initialize Terminal.", e);
        }
    }

    @Override
    public void buildLines(List<String> lines) {
        if (newData) {
            lineCache.clear();
            Ansi a = Ansi.ansi();
            if (status == Status.DOWNLOADING) {

                int termWidth = terminal.getWidth();
                double done = (double) progress / (double) totalLen;
                long elapsed = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);

                double speedBps = ((double) progress / elapsed);
                if (Double.isInfinite(speedBps)) {
                    speedBps = 0;
                }

                String prefix = "Downloading: " + fileName + " ";
                String progressString = format(" {0}/{1}({2}%) {3}", humanSize(progress), humanSize(totalLen), (int) (done * 100), humanSpeed(speedBps));
                int remaining = termWidth - (progressString.length() + prefix.length());
                remaining -= 2;//Square brackets.

                a.a(prefix);

                Ansi.Color color = done > 0.75D ? Ansi.Color.GREEN : (done > 0.50D ? Ansi.Color.YELLOW : Ansi.Color.RED);

                int width = (int) Math.floor(done * remaining);
                a.fg(color).a("[").bold();
                for (int i = 1; i <= remaining; i++) {
                    if (i <= width) {
                        if (i == width) {
                            a.a(">");
                        } else {
                            a.a("=");
                        }
                    } else {
                        a.a("-");
                    }
                }
                a.boldOff();

                a.a("]").fgDefault().a(progressString);
            } else {
                a.a("Idle..");
            }
            lineCache.add(a.toString());
            newData = false;
        }
        lines.addAll(lineCache);
    }

    //@formatter:off
    public Status getStatus() { return status; }
    public String getFileName() { return fileName; }
    public long getTotalLen() { return totalLen; }
    public long getProgress() { return progress; }
    public long getStartTime() { return startTime; }
    public void setStatus(Status status) { this.status = status; onNewData(); }
    public void setFileName(String fileName) { this.fileName = fileName; onNewData(); }
    public void setTotalLen(long totalLen) { this.totalLen = totalLen; onNewData(); }
    public void setProgress(long progress) { this.progress = progress; onNewData(); }
    public void setStartTime(long startTime) { this.startTime = startTime; onNewData(); }
    private void onNewData() { newData = true; }
    //@formatter:on

    private static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private static String humanSpeed(double bytes) {
        if (bytes < 1024) {
            return String.format("%.2f B/s", bytes);
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB/s", (bytes / 1024));
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB/s", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB/s", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public enum Status {
        IDLE,
        DOWNLOADING
    }

    public static class Pool {

        private final TailGroup group;
        private final ArrayDeque<DownloadProgressTail> tails = new ArrayDeque<>();

        public Pool(TailGroup group) {
            this.group = group;
        }

        public DownloadProgressTail pop() {
            DownloadProgressTail ret;
            synchronized (tails) {
                ret = tails.poll();
            }
            if (ret == null) {
                ret = group.add(new DownloadProgressTail());
            }
            return ret;
        }

        public void push(DownloadProgressTail tail) {
            synchronized (tails) {
                tails.addLast(tail);
            }
        }
    }

}

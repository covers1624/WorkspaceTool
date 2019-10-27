package net.covers1624.wt.tail;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.covers1624.wt.api.tail.Tail;
import net.covers1624.wt.api.tail.TailGroup;
import org.apache.logging.log4j.LogManager;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.internal.CLibrary;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by covers1624 on 10/8/19.
 */
public class AnsiTailConsoleImpl implements AnsiTailConsoleInternal {

    private static final ThreadFactory factory = new ThreadFactoryBuilder()//
            .setDaemon(true)//
            .setNameFormat("AnsiTailConsole Executor")//
            .build();

    private final List<TailGroup> groups = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(factory);
    private final boolean isatty;
    private ScheduledFuture updateIntervalFuture = null;

    private PrintStream ansiOut;
    private boolean initialized = false;

    private List<String> lineCache = new ArrayList<>();
    private int drawnLines = 0;

    public AnsiTailConsoleImpl() {
        boolean result = false;
        try {
            result = CLibrary.isatty(CLibrary.STDOUT_FILENO) != 0;
        } catch (Throwable e) {
            System.out.println("Ansi output disabled due to native error.");
            LogManager.getLogger("AnsiConsoleImpl").debug(e);
        }
        isatty = result;
    }

    @Override
    public boolean isSupported() {
        return isatty;
    }

    @Override
    public void install() {
        if (initialized || !isSupported()) {
            return;
        }
        AnsiConsole.systemInstall();
        ansiOut = AnsiConsole.out();
        initialized = true;
    }

    @Override
    public void uninstall() {
        if (!initialized || !isSupported()) {
            return;
        }
        AnsiConsole.systemUninstall();
        initialized = false;
        executor.shutdown();
        try {
            while (!executor.awaitTermination(200, TimeUnit.MILLISECONDS)) {

            }
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void redrawTails() {
        if (!isSupported()) {
            return;
        }
        schedule(() -> {
            clearTails();
            drawTails();
        });
    }

    @Override
    public List<TailGroup> getTailGroups() {
        return Collections.unmodifiableList(groups);
    }

    @Override
    public TailGroup newGroup() {
        TailGroup group = new TailGroupImpl(this);
        groups.add(group);
        return group;
    }

    @Override
    public TailGroup newGroupBefore(TailGroup other) {
        int idx = groups.indexOf(other);
        if (idx == -1) {
            throw new RuntimeException("Group does not exist.");
        }
        TailGroup group = new TailGroupImpl(this);
        groups.add(idx, group);
        return group;
    }

    @Override
    public TailGroup newGroupAfter(TailGroup other) {
        int idx = groups.indexOf(other);
        if (idx == -1) {
            throw new RuntimeException("Group does not exist.");
        }
        TailGroup group = new TailGroupImpl(this);
        if (idx == groups.size() - 1) {
            groups.add(group);
        } else {
            groups.add(idx, group);
        }
        return group;
    }

    @Override
    public void removeGroup(TailGroup group) {
        groups.remove(group);
    }

    @Override
    public void clearTails() {
        if (!isSupported()) {
            return;
        }
        if (drawnLines != 0) {
            Ansi reset = Ansi.ansi();
            for (int i = 0; i < drawnLines; i++) {
                reset.cursorUpLine().eraseLine(Ansi.Erase.ALL);
            }
            ansiOut.print(reset);
            drawnLines = 0;
        }
        ansiOut.flush();
    }

    @Override
    public void drawTails() {
        if (!isSupported()) {
            return;
        }
        lineCache.clear();
        Ansi a = Ansi.ansi();
        a.cursorToColumn(0);
        for (TailGroup group : groups) {
            List<Tail> tails = group.getTails();
            for (Tail tail : tails) {
                tail.buildLines(lineCache);
            }
        }
        drawnLines = lineCache.size();
        for (int i = 0; i < drawnLines; i++) {
            String line = lineCache.get(i);
            if (i != 0) {
                a.newline();
            }
            a.a(line.trim()).reset();
        }
        ansiOut.print(a.newline());
        ansiOut.flush();
    }

    @Override
    public void setUpdateFixedRate(long interval, TimeUnit unit) {
        if (!isSupported()) {
            return;
        }
        if (updateIntervalFuture != null) {
            updateIntervalFuture.cancel(false);
            try {
                updateIntervalFuture.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }
        updateIntervalFuture = executor.scheduleAtFixedRate(() -> {
            clearTails();
            drawTails();
        }, 0, interval, unit);
    }

    @Override
    public void schedule(Runnable task) {
        if (!isSupported()) {
            return;
        }
        executor.submit(task);
    }

}

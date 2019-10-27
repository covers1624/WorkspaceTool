package net.covers1624.wt;

import net.covers1624.wt.api.tail.AnsiTailConsole;
import net.covers1624.wt.api.tail.DownloadProgressTail;
import net.covers1624.wt.api.tail.TailGroup;
import net.covers1624.wt.tail.AnsiTailConsoleAppender;
import net.covers1624.wt.tail.OverallProgressTail;
import net.rubygrapefruit.platform.Native;
import net.rubygrapefruit.platform.terminal.Terminals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Created by covers1624 on 13/6/19.
 */
public class TestStuff {

    private static final Logger logger = LogManager.getLogger("TestStuff");

    public static void main(String[] args) throws Exception {
        Terminals terminals = Native.get(Terminals.class);
        System.out.println(terminals.isTerminal(Terminals.Output.Stdout));

//        AnsiTailConsole console = AnsiTailConsoleAppender.getTailConsole();
//        console.setUpdateFixedRate(200, TimeUnit.MILLISECONDS);
//        TailGroup group = console.newGroup();
//        group.add(new OverallProgressTail());
//        DownloadProgressTail progress = group.add(new DownloadProgressTail());
//
//        logger.info("Boop");
//        logger.info("Boop");
//        logger.info("Boop");
//        int fileLength = 1024 * 1024 * 1024;
//        progress.setFileName("someTestFile.txt");
//        progress.setTotalLen(fileLength);
//        progress.setStartTime(System.currentTimeMillis());
//        progress.setStatus(DownloadProgressTail.Status.DOWNLOADING);
//        for (int i = 0; i < 1024 * 1024 * 1024; i += 1024 * 1024) {
//            progress.setProgress(i);
//
//            Thread.sleep(200);
//        }
//        progress.setStatus(DownloadProgressTail.Status.IDLE);

    }

}

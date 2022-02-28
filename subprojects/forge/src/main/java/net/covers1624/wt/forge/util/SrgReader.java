/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.util;

import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Created by covers1624 on 10/01/19.
 */
public class SrgReader {

    /**
     * Reads the specified SRG File, giving each element to the LineConsumer.
     * <p>
     * See {@link LineConsumer#consume(LineType, String[])} for indexes.
     *
     * @param file     The File to read.
     * @param consumer The LineConsumer.
     */
    public static void readSrg(File file, LineConsumer consumer) {
        readSrg(file.toPath(), consumer);
    }

    /**
     * Reads the specified SRG File, giving each element to the LineConsumer.
     * <p>
     * See {@link LineConsumer#consume(LineType, String[])} for indexes.
     *
     * @param path     The Path to read.
     * @param consumer The LineConsumer.
     */
    public static void readSrg(Path path, LineConsumer consumer) {
        try {
            Files.lines(path).filter(StringUtils::isNotEmpty).filter(e -> !e.startsWith("#")).forEach(line -> {
                String type = line.substring(0, 2);
                String[] args = line.substring(4).split(" ");
                switch (type) {
                    case "PK" -> consumer.consume(LineType.PACKAGE, Arrays.copyOf(args, 2));
                    case "CL" -> consumer.consume(LineType.CLASS, Arrays.copyOf(args, 2));
                    case "FD" -> {
                        String[] newArgs = new String[4];
                        int lastSlash1 = args[0].lastIndexOf('/');
                        newArgs[0] = args[0].substring(0, lastSlash1);
                        newArgs[1] = args[0].substring(lastSlash1 + 1);

                        int lastSlash2 = args[1].lastIndexOf('/');
                        newArgs[2] = args[1].substring(0, lastSlash2);
                        newArgs[3] = args[1].substring(lastSlash2 + 1);
                        consumer.consume(LineType.FIELD, newArgs);
                    }
                    case "MD" -> {
                        String[] newArgs = new String[6];
                        int lastSlash1 = args[0].lastIndexOf('/');
                        newArgs[0] = args[0].substring(0, lastSlash1);
                        newArgs[1] = args[0].substring(lastSlash1 + 1);
                        newArgs[2] = args[1];

                        int lastSlash2 = args[2].lastIndexOf('/');
                        newArgs[3] = args[2].substring(0, lastSlash2);
                        newArgs[4] = args[2].substring(lastSlash2 + 1);
                        newArgs[5] = args[3];
                        consumer.consume(LineType.METHOD, newArgs);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to read SRG file: " + path, e);
        }
    }

    public interface LineConsumer {

        /**
         * Called for each line in an SRG file.
         * ArrayIndexes:
         * <p>
         * PACKAGE:
         * 0 = Old Package
         * 1 = New Package
         * <p>
         * CLASS:
         * 0 = Old Class
         * 1 = New Class
         * <p>
         * FIELD:
         * 0 = Old Class
         * 1 = Old Name
         * <p>
         * 2 = New Class
         * 3 = New name
         * <p>
         * METHOD:
         * 0 = Old Class
         * 1 = Old Name
         * 2 = Old Desc
         * <p>
         * 3 = New Class
         * 4 = New Name
         * 5 = New Desc
         *
         * @param type The Line Type.
         * @param args The Arguments for that line.
         */
        void consume(LineType type, String[] args);
    }

    public enum LineType {
        PACKAGE,
        CLASS,
        FIELD,
        METHOD
    }

}

/*
 * Copyright (c) 2018-2019 covers1624
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
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
     *
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
     *
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
                    case "PK":
                        consumer.consume(LineType.PACKAGE, Arrays.copyOf(args, 2));
                        break;
                    case "CL":
                        consumer.consume(LineType.CLASS, Arrays.copyOf(args, 2));
                        break;
                    case "FD": {
                        String[] newArgs = new String[4];
                        int lastSlash1 = args[0].lastIndexOf('/');
                        newArgs[0] = args[0].substring(0, lastSlash1);
                        newArgs[1] = args[0].substring(lastSlash1 + 1);

                        int lastSlash2 = args[1].lastIndexOf('/');
                        newArgs[2] = args[1].substring(0, lastSlash2);
                        newArgs[3] = args[1].substring(lastSlash2 + 1);
                        consumer.consume(LineType.FIELD, newArgs);
                        break;
                    }
                    case "MD": {
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
                        break;
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
         *
         * PACKAGE:
         * 0 = Old Package
         * 1 = New Package
         *
         * CLASS:
         * 0 = Old Class
         * 1 = New Class
         *
         * FIELD:
         * 0 = Old Class
         * 1 = Old Name
         *
         * 2 = New Class
         * 3 = New name
         *
         * METHOD:
         * 0 = Old Class
         * 1 = Old Name
         * 2 = Old Desc
         *
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

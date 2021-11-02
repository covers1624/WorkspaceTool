/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.remap;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import net.covers1624.wt.util.Utils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 1/11/19.
 */
public class CSVRemapper extends SimpleRemapper {

    public CSVRemapper(Path zip) throws IOException {
        try (FileSystem jarFS = Utils.getJarFileSystem(zip, true)) {
            Path fieldsCSV = jarFS.getPath("/fields.csv");
            Path methodsCSV = jarFS.getPath("/methods.csv");

            parseCSV(fieldsCSV, line -> fieldMap.put(line[0], line[1]));
            parseCSV(methodsCSV, line -> methodMap.put(line[0], line[1]));
        }
    }

    private void parseCSV(Path path, Consumer<String[]> processor) throws IOException {
        try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(path)).withSkipLines(1).build()) {
            for (String[] line : reader) {
                processor.accept(line);
            }
        }
    }
}

/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.remap;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static net.covers1624.quack.io.IOUtils.getJarFileSystem;

/**
 * Created by covers1624 on 1/11/19.
 */
public class CSVRemapper extends SimpleRemapper {

    public CSVRemapper(Path zip) throws IOException {
        try (FileSystem jarFS = getJarFileSystem(zip, true)) {
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

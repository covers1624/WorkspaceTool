/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple column formatter. Likely horrible.
 * Created by covers1624 on 6/08/18.
 */
public class ColFormatter {

    public static List<String> format(List<List<String>> input) {
        List<List<String>> cols = rotateLists(input);
        List<List<String>> newCols = new ArrayList<>();
        for (List<String> col : cols) {
            int max = ColUtils.maxBy(col, String::length).length();
            List<String> newCol = new ArrayList<>();
            for (String cell : col) {
                StringBuilder str = new StringBuilder(cell);
                if (cell.length() < max) {
                    for (int i = cell.length(); i < max; i++) {
                        str.append(" ");
                    }
                }
                newCol.add(str.toString());
            }
            newCols.add(newCol);
        }
        List<String> lines = new ArrayList<>();
        List<List<String>> rows = rotateLists(newCols);
        for (List<String> row : rows) {
            StringBuilder builder = new StringBuilder();
            for (String cell : row) {
                if (builder.capacity() != 0) {
                    builder.append(" ");
                }
                builder.append(cell);
            }
            lines.add(builder.toString());
        }
        return lines;
    }

    public static List<List<String>> rotateLists(List<List<String>> input) {
        if (input.isEmpty()) {
            return input;
        }
        List<List<String>> sqLst = toSquare(input);
        List<List<String>> cols = new ArrayList<>();
        for (int colIndex = 0; colIndex < sqLst.get(0).size(); colIndex++) {
            List<String> col = new ArrayList<>();
            for (List<String> row : sqLst) {
                col.add(row.get(colIndex));
            }
            cols.add(col);
        }
        return cols;
    }

    public static List<List<String>> toSquare(List<List<String>> input) {
        int len = ColUtils.maxBy(input, List::size).size();
        if (ColUtils.forAll(input, e -> e.size() == len)) {
            return input;
        }
        List<List<String>> out = new ArrayList<>();
        for (List<String> row : input) {
            List<String> elm = new ArrayList<>(row);
            if (row.size() != len) {
                for (int i = row.size(); i < len; i++) {
                    elm.add(" ");
                }
            }
            out.add(elm);
        }
        return out;
    }

}

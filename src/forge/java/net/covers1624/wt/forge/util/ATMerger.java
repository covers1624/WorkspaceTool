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

import net.covers1624.wt.util.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static net.covers1624.wt.util.ParameterFormatter.format;

/**
 * Created by covers1624 on 8/01/19.
 */
public class ATMerger {

    private Map<String, AtClass> classMap = new HashMap<>();

    public void consume(Path atFile) {
        try (Stream<String> lines = Files.lines(atFile)) {
            lines.forEach(_line -> {
                String line = _line;
                int hashIdx = line.indexOf('#');
                if (hashIdx != -1) {
                    line = line.substring(0, hashIdx).trim();
                }
                line = line.replace(".", "/");//Forge has some broken AT lines. Just guard against this..
                String[] segs = line.split(" ");
                if (segs.length > 3) {
                    throw new RuntimeException("Invalid AT line: '" + _line + "', File: " + atFile);
                }
                if (line.isEmpty()) {
                    return;//basically continue from inside a lambda.
                }

                //Parse changes.
                FinalChange finalChange = FinalChange.fromBools(segs[0].endsWith("-f"), segs[0].endsWith("+f"));
                AccessChange accessChange = AccessChange.fromName(segs[0].replace("-f", "").replace("+f", ""));

                //Lookup class.
                String owner = segs[1];
                AtClass atClass = classMap.computeIfAbsent(owner, AtClass::new);
                if (segs.length == 2) {
                    //Its a class, merge access.
                    atClass.mergeAccess(accessChange);
                    atClass.mergeFinal(finalChange);
                } else {
                    String name = segs[2];
                    //Is it a method?
                    if (name.contains("(")) {
                        AtMethod method;
                        //Check for a wildcard method.
                        if (!atClass.methods.containsKey("*()")) {
                            //Lookup method.
                            method = atClass.methods.computeIfAbsent(name, AtMethod::new);
                            //If its a wildcard, remove all others.
                            if (method.isWild()) {
                                atClass.methods.values().removeIf(e -> e != method);
                            }
                        } else {
                            //Lookup the wildcard method.
                            method = atClass.methods.get("*()");
                        }
                        //Merge access.
                        method.mergeAccess(accessChange);
                        method.mergeFinal(finalChange);
                    } else {
                        AtField field;
                        //Check for a wildcard field.
                        if (!atClass.fields.containsKey("*")) {
                            //Lookup the field.
                            field = atClass.fields.computeIfAbsent(name, AtField::new);
                            //If its a wildcard, remove all others.
                            if (field.isWild()) {
                                atClass.fields.values().removeIf(e -> e != field);
                            }
                        } else {
                            //Lookup the wildcard field.
                            field = atClass.fields.get("*");
                        }
                        //Merge access.
                        field.mergeAccess(accessChange);
                        field.mergeFinal(finalChange);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Error reading AccessTransformer file: " + atFile, e);
        }
    }

    public void write(Path outputFile) {
        if (Files.exists(outputFile)) {
            Utils.sneaky(() -> Files.delete(outputFile));
        }
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, WRITE, CREATE))) {
            boolean first = true;
            for (AtClass clazz : classMap.values()) {
                if (!first) {
                    writer.println();
                }
                first = false;
                if (clazz.accessChange != null) {
                    writer.println(format("{}{} {}", clazz.accessChange.seg, clazz.finalChange.seg, clazz.name));
                }
                for (AtField field : clazz.fields.values()) {
                    writer.println(format("{}{} {} {}", field.accessChange.seg, field.finalChange.seg, clazz.name, field.name));
                }
                for (AtMethod method : clazz.methods.values()) {
                    writer.println(format("{}{} {} {}{}", method.accessChange.seg, method.finalChange.seg, clazz.name, method.name, method.desc));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to write merged at to: " + outputFile, e);
        }
    }

    public static abstract class AtNode {

        public AccessChange accessChange;
        public FinalChange finalChange = FinalChange.NONE;

        protected AtNode(AccessChange default_) {
            accessChange = default_;
        }

        public void mergeAccess(AccessChange other) {
            accessChange = accessChange.merge(other);
        }

        public void mergeFinal(FinalChange other) {
            finalChange = finalChange.merge(other);
        }

    }

    public static class AtClass extends AtNode {

        public String name;
        public Map<String, AtMethod> methods = new HashMap<>();
        public Map<String, AtField> fields = new HashMap<>();

        public AtClass(String name) {
            super(null);
            this.name = name;
        }

        @Override
        public void mergeAccess(AccessChange other) {
            if (accessChange == null) {
                accessChange = other;
            } else {
                super.mergeAccess(other);
            }
        }
    }

    public static class AtMethod extends AtNode {

        public String name;
        public String desc;

        public AtMethod(String name) {
            super(AccessChange.PRIVATE);
            int descIdx = name.indexOf("(");
            if (descIdx == -1) {
                throw new RuntimeException("Wot? " + descIdx);
            }
            this.name = name.substring(0, descIdx);
            desc = name.substring(descIdx);
        }

        public boolean isWild() {
            return name.equals("*");
        }
    }

    public static class AtField extends AtNode {

        public String name;

        public AtField(String name) {
            super(AccessChange.PRIVATE);
            this.name = name;
        }

        public boolean isWild() {
            return name.equals("*");
        }
    }

    public enum AccessChange {
        PUBLIC,
        PROTECTED,
        PRIVATE;

        public String seg;

        AccessChange() {
            this.seg = name().toLowerCase();
        }

        public static AccessChange fromName(String name) {
            for (AccessChange acc : values()) {
                if (acc.name().equalsIgnoreCase(name)) {
                    return acc;
                }
            }
            throw new AssertionError("Unknown access change: " + name);
        }

        public AccessChange merge(AccessChange other) {
            if (this.ordinal() > other.ordinal()) {
                return other;
            }
            return this;
        }
    }

    public enum FinalChange {
        STRIP("-f"),
        MARK("+f"),
        NONE();

        public String seg;

        FinalChange() {
            this("");
        }

        FinalChange(String seg) {
            this.seg = seg;
        }

        public static FinalChange fromBools(boolean strip, boolean mark) {
            if (strip) {
                return STRIP;
            } else if (mark) {
                return MARK;
            }
            return NONE;
        }

        public FinalChange merge(FinalChange other) {
            if (this.ordinal() > other.ordinal()) {
                return other;
            }
            return this;
        }

    }

}

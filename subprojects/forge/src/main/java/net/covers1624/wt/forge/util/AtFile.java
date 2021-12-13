/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.util;

import com.google.common.base.Strings;
import net.covers1624.quack.util.SneakyUtils.ThrowingFunction;
import net.minecraftforge.srgutils.IMappingFile;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.objectweb.asm.Type;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static net.covers1624.quack.collection.ColUtils.iterable;
import static net.covers1624.quack.util.SneakyUtils.sneaky;

/**
 * Created by covers1624 on 16/11/19.
 */
public class AtFile {

    private final List<String> fileComment = new ArrayList<>();
    private final Map<String, AtClass> classMap = new LinkedHashMap<>();
    private boolean useDot;
    private boolean groupByPackage;

    public AtFile() {
    }

    public AtFile(Path path) {
        this(path, CompressionMethod.NONE);
    }

    public AtFile(Path path, CompressionMethod cMethod) {
        this();
        parse(path, cMethod);
    }

    public AtFile useDot() {
        useDot = true;
        return this;
    }

    public AtFile groupByPackage() {
        groupByPackage = true;
        return this;
    }

    private void parse(Path atFile, CompressionMethod cMethod) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(cMethod.wrapInput(Files.newInputStream(atFile))))) {
            boolean foundEntries = false;
            for (String _line : iterable(reader.lines())) {
                String line = _line;
                int hashIdx = line.indexOf('#');
                String lineComment = null;
                if (hashIdx != -1) {
                    lineComment = line.substring(hashIdx + 1).trim();
                    line = line.substring(0, hashIdx).trim();
                }
                line = line.replace(".", "/"); // Normalize dots to slashes.
                String[] segs = line.split(" ");
                if (segs.length > 3) {
                    throw new RuntimeException("Invalid AT line: '" + _line + "', File: " + atFile);
                }
                if (line.isEmpty()) {
                    if (lineComment != null && !foundEntries) {
                        fileComment.add(lineComment);
                    }
                    continue;
                }

                //Parse changes.
                FinalChange finalChange = FinalChange.fromBools(segs[0].endsWith("-f"), segs[0].endsWith("+f"));
                AccessChange accessChange = AccessChange.fromName(segs[0].replace("-f", "").replace("+f", ""));

                //Lookup class.
                String owner = segs[1];
                AtClass atClass = classMap.computeIfAbsent(owner, AtClass::new);
                if (segs.length == 2) {
                    atClass.comment = lineComment;

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
                            method.comment = lineComment;
                            //If its a wildcard, remove all others.
                            if (method.isWild()) {
                                atClass.methods.values().removeIf(e -> e != method && e.finalChange == FinalChange.NONE);
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
                            field.comment = lineComment;
                            //If its a wildcard, remove all others.
                            if (field.isWild()) {
                                atClass.fields.values().removeIf(e -> e != field && e.finalChange == FinalChange.NONE);
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
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading AccessTransformer file: " + atFile, e);
        }
    }

    public AtFile remap(IMappingFile mappings, IMappingFile commentMappings) {
        AtFile newFile = new AtFile();
        newFile.fileComment.addAll(fileComment);
        for (AtClass oldClass : classMap.values()) {
            IMappingFile.IClass clazz = mappings.getClass(oldClass.name);
            IMappingFile.IClass commentClass = commentMappings != null ? commentMappings.getClass(oldClass.name) : null;
            AtClass newClass = new AtClass(clazz == null ? oldClass.name : clazz.getMapped());
            newClass.accessChange = oldClass.accessChange;
            newClass.finalChange = oldClass.finalChange;
            newClass.comment = commentClass != null ? commentClass.getMapped() : null;

            newFile.classMap.put(newClass.name, newClass);

            for (AtMethod oldMethod : oldClass.methods.values()) {
                IMappingFile.IMethod method = clazz != null ? clazz.getMethod(oldMethod.name, oldMethod.desc) : null;
                IMappingFile.IMethod methodComment = commentClass != null ? commentClass.getMethod(oldMethod.name, oldMethod.desc) : null;
                AtMethod newMethod = new AtMethod(method != null ? method.getMapped() : oldMethod.name, method != null ? method.getMappedDescriptor() : oldMethod.desc);
                if (newMethod.name.equals("<init>")) {
                    newMethod.desc = remapType(mappings, Type.getMethodType(newMethod.desc)).getDescriptor();
                }
                newMethod.accessChange = oldMethod.accessChange;
                newMethod.finalChange = oldMethod.finalChange;
                newMethod.comment = methodComment != null ? methodComment.getMapped() : null;

                newClass.methods.put(newMethod.name + newMethod.desc, newMethod);
            }

            for (AtField oldField : oldClass.fields.values()) {
                IMappingFile.IField field = clazz != null ? clazz.getField(oldField.name) : null;
                IMappingFile.IField fieldComment = commentClass != null ? commentClass.getField(oldField.name) : null;
                AtField newField = new AtField(field != null ? field.getMapped() : oldField.name);
                newField.accessChange = oldField.accessChange;
                newField.finalChange = oldField.finalChange;
                newField.comment = fieldComment != null ? fieldComment.getMapped() : null;

                newClass.fields.put(newField.name, newField);
            }
        }

        return newFile;
    }

    private static Type remapType(IMappingFile mappings, Type type) {
        switch (type.getSort()) {
            case Type.VOID:
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                return type;
            case Type.ARRAY:
                int dims = type.getDimensions();
                Type elementType = type.getElementType();
                return Type.getObjectType(Strings.repeat("[", dims) + remapType(mappings, elementType));
            case Type.OBJECT:
                IMappingFile.IClass clazz = mappings.getClass(type.getInternalName());
                return clazz != null ? Type.getObjectType(clazz.getMapped()) : type;
            case Type.METHOD:
                Type[] types = type.getArgumentTypes();
                Type returnType = type.getReturnType();
                for (int i = 0; i < types.length; i++) {
                    types[i] = remapType(mappings, types[i]);
                }
                return Type.getMethodType(remapType(mappings, returnType), types);
            default:
                throw new AssertionError();
        }
    }

    public void sort() {
        List<AtClass> classes = new ArrayList<>(classMap.values());
        classes.sort(Comparator.comparing(e -> e.name));
        classMap.clear();
        for (AtClass clazz : classes) {
            classMap.put(clazz.name, clazz);

            List<AtField> fields = new ArrayList<>(clazz.fields.values());
            fields.sort(Comparator.comparing(e -> e.name));
            clazz.fields.clear();
            fields.forEach(e -> clazz.fields.put(e.name, e));

            List<AtMethod> methods = new ArrayList<>(clazz.methods.values());
            methods.sort(Comparator.comparing(e -> e.name));
            clazz.methods.clear();
            methods.forEach(e -> clazz.methods.put(e.name + e.desc, e));
        }
    }

    public void merge(AtFile other) {
        other.classMap.forEach((cName, cNode) -> {
            AtClass atClass = getClass(cName);
            atClass.mergeAccess(cNode.accessChange);
            atClass.mergeFinal(cNode.finalChange);
            cNode.methods.forEach((mName, mNode) -> {
                AtMethod atMethod = atClass.methods.get("*()");
                if (atMethod == null) {
                    atMethod = atClass.getMethod(mName);
                    if (atMethod.isWild()) {
                        AtMethod finalAtMethod = atMethod;
                        atClass.methods.values().removeIf(e -> e != finalAtMethod && e.finalChange == FinalChange.NONE);
                    }
                }
                atMethod.mergeAccess(mNode.accessChange);
                atMethod.mergeFinal(mNode.finalChange);
            });
            cNode.fields.forEach((fName, fNode) -> {
                AtField atField = atClass.fields.get("*");
                if (atField == null) {
                    atField = atClass.getField(fName);
                    if (atField.isWild()) {
                        AtField finalAtField = atField;
                        atClass.fields.values().removeIf(e -> e != finalAtField && e.finalChange == FinalChange.NONE);
                    }
                }
                atField.mergeAccess(fNode.accessChange);
                atField.mergeFinal(fNode.finalChange);
            });
        });
    }

    public void write(Path outputFile) {
        write(outputFile, CompressionMethod.NONE);
    }

    public void write(Path outputFile, CompressionMethod cMethod) {
        if (Files.exists(outputFile)) {
            sneaky(() -> Files.delete(outputFile));
        }
        try (PrintWriter writer = new PrintWriter(cMethod.wrapOutput(Files.newOutputStream(outputFile, WRITE, CREATE)))) {
            for (String s : fileComment) {
                writer.println("#" + s);
            }
            if (!fileComment.isEmpty()) {
                writer.println();
            }
            boolean first = true;
            String lastPackage = null;
            for (AtClass clazz : classMap.values()) {
                String classPackage = clazz.name;
                int idx = classPackage.lastIndexOf("/");
                if (idx != -1) {
                    classPackage = classPackage.substring(0, idx);
                }
                if (!first && (!groupByPackage || !classPackage.equals(lastPackage))) {
                    writer.println();
                }
                first = false;
                if (clazz.accessChange != null) {
                    String comment = clazz.comment == null ? "" : " #" + clazz.comment;
                    writer.println(MessageFormat.format("{0}{1} {2}{3}", clazz.accessChange.seg, clazz.finalChange.seg, clazz.name(useDot), comment));
                }
                for (AtField field : clazz.fields.values()) {
                    String comment = field.comment == null ? "" : " #" + field.comment;
                    writer.println(MessageFormat.format("{0}{1} {2} {3}{4}", field.accessChange.seg, field.finalChange.seg, clazz.name(useDot), field.name, comment));
                }
                for (AtMethod method : clazz.methods.values()) {
                    String comment = method.comment == null ? "" : " #" + method.comment;
                    writer.println(MessageFormat.format("{0}{1} {2} {3}{4}{5}", method.accessChange.seg, method.finalChange.seg, clazz.name(useDot), method.name, method.desc, comment));
                }
                lastPackage = classPackage;
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write AtFile to: " + outputFile, e);
        }
    }

    public AtClass getClass(String className) {
        return classMap.computeIfAbsent(className, AtClass::new);
    }

    public abstract static class AtNode {

        public String comment;
        public AccessChange accessChange;
        public FinalChange finalChange = FinalChange.NONE;

        protected AtNode(AccessChange default_) {
            accessChange = default_;
        }

        public void mergeAccess(AccessChange other) {
            if (other == null) return;
            accessChange = accessChange.merge(other);
        }

        public void mergeFinal(FinalChange other) {
            if (other == null) return;
            finalChange = finalChange.merge(other);
        }

    }

    public static class AtClass extends AtNode {

        public String name;
        public Map<String, AtMethod> methods = new LinkedHashMap<>();
        public Map<String, AtField> fields = new LinkedHashMap<>();

        public AtClass(String name) {
            super(null);
            this.name = name;
        }

        public AtMethod getMethod(String name) {
            return methods.computeIfAbsent(name, AtMethod::new);
        }

        public AtField getField(String name) {
            return fields.computeIfAbsent(name, AtField::new);
        }

        public String name(boolean useDot) {
            if (useDot) {
                return name.replace("/", ".");
            }
            return name;
        }

        @Override
        public void mergeAccess(AccessChange other) {
            if (accessChange == null) {
                accessChange = other;
            } else {
                super.mergeAccess(other);
            }
        }

        @Override
        public void mergeFinal(FinalChange other) {
            if (finalChange == null) {
                finalChange = other;
            } else {
                super.mergeFinal(other);
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

        public AtMethod(String name, String desc) {
            super(AccessChange.PRIVATE);
            this.name = name;
            this.desc = desc;
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
        DEFAULT,
        PRIVATE;

        public String seg;

        AccessChange() {
            seg = name().toLowerCase();
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
            if (ordinal() > other.ordinal()) {
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
            }
            if (mark) {
                return MARK;
            }
            return NONE;
        }

        public FinalChange merge(FinalChange other) {
            if (ordinal() > other.ordinal()) {
                return other;
            }
            return this;
        }
    }

    public enum CompressionMethod {
        NONE(e -> e, e -> e),
        XZ(XZCompressorInputStream::new, XZCompressorOutputStream::new),
        ZIP(InflaterInputStream::new, DeflaterOutputStream::new);

        private final ThrowingFunction<InputStream, InputStream, IOException> isFunc;
        private final ThrowingFunction<OutputStream, OutputStream, IOException> osFunc;

        CompressionMethod(ThrowingFunction<InputStream, InputStream, IOException> isFunc, ThrowingFunction<OutputStream, OutputStream, IOException> osFunc) {
            this.isFunc = isFunc;
            this.osFunc = osFunc;
        }

        public InputStream wrapInput(InputStream is) throws IOException {
            return isFunc.apply(is);
        }

        public OutputStream wrapOutput(OutputStream os) throws IOException {
            return osFunc.apply(os);
        }

    }

}

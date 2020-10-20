package net.covers1624.wt.forge.util;

import net.covers1624.wt.util.ThrowingFunction;
import net.covers1624.wt.util.Utils;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static net.covers1624.wt.util.ParameterFormatter.format;

/**
 * Created by covers1624 on 16/11/19.
 */
public class AtFile {

    private final Map<String, AtClass> classMap = new HashMap<>();
    private boolean useDot;

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

    private void parse(Path atFile, CompressionMethod cMethod) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(cMethod.wrapInput(Files.newInputStream(atFile))))) {
            reader.lines().forEach(_line -> {
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
                        atClass.methods.values().removeIf(e -> e != finalAtMethod);
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
                        atClass.fields.values().removeIf(e -> e != finalAtField);
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
            Utils.sneaky(() -> Files.delete(outputFile));
        }
        try (PrintWriter writer = new PrintWriter(cMethod.wrapOutput(Files.newOutputStream(outputFile, WRITE, CREATE)))) {
            boolean first = true;
            for (AtClass clazz : classMap.values()) {
                if (!first) {
                    writer.println();
                }
                first = false;
                if (clazz.accessChange != null) {
                    writer.println(format("{}{} {}", clazz.accessChange.seg, clazz.finalChange.seg, clazz.name()));
                }
                for (AtField field : clazz.fields.values()) {
                    writer.println(format("{}{} {} {}", field.accessChange.seg, field.finalChange.seg, clazz.name(), field.name));
                }
                for (AtMethod method : clazz.methods.values()) {
                    writer.println(format("{}{} {} {}{}", method.accessChange.seg, method.finalChange.seg, clazz.name(), method.name, method.desc));
                }
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write AtFile to: " + outputFile, e);
        }
    }

    public AtClass getClass(String className) {
        return classMap.computeIfAbsent(className, AtClass::new);
    }

    public abstract class AtNode {

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

    public class AtClass extends AtNode {

        public String name;
        public Map<String, AtMethod> methods = new HashMap<>();
        public Map<String, AtField> fields = new HashMap<>();

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

        public String name() {
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

    public class AtMethod extends AtNode {

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

    public class AtField extends AtNode {

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

    public enum CompressionMethod {
        NONE(e -> e, e -> e),
        XZ(XZCompressorInputStream::new, XZCompressorOutputStream::new),
        ZIP(InflaterInputStream::new, DeflaterOutputStream::new);

        private ThrowingFunction<InputStream, InputStream, IOException> isFunc;
        private ThrowingFunction<OutputStream, OutputStream, IOException> osFunc;

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

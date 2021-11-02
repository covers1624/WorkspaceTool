/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper.java;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.objectweb.asm.Opcodes.*;

/**
 * Generates a Java class to extract Key-value pairs of properties.
 * <p>
 * Created by covers1624 on 28/10/21.
 */
public class JavaPropExtractGenerator {

    /**
     * The list of properties extracted by this tool.
     */
    public static String[] DEFAULTS = {
            "java.home",
            "java.version",
            "java.vendor",
            "os.arch",
            "java.vm.name",
            "java.vm.version",
            "java.runtime.name",
            "java.runtime.version",
            "java.class.version",
    };
    private static final byte[] DEFAULT_CLASS_BYTES = generateClass(DEFAULTS);

    public static Path writeClass(Path folder) throws IOException {
        Path classFile = folder.resolve("PropExtract.class");
        Files.createDirectories(classFile.getParent());
        try (OutputStream os = Files.newOutputStream(classFile)) {
            os.write(DEFAULT_CLASS_BYTES);
            os.flush();
        }
        return classFile;
    }

    /**
     * Generates a minimal Java class compatible with any JVM version to
     * export the configured properties.
     *
     * @return The class bytes.
     */
    public static byte[] generateClass(String[] properties) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_1, ACC_PUBLIC | ACC_SUPER, "PropExtract", null, "java/lang/Object", null);
        MethodVisitor mv;

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        for (String property : properties) {
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(property + "=");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);

            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(property);
            mv.visitLdcInsn("");
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
        return cw.toByteArray();
    }
}

/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import codechicken.asm.InsnListSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.covers1624.quack.io.IOUtils.toBytes;
import static net.covers1624.quack.util.SneakyUtils.throwUnchecked;
import static org.objectweb.asm.Opcodes.*;

/**
 * Simple class Mixin Generator.
 * Basically class Squishing, no overlaps allowed.
 * <p>
 * Created by covers1624 on 8/8/19.
 */
public class ClassSmusher {

    private static final Logger logger = LogManager.getLogger("MixinGenerator");
    private static final boolean DEBUG = Boolean.getBoolean("net.covers1624.wt.class_smusher.debug");
    private static final AtomicInteger counter = new AtomicInteger();
    private final GeneratorClassLoader classLoader = new GeneratorClassLoader();

    public Class<?> createMixinClass(Class<?> targetIFace, Class<?> targetImpl, Map<Class<?>, Class<?>> mixins) {
        try {
            String cName = asmName(targetImpl) + "$$MixinDecorated_" + counter.getAndIncrement();
            ClassNode cNode = new ClassNode();
            cNode.visit(V1_8, ACC_PUBLIC | ACC_SYNTHETIC, cName, null, asmName(Object.class), null);
            cNode.interfaces.add(asmName(targetIFace));
            cNode.interfaces.addAll(mixins.keySet().stream().map(ClassSmusher::asmName).collect(Collectors.toList()));

            List<Class<?>> templates = new ArrayList<>();
            templates.add(targetImpl);
            templates.addAll(mixins.values());

            MethodVisitor ctor = cNode.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            MethodVisitor cinit = cNode.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);

            ctor.visitCode();
            cinit.visitCode();

            //Setup void constructor.
            ctor.visitVarInsn(ALOAD, 0);
            ctor.visitMethodInsn(INVOKESPECIAL, asmName(Object.class), "<init>", "()V", false);

            Map<String, String> remap = new HashMap<>();
            for (Class<?> template : templates) {
                ClassReader tReader = readerFor(template);
                ClassNode tNode = new ClassNode();
                tReader.accept(tNode, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG);
                remap.put(tNode.name, cName);
                for (FieldNode field : tNode.fields) {
                    if (cNode.fields.stream().anyMatch(e -> e.name.equals(field.name))) {
                        throw new RuntimeException("Field " + field.name + " already exists applying template: " + template);
                    }
                    cNode.fields.add(field);
                }
                for (MethodNode method : tNode.methods) {
                    if (method.name.equals("<init>")) {
                        if (!method.desc.equals("()V")) {
                            throw new RuntimeException("Found invalid constructor in template: " + template);
                        }
                        InsnListSection section = new InsnListSection(method.instructions);
                        //Drop first 2 instructions, always ALOAD0, INVOKESPECIAL since we skip debug.
                        section = section.drop(2);
                        //If return is the only thing left, its pointless.
                        if (section.size() == 1) {
                            continue;
                        }

                        String mName = "_init_" + counter.getAndIncrement();
                        MethodVisitor mNode = cNode.visitMethod(ACC_PRIVATE, mName, "()V", null, null);
                        mNode.visitCode();

                        section.accept(mNode);
                        mNode.visitMaxs(-1, -1);
                        mNode.visitEnd();

                        ctor.visitVarInsn(ALOAD, 0);
                        ctor.visitMethodInsn(INVOKEVIRTUAL, cName, mName, "()V", false);
                    } else if (method.name.equals("<clinit>")) {
                        InsnListSection section = new InsnListSection(method.instructions).copy();
                        //Drop first 2 instructions, always ALOAD0, INVOKESPECIAL since we skip debug.
                        section = section.drop(2);
                        //If return is the only thing left, its pointless.
                        if (section.size() == 1) {
                            continue;
                        }

                        String mName = "_clinit_" + counter.getAndIncrement();
                        MethodVisitor mNode = cNode.visitMethod(ACC_PRIVATE | ACC_STATIC, mName, "()V", null, null);
                        mNode.visitCode();

                        section.accept(mNode);
                        mNode.visitMaxs(-1, -1);
                        mNode.visitEnd();

                        cinit.visitMethodInsn(INVOKESTATIC, cName, mName, "()V", false);
                    } else {
                        if (cNode.methods.stream().anyMatch(e -> e.name.equals(method.name) && e.desc.equals(method.desc))) {
                            throw new RuntimeException("Method " + method.name + method.desc + " already exists applying template: " + template);
                        }
                        cNode.methods.add(method);
                    }
                }
            }
            ctor.visitInsn(RETURN);
            ctor.visitMaxs(-1, -1);
            ctor.visitEnd();

            cinit.visitInsn(RETURN);
            cinit.visitMaxs(-1, -1);
            cinit.visitEnd();

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassRemapper remapper = new ClassRemapper(writer, new SimpleRemapper(remap));
            cNode.accept(remapper);

            byte[] bytes = writer.toByteArray();
            if (DEBUG) {
                Path dump = Paths.get("asm/", cName.replace("/", ".") + ".class");
                if (Files.exists(dump)) {
                    Files.delete(dump);
                }
                if (Files.notExists(dump.getParent())) {
                    Files.createDirectories(dump.getParent());
                }
                try (OutputStream os = Files.newOutputStream(dump, StandardOpenOption.CREATE)) {
                    os.write(bytes);
                    os.flush();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write class.", e);
                }
            }
            return classLoader.defineClass(cName.replace("/", "."), bytes);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create mixin class.", e);
        }
    }

    public static String asmName(Class<?> clazz) {
        return clazz.getName().replace(".", "/");
    }

    public static ClassReader readerFor(Class<?> clazz) throws IOException {
        try (InputStream is = ClassSmusher.class.getResourceAsStream("/" + asmName(clazz) + ".class")) {
            if (is == null) {
                throwUnchecked(new ClassNotFoundException(clazz.getName()));
            }
            return new ClassReader(toBytes(is));
        }
    }
}

/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.util;

import net.covers1624.wt.forge.util.AccessUsageFile.FieldUsageNode;
import net.covers1624.wt.forge.util.AccessUsageFile.MethodUsageNode;
import net.covers1624.wt.forge.util.AccessUsageFile.TypeUsageNode;
import org.objectweb.asm.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static net.covers1624.quack.util.SneakyUtils.sneak;
import static net.covers1624.quack.util.SneakyUtils.sneaky;
import static net.covers1624.wt.forge.util.AtFile.AccessChange.*;
import static net.covers1624.wt.forge.util.AtFile.FinalChange.MARK;
import static net.covers1624.wt.forge.util.AtFile.FinalChange.NONE;
import static org.objectweb.asm.ClassReader.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Created by covers1624 on 17/11/19.
 */
public class AccessExtractor {

    private static final Set<Handle> META_FACTORIES = new HashSet<>(Arrays.asList(//
            new Handle(//
                    Opcodes.H_INVOKESTATIC,//
                    "java/lang/invoke/LambdaMetafactory",//
                    "metafactory",//
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",//
                    false//
            ),//
            new Handle(//
                    Opcodes.H_INVOKESTATIC,//
                    "java/lang/invoke/LambdaMetafactory",//
                    "altMetafactory",//
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",//
                    false//
            )//
    ));

    public static AtFile extractAccess(Set<Path> paths) {
        AtFile atFile = new AtFile();
        processClasses(paths, reader -> reader.accept(new AccessVisitor(atFile), SKIP_CODE));
        return atFile;
    }

    public static AccessUsageFile extractUsage(Set<Path> paths) {
        AccessUsageFile auFile = new AccessUsageFile();
        processClasses(paths, reader -> reader.accept(new AccessUsageClassVisitor(auFile), SKIP_DEBUG | SKIP_FRAMES));
        return auFile;
    }

    private static void processClasses(Set<Path> paths, Consumer<ClassReader> consumer) {
        paths.stream()//
                .flatMap(e -> sneaky(() -> Files.walk(e)))//
                .filter(e -> e.getFileName().toString().endsWith(".class"))//
                .filter(Files::exists)//
                .distinct()//
                .forEach(sneak(e -> {
                    try (InputStream is = Files.newInputStream(e)) {
                        ClassReader reader = new ClassReader(is);
                        consumer.accept(reader);
                    }
                }));
    }

    private static class AccessVisitor extends ClassVisitor {

        private final AtFile atFile;
        private AtFile.AtClass atClass;

        public AccessVisitor(AtFile atFile) {
            super(ASM7);
            this.atFile = atFile;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            atClass = atFile.getClass(name);
            processAccess(atClass, access);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            AtFile.AtMethod atMethod = atClass.getMethod(name + descriptor);
            processAccess(atMethod, access);
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            AtFile.AtField atField = atClass.getField(name);
            processAccess(atField, access);
            return null;
        }

        private static void processAccess(AtFile.AtNode atNode, int access) {
            if ((access & ACC_PUBLIC) != 0) {
                atNode.accessChange = PUBLIC;
            } else if ((access & ACC_PRIVATE) != 0) {
                atNode.accessChange = PRIVATE;
            } else if ((access & ACC_PROTECTED) != 0) {
                atNode.accessChange = PROTECTED;
            } else {
                atNode.accessChange = DEFAULT;
            }
            atNode.finalChange = (access & ACC_FINAL) != 0 ? MARK : NONE;
        }
    }

    private static class AccessUsageClassVisitor extends ClassVisitor {

        private final AccessUsageFile auFile;
        private AccessUsageFile.UsageClass uClass;

        public AccessUsageClassVisitor(AccessUsageFile auFile) {
            super(ASM7);
            this.auFile = auFile;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            uClass = auFile.getUsageClass(name);
            uClass.addUsageNode(new TypeUsageNode(superName));
            if (interfaces != null) {
                for (String iFace : interfaces) {
                    uClass.addUsageNode(new TypeUsageNode(iFace));
                }
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            handleType(Type.getType(descriptor));
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            handleType(Type.getMethodType(descriptor));
            return new AccessUsageMethodVisitor();
        }

        private class AccessUsageMethodVisitor extends MethodVisitor {

            public AccessUsageMethodVisitor() {
                super(ASM7);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                uClass.addUsageNode(new TypeUsageNode(owner));
                handleType(Type.getType(descriptor));
                uClass.addUsageNode(new FieldUsageNode(owner, name, opcode == PUTSTATIC || opcode == PUTFIELD));
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                handleType(Type.getType(type));
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                uClass.addUsageNode(new TypeUsageNode(owner));
                handleType(Type.getMethodType(descriptor));
                uClass.addUsageNode(new MethodUsageNode(owner, name, descriptor));
            }

            @Override
            public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bamArgs) {
                //Yaaay thanks FML.
                if (META_FACTORIES.contains(bsm)) {
                    String owner = Type.getReturnType(desc).getInternalName();
                    String odesc = ((Type) bamArgs[0]).getDescriptor(); // First constant argument is "samMethodType - Signature and return type of method to be implemented by the function object."
                    uClass.addUsageNode(new TypeUsageNode(owner));
                    handleType(Type.getMethodType(odesc));
                    uClass.addUsageNode(new MethodUsageNode(owner, name, odesc));
                }
            }
        }

        private void handleType(Type type) {
            if (type.getSort() == Type.OBJECT) {
                uClass.addUsageNode(new TypeUsageNode(type.getInternalName()));
            } else if (type.getSort() == Type.METHOD) {
                handleType(type.getReturnType());
                for (Type argType : type.getArgumentTypes()) {
                    handleType(type);
                }
            } else if (type.getSort() == Type.ARRAY) {
                handleType(type.getElementType());
            }
        }
    }

}


/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util.tree;

import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by covers1624 on 3/17/20.
 */
public class TreeMethodNode implements Serializable {

    @Nullable
    private transient final ClassTree tree;

    //Weather this method has been fully loaded.
    public boolean loaded;

    public TreeClassNode owner;

    public int access;
    public String name;
    public Parameter returnType = null;
    public Parameter[] parameters = new Parameter[0];
    public String desc;
    public String signature;
    public List<TreeClassNode> exceptions = new ArrayList<>();

    private TreeMethodNode() {
        this(null);
    }

    public TreeMethodNode(ClassTree tree) {
        this.tree = tree;
    }

    /**
     * Copies the data from the {@code other} node, into
     * this node, re-Linking the hierarchy of the other node
     * to the hierarchy of this node, in the event that the nodes
     * have different tree's or are loaded via a {@link ObjectInputStream}
     *
     * @param other The other node to copy data from.
     */
    public void copyFrom(TreeMethodNode other) {
        loaded = other.loaded;
        owner = tree.getClassNode(other.owner.name);
        access = other.access;
        name = other.name;
        desc = other.desc;
        signature = other.signature;
        exceptions.clear();
        other.exceptions.stream().map(e -> e.name).map(tree::getClassNode).forEach(exceptions::add);
        returnType = new Parameter().copyFrom(tree, other.returnType);
        parameters = new Parameter[other.parameters.length];
        for (int i = 0; i < other.parameters.length; i++) {
            parameters[i] = new Parameter().copyFrom(tree, other.parameters[i]);
        }
    }

    public MethodVisitor visitMethod(TreeClassNode owner, int access, String name, String desc, String signature, String[] exceptions) {
        this.owner = owner;
        loaded = true;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.exceptions.clear();
        if (exceptions != null) {
            Arrays.stream(exceptions).map(tree::getClassNode).forEach(this.exceptions::add);
        }

        Type descType = Type.getMethodType(desc);
        returnType = createParameter(-1, descType.getReturnType());
        Type[] params = descType.getArgumentTypes();
        parameters = new Parameter[params.length];
        for (int i = 0; i < params.length; i++) {
            parameters[i] = createParameter(i, params[i]);
        }

        return new Visitor((access & Opcodes.ACC_STATIC) != 0);
    }

    private Parameter createParameter(int idx, Type type) {
        boolean isPrimitive = isPrimitive(type);
        TreeClassNode treeType = isPrimitive ? null : tree.getClassNode(type.getInternalName());
        return new Parameter(idx, "", treeType, isPrimitive, type);
    }

    private boolean isPrimitive(Type type) {
        return type.getSort() == Type.ARRAY ? isPrimitive(type.getElementType()) : Type.VOID <= type.getSort() && type.getSort() <= Type.DOUBLE;
    }

    public static class Parameter implements Externalizable {

        public int idx;
        public String name;
        public TreeClassNode type;
        public boolean primitive;
        public Type desc;
        public String signature;

        public Parameter() {
        }

        public Parameter(int idx, String name, TreeClassNode type, boolean primitive, Type desc) {
            this.idx = idx;
            this.name = name;
            this.type = type;
            this.primitive = primitive;
            this.desc = desc;
            if (idx != -1 && StringUtils.isEmpty(name)) {
                this.name = "p" + idx;
            }
        }

        public Parameter copyFrom(ClassTree tree, Parameter other) {
            idx = other.idx;
            name = other.name;
            if (other.type != null) {
                type = tree.getClassNode(other.type.name);
            }
            primitive = other.primitive;
            desc = other.desc;
            signature = other.signature;
            return this;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(idx);
            out.writeObject(name);
            out.writeBoolean(primitive);
            out.writeObject(desc.getDescriptor());
            out.writeObject(signature);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            idx = in.readInt();
            name = (String) in.readObject();
            primitive = in.readBoolean();
            desc = Type.getType((String) in.readObject());
            signature = (String) in.readObject();
        }
    }

    private class Visitor extends MethodVisitor {

        private final boolean isStatic;
        private final List<ParameterHolder> parameters = new ArrayList<>();

        public Visitor(boolean isStatic) {
            super(Opcodes.ASM7);
            this.isStatic = isStatic;
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            //Store all parameters in the order that they get visited in, so we can extract signatures and names for method parameters.
            parameters.add(new ParameterHolder(index, name, signature));
        }

        @Override
        public void visitEnd() {
            //TreeMethodNode.this.parameters;
            for (int i = 0; i < parameters.size(); i++) {
                int index = isStatic ? i : i - 1;
                if (0 <= index && TreeMethodNode.this.parameters.length > index) {
                    Parameter parameter = TreeMethodNode.this.parameters[index];
                    ParameterHolder holder = parameters.get(i);
                    parameter.name = holder.name;
                    parameter.signature = holder.signature;
                }
            }
        }

        private class ParameterHolder {

            public final int idx;
            public final String name;
            public final String signature;

            private ParameterHolder(int idx, String name, String signature) {
                this.idx = idx;
                this.name = name;
                this.signature = signature;
            }
        }
    }

}

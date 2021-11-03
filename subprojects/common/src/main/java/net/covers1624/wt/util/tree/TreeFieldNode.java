/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util.tree;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Created by covers1624 on 3/17/20.
 */
public class TreeFieldNode implements Serializable {

    @Nullable
    private transient final ClassTree tree;

    //Weather this field has been fully loaded.
    public boolean loaded;

    public TreeClassNode owner;

    public int access;
    public String name;
    public String desc;
    public String signature;

    private TreeFieldNode() {
        this(null);
    }

    public TreeFieldNode(ClassTree tree) {
        this.tree = tree;
    }

    /**
     * Copies the data from the {@code other} node, into
     * this node. Re-Linking the hierarchy of the other node
     * to the hierarchy of this node, in the event that the nodes
     * have different tree's or are loaded via a {@link ObjectInputStream}
     *
     * @param other The other node to copy data from.
     */
    public void copyFrom(TreeFieldNode other) {
        loaded = other.loaded;
        owner = tree.getClassNode(other.owner.name);
        loaded = other.loaded;
        access = other.access;
        name = other.name;
        desc = other.desc;
        signature = other.signature;
    }

    public FieldVisitor visitField(TreeClassNode owner, int access, String name, String desc, String signature) {
        loaded = true;
        this.owner = owner;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        return new Visitor();
    }

    private class Visitor extends FieldVisitor {

        public Visitor() {
            super(Opcodes.ASM7);
        }
    }
}

package net.covers1624.wt.util.tree;

import com.google.common.collect.Streams;
import org.apache.commons.collections.map.LinkedMap;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by covers1624 on 3/17/20.
 */
public class TreeClassNode implements Serializable {

    @Nullable
    private transient final ClassTree tree;

    //Weather this class has been fully loaded.
    public boolean loaded;

    public int access;

    public String name;
    public String signature;

    public TreeClassNode superClass;
    //Map<ClassName, Node>
    public Map<String, TreeClassNode> interfaces = new LinkedHashMap<>();

    public TreeClassNode outerClass;
    public TreeMethodNode outerMethod;

    //Map<ClassName, Node>
    public Map<String, TreeClassNode> innerClasses = new LinkedHashMap<>();

    //Map<FieldName, Node>
    public Map<String, TreeFieldNode> fields = new LinkedHashMap<>();
    //Map<MethodName + MethodDesc, Node>
    public Map<String, TreeMethodNode> methods = new LinkedHashMap();

    //Serializable.
    private TreeClassNode() {
        this(null);
    }

    public TreeClassNode(ClassTree tree) {
        this.tree = tree;
    }

    public void copyFrom(TreeClassNode other) {
        this.loaded = other.loaded;
        this.access = other.access;
        this.name = other.name;
        this.signature = other.signature;
        this.superClass = tree.getClassNode(other.superClass.name);

        this.interfaces.clear();
        other.interfaces.forEach((name, node) -> interfaces.put(name, tree.getClassNode(node.name)));

        this.innerClasses.clear();
        other.innerClasses.forEach((name, node) -> innerClasses.put(name, tree.getClassNode(node.name)));

        this.fields.clear();
        other.fields.forEach((name, node) -> fields.put(name, tree.getClassNode(node.owner.name).getField(name)));

        this.fields.clear();
        other.fields.forEach((name, node) -> {
            TreeFieldNode newNode = tree.getClassNode(node.owner.name).getField(node.name);
            newNode.copyFrom(node);
            fields.put(name, newNode);
        });

        this.methods.clear();
        other.methods.forEach((name, node) -> {
            TreeMethodNode newNode = tree.getClassNode(node.owner.name).getMethod(node.name, node.desc);
            newNode.copyFrom(node);
            methods.put(name, newNode);
        });
    }

    public Stream<TreeClassNode> getHierarchy() {
        if (!loaded) {
            return Stream.empty();
        }
        return Streams.concat(//
                Stream.of(this),//
                Streams.concat(Stream.of(superClass),//
                        interfaces.values().stream()//
                ).flatMap(TreeClassNode::getHierarchy)//
        ).distinct();
    }

    public TreeClassNode getInterface(String name) {
        return interfaces.computeIfAbsent(name, tree::getClassNode);
    }

    public TreeClassNode getInnerClass(String name) {
        return innerClasses.computeIfAbsent(name, tree::getClassNode);
    }

    public TreeFieldNode getField(String name) {
        return fields.computeIfAbsent(name, tree::createFieldNode);
    }

    public TreeMethodNode getMethod(String name, String desc) {
        return methods.computeIfAbsent(name + desc, e -> tree.createMethodNode(name, desc));
    }

    public ClassVisitor visitClass() {
        return new Visitor();
    }

    private class Visitor extends ClassVisitor {

        public Visitor() {
            super(Opcodes.ASM7);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            TreeClassNode.this.loaded = true;
            TreeClassNode.this.access = access;
            TreeClassNode.this.name = name;
            TreeClassNode.this.signature = signature;
            TreeClassNode.this.superClass = tree.getClassNode(superName);
            TreeClassNode.this.interfaces.clear();
            Arrays.stream(interfaces).map(tree::getClassNode).forEach(e -> TreeClassNode.this.interfaces.put(e.name, e));
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            outerClass = tree.getClassNode(owner);
            if (name != null && descriptor != null) {
                outerMethod = outerClass.getMethod(name, descriptor);
            }
        }

        //        @Override
        //        public void visitInnerClass(String name, String outerName, String innerName, int access) {
        //            getOrCreateInnerClass(name);
        //        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            return getField(name).visitField(TreeClassNode.this, access, name, descriptor, signature);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return getMethod(name, descriptor).visitMethod(TreeClassNode.this, access, name, descriptor, signature, exceptions);
        }
    }
}

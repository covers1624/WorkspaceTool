/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.util;

import java.util.*;

/**
 * Created by covers1624 on 18/11/19.
 */
public class AccessUsageFile {

    private final Map<String, UsageClass> usageClasses = new HashMap<>();

    public UsageClass getUsageClass(String name) {
        return usageClasses.computeIfAbsent(name, UsageClass::new);
    }

//    public AtFile generateAT(AtFile accessList) {
//        AtFile atFile = new AtFile();
//        usageClasses.values().parallelStream()//
//                .flatMap(e -> e.usageNodes.parallelStream())//
//                .collect(Collectors.toSet())//
//                .forEach(node -> {
//                    if (node instanceof TypeUsageNode) {
//                        TypeUsageNode typeNode = (TypeUsageNode) node;
//                        AtClass accessClass = accessList.getClass(typeNode.owner);
//                        if(accessClass.accessChange)
//                    } else if (node instanceof FieldUsageNode) {
//                        FieldUsageNode fieldNode = (FieldUsageNode) node;
//
//                    } else if (node instanceof MethodUsageNode) {
//                        MethodUsageNode methodNode = (MethodUsageNode) node;
//
//                    }
//                });
//        return atFile;
//    }

    public Map<String, UsageClass> getUsageClasses() {
        return usageClasses;
    }

    public static class UsageClass {

        public final String name;
        private final List<UsageNode> usageNodes = new ArrayList<>();

        public UsageClass(String name) {
            this.name = name;
        }

        public void addUsageNode(UsageNode node) {
            usageNodes.add(node);
        }

        public void addAllUsageNodes(Collection<UsageNode> nodes) {
            usageNodes.addAll(nodes);
        }

        public List<UsageNode> getUsageNodes() {
            return usageNodes;
        }
    }

    public static class UsageNode {

        public final String owner;

        public UsageNode(String owner) {
            this.owner = owner;
        }
    }

    public static class TypeUsageNode extends UsageNode {

        public TypeUsageNode(String owner) {
            super(owner);
        }

        @Override
        public int hashCode() {
            int i = 0;
            i = 31 * i + owner.hashCode();
            return i;
        }

        @Override
        public boolean equals(Object obj) {
            if (super.equals(obj)) {
                return true;
            }
            if (!(obj instanceof TypeUsageNode)) {
                return false;
            }
            TypeUsageNode other = (TypeUsageNode) obj;
            return other.owner.equals(owner);
        }
    }

    public static class FieldUsageNode extends UsageNode {

        public final String name;
        public final boolean isPut;

        public FieldUsageNode(String owner, String name, boolean isPut) {
            super(owner);
            this.name = name;
            this.isPut = isPut;
        }

        @Override
        public int hashCode() {
            int i = 0;
            i = 31 * i + owner.hashCode();
            i = 31 * i + name.hashCode();
            i = 31 * i + (isPut ? 1 : 0);
            return i;
        }

        @Override
        public boolean equals(Object obj) {
            if (super.equals(obj)) {
                return true;
            }
            if (!(obj instanceof FieldUsageNode)) {
                return false;
            }
            FieldUsageNode other = (FieldUsageNode) obj;
            return other.owner.equals(owner) && other.name.equals(name) && other.isPut == isPut;
        }
    }

    public static class MethodUsageNode extends UsageNode {

        public final String name;
        public final String desc;

        public MethodUsageNode(String owner, String name, String desc) {
            super(owner);
            this.name = name;
            this.desc = desc;
        }

        @Override
        public int hashCode() {
            int i = 0;
            i = 31 * i + owner.hashCode();
            i = 31 * i + name.hashCode();
            i = 31 * i + desc.hashCode();
            return i;
        }

        @Override
        public boolean equals(Object obj) {
            if (super.equals(obj)) {
                return true;
            }
            if (!(obj instanceof MethodUsageNode)) {
                return false;
            }
            MethodUsageNode other = (MethodUsageNode) obj;
            return other.owner.equals(owner) && other.name.equals(name) && other.desc.equals(desc);
        }

    }

}

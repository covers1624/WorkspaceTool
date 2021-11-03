/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util.tree;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.wt.util.Utils;
import net.covers1624.wt.util.tree.cache.CacheNode;
import net.covers1624.wt.util.tree.cache.ClassTreeCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static net.covers1624.quack.util.SneakyUtils.sneaky;

/**
 * Created by covers1624 on 3/17/20.
 */
@SuppressWarnings ("UnstableApiUsage")
public class ClassTree {

    private static final Logger logger = LogManager.getLogger("ClassTree");

    private static final HashFunction sha256 = Hashing.sha256();

    private final Map<String, TreeClassNode> classMap = new HashMap<>();
    private final Map<String, CacheNode> loadedClasses = new HashMap<>();
    private ClassTreeCache classCache = new ClassTreeCache.NullClassTreeCache();

    public void setCache(ClassTreeCache cache) {
        classCache = cache;
    }

    public void loadClasses(Path path) {
        sneaky(() -> Files.walk(path))
                .filter(Files::isRegularFile)
                .filter(e -> e.getFileName().toString().endsWith(".class"))
                .forEach(file -> {
                    //logger.info("Loading {}", file);
                    try (InputStream is = Files.newInputStream(file)) {
                        loadClass(is);
                    } catch (IOException e) {
                        logger.warn("Failed to load class from: {}", file, e);
                    }
                });
        classCache.save();
    }

    public void loadClass(InputStream is) {
        byte[] bytes = sneaky(() -> IOUtils.toBytes(is));
        ClassReader reader = new ClassReader(bytes);
        String className = reader.getClassName();
        HashCode hashCode = sha256.hashBytes(bytes);
        CacheNode cacheNode = classCache.getNode(className);
        CacheNode loadedNode = loadedClasses.get(className);
        if (loadedNode != null && !loadedNode.check(hashCode, bytes.length)) {
            logger.warn("Attempted to load Duplicate classes with differing hashes, Skipping.. Class: {}", className);
            return;
        }
        loadedClasses.put(className, cacheNode);
        if (cacheNode != null && cacheNode.check(hashCode, bytes.length)) {
            //If the cache matches, grab the cached node and rebuild hierarchy.
            TreeClassNode node = cacheNode.getNode();
            getClassNode(className).copyFrom(node);
            return;
        }
        TreeClassNode node = getClassNode(className);
        reader.accept(node.visitClass(), 0);
        classCache.update(node, hashCode, bytes.length);
    }

    public TreeClassNode getClassNode(String name) {
        return classMap.computeIfAbsent(name, e -> {
            TreeClassNode node = new TreeClassNode(this);
            node.name = name;
            return node;
        });
    }

    public Map<String, TreeClassNode> getClassMap() {
        return classMap;
    }

    public TreeFieldNode createFieldNode(String name) {
        TreeFieldNode node = new TreeFieldNode(this);
        node.name = name;
        return node;
    }

    public TreeMethodNode createMethodNode(String name, String desc) {
        TreeMethodNode node = new TreeMethodNode(this);
        node.name = name;
        node.desc = desc;
        return node;
    }

}

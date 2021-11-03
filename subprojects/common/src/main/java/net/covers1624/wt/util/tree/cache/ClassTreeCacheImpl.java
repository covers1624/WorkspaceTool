/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util.tree.cache;

import com.google.common.hash.HashCode;
import net.covers1624.wt.util.Utils;
import net.covers1624.wt.util.tree.TreeClassNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 3/22/20.
 */
@SuppressWarnings ("UnstableApiUsage")
public class ClassTreeCacheImpl implements ClassTreeCache, Serializable {

    private static final Logger logger = LogManager.getLogger("ClassTreeCache");

    private final Map<String, CacheNodeImpl> hashCache = new HashMap<>();
    private transient Path path;
    private transient boolean dirty;

    private ClassTreeCacheImpl() {
        this(null);
    }

    public ClassTreeCacheImpl(Path path) {
        this.path = path;
    }

    @Override
    public CacheNode getNode(String className) {
        return hashCache.get(className);
    }

    @Override
    public void update(TreeClassNode node, HashCode hash, int contentLength) {
        dirty = true;
        hashCache.put(node.name, new CacheNodeImpl(node, hash, contentLength));
    }

    @Override
    public void save() {
        if (dirty) {
            dirty = false;
            try (ObjectOutputStream os = new ObjectOutputStream(Files.newOutputStream(Utils.makeFile(path)))) {
                os.writeObject(this);
            } catch (IOException e) {
                logger.warn("Failed to save ClassTreeCache to file: {}", path, e);
            }
        }
    }

    public static ClassTreeCache from(Path path) {
        if (Files.exists(path)) {
            try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(path))) {
                ClassTreeCacheImpl cache = (ClassTreeCacheImpl) is.readObject();
                cache.path = path;
                return cache;
            } catch (IOException | ClassNotFoundException e) {
                logger.warn("Failed to load ClassTreeCache from path: {}", path, e);
            }
        }
        ClassTreeCacheImpl cache = new ClassTreeCacheImpl(path);
        cache.dirty = true;
        return cache;
    }

    private class CacheNodeImpl implements Serializable, CacheNode {

        public TreeClassNode node;

        public String className;
        public HashCode hashCode;
        public int contentLength;

        public CacheNodeImpl(TreeClassNode node, HashCode hashCode, int contentLength) {
            this.node = node;
            className = node.name;
            this.hashCode = hashCode;
            this.contentLength = contentLength;
        }

        @Override
        public TreeClassNode getNode() {
            return node;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public HashCode getHashCode() {
            return hashCode;
        }

        @Override
        public int getContentLength() {
            return contentLength;
        }

        @Override
        public boolean check(HashCode hashCode, int contentLength) {
            return this.hashCode.equals(hashCode) && this.contentLength == contentLength;
        }
    }
}

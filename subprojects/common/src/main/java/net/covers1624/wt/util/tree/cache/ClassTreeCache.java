/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util.tree.cache;

import com.google.common.hash.HashCode;
import net.covers1624.wt.util.tree.TreeClassNode;

/**
 * Created by covers1624 on 3/22/20.
 */
public interface ClassTreeCache {

    CacheNode getNode(String className);

    //    /**
    //     * Gets the TreeClassNode in the cache for the given class name.
    //     *
    //     * @param className The Class name.
    //     * @return The node, null if the entry doesnt exist.
    //     */
    //    TreeClassNode getNode(String className);
    //
    //    /**
    //     * Checks if the HashCode and contentLength cache for the given Class name is equal or not.
    //     *
    //     * @param className The ClassName.
    //     * @param hash      The HashCode.
    //     * @param contentLength The ContentLength.
    //     * @return True if the hash and contentLength match the cache, False if the cache does not match OR is not cached.
    //     */
    //    boolean check(String className, HashCode hash, int contentLength);
    //

    /**
     * Updates the HashCode and ContentLength properties in the cache for
     * the given TreeClassNode.
     *
     * @param node          The TreeClassNode's cache to update.
     * @param hash          The new HashCode.
     * @param contentLength The new ContentLength.
     */
    void update(TreeClassNode node, HashCode hash, int contentLength);

    void save();

    //@formatter:off
    class NullClassTreeCache implements ClassTreeCache {
        @Override public CacheNode getNode(String className) { return null; }
        @Override public void update(TreeClassNode node, HashCode hash, int contentLength) { }
        @Override public void save() { }
    }
    //@formatter:on
}

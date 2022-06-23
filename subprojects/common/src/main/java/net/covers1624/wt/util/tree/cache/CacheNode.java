/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util.tree.cache;

import com.google.common.hash.HashCode;
import net.covers1624.wt.util.tree.TreeClassNode;

/**
 * Created by covers1624 on 3/25/20.
 */
public interface CacheNode {

    /**
     * @return The TreeClassNode for this cache node.
     */
    TreeClassNode getNode();

    /**
     * @return The class name for this node.
     */
    String getClassName();

    /**
     * @return The HashCode of the class bytes this TreeClassNode was loaded from.
     */
    HashCode getHashCode();

    /**
     * @return The number of bytes this TreeClassNode was loaded from.
     */
    int getContentLength();

    boolean check(HashCode hashCode, int contentLength);

}

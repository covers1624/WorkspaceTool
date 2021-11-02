/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.mc.data;

import java.util.Map;

/**
 * Created by covers1624 on 5/02/19.
 */
public class AssetIndexJson {

    public Map<String, AssetObject> objects;
    public boolean virtual;

    public class AssetObject {

        public String hash;
        public int size;
    }
}

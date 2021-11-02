/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.remap;

import net.covers1624.wt.forge.gradle.data.FG2McpMappingData;
import net.covers1624.wt.forge.util.SrgReader;

/**
 * A simple Remapper that maps from SRG to MCP mappings.
 * Created by covers1624 on 10/01/19.
 */
public class SRGToMCPRemapper extends SimpleRemapper {

    public SRGToMCPRemapper(FG2McpMappingData data) {
        SrgReader.readSrg(data.srgToMcp, (type, args) -> {
            switch (type) {
                case FIELD:
                    fieldMap.put(args[1], args[3]);
                    break;
                case METHOD:
                    methodMap.put(args[1], args[4]);
                    break;
            }
        });
    }
}

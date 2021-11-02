/*
 * Copyright (c) 2018-2019 covers1624
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
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

package net.covers1624.wstool.gradle.api.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by covers1624 on 16/5/23.
 */
public class SourceSetDataList extends Data {

    public final Map<String, SourceSetData> sourceSets = new LinkedHashMap<>();
}

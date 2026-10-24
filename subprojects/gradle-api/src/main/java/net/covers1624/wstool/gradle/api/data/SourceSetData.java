package net.covers1624.wstool.gradle.api.data;

import java.io.File;
import java.util.*;

/**
 * Created by covers1624 on 16/5/23.
 */
public class SourceSetData extends Data {

    public final String name;

    public final String compileClasspathConfiguration;
    public final String runtimeClasspathConfiguration;
    public final String annotationProcessorClasspathConfiguration;

    public final Map<String, List<File>> sourceMap = new HashMap<>();

    public final Set<SourceSetAttribute> attributes = new LinkedHashSet<>();

    public SourceSetData(String name, String compileClasspathConfiguration, String runtimeClasspathConfiguration, String annotationProcessorClasspathConfiguration) {
        this.name = name;
        this.compileClasspathConfiguration = compileClasspathConfiguration;
        this.runtimeClasspathConfiguration = runtimeClasspathConfiguration;
        this.annotationProcessorClasspathConfiguration = annotationProcessorClasspathConfiguration;
    }
}

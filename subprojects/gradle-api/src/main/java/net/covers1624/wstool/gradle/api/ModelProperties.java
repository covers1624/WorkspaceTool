package net.covers1624.wstool.gradle.api;

import java.io.File;
import java.util.Set;

/**
 * Used as a parameter to the Gradle Model builder implementation.
 * <p>
 * Created by covers1624 on 15/5/23.
 */
public interface ModelProperties {

    /**
     * @return The file to write the built model to.
     */
    File getOutputFile();

    void setOutputFile(File file);

    /**
     * @return DataBuilders to extract data from the Project.
     */
    Set<String> getDataBuilders();

    void setDataBuilders(Set<String> builders);
}

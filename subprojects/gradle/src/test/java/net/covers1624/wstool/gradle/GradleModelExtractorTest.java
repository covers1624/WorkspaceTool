package net.covers1624.wstool.gradle;

import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.wstool.gradle.api.GradleTestBase;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by covers1624 on 19/5/23.
 */
public class GradleModelExtractorTest extends GradleTestBase {

    @Test
    public void testGetJavaVersionForGradle() {
        assertEquals(JavaVersion.JAVA_1_8, GradleModelExtractor.getJavaVersionForGradle(GradleVersion.version("4.0")));
        assertEquals(JavaVersion.JAVA_16, GradleModelExtractor.getJavaVersionForGradle(GradleVersion.version("7.0")));
        assertEquals(JavaVersion.JAVA_16, GradleModelExtractor.getJavaVersionForGradle(GradleVersion.version("7.2")));
        assertEquals(JavaVersion.JAVA_17, GradleModelExtractor.getJavaVersionForGradle(GradleVersion.version("7.3")));
        assertEquals(JavaVersion.JAVA_17, GradleModelExtractor.getJavaVersionForGradle(GradleVersion.version("9.0")));
    }
}

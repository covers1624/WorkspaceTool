/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.covers1624.wt.util.pattern;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

/*
 * This class belongs to Gradle.
 * If you wish to retrieve a copy of the original code please see here:
 * https://github.com/gradle/gradle/tree/a138710ab109e271b17ee48dc49b359284cd3f66/subprojects/core-api/src/main/java/org/gradle/api/internal/file/pattern
 *
 * The following changes have been made:
 * - Gradle specific references have been stripped in 'PatternMatcherFactory'
 * - Code has had a formatter applied.
 */
public class PatternMatcherFactory {

    private static final EndOfPathMatcher END_OF_PATH_MATCHER = new EndOfPathMatcher();
    private static final String PATH_SEPARATORS = "\\/";

    //Modifications:
    // - Migrated from Spec to Predicate.
    // - Migrated from RelativePath to Path
    // - Use changed class name bellow.
    public static Predicate<Path> getPatternMatcher(boolean partialMatchDirs, boolean caseSensitive, String pattern) {
        PathMatcher pathMatcher = compile(caseSensitive, pattern);
        return new PathMatcherBackedPredicate(partialMatchDirs, pathMatcher);
    }

    public static PathMatcher compile(boolean caseSensitive, String pattern) {
        if (pattern.length() == 0) {
            return END_OF_PATH_MATCHER;
        }

        // trailing / or \ assumes **
        if (pattern.endsWith("/") || pattern.endsWith("\\")) {
            pattern = pattern + "**";
        }
        String[] parts = StringUtils.split(pattern, PATH_SEPARATORS);
        return compile(parts, 0, caseSensitive);
    }

    private static PathMatcher compile(String[] parts, int startIndex, boolean caseSensitive) {
        if (startIndex >= parts.length) {
            return END_OF_PATH_MATCHER;
        }
        int pos = startIndex;
        while (pos < parts.length && parts[pos].equals("**")) {
            pos++;
        }
        if (pos > startIndex) {
            if (pos == parts.length) {
                return new AnythingMatcher();
            }
            return new GreedyPathMatcher(compile(parts, pos, caseSensitive));
        }
        return new FixedStepPathMatcher(PatternStepFactory.getStep(parts[pos], caseSensitive), compile(parts, pos + 1, caseSensitive));
    }

    //Modifications:
    // - Migrated from RelativePath to java.nio.Path
    // - Migrated from Spec to Predicate
    // - Renamed from 'PathMatcherBackedSpec' to 'PathMatcherBackedPredicate'
    static class PathMatcherBackedPredicate implements Predicate<Path> {

        private final boolean partialMatchDirs;
        private final PathMatcher pathMatcher;

        PathMatcherBackedPredicate(boolean partialMatchDirs, PathMatcher pathMatcher) {
            this.partialMatchDirs = partialMatchDirs;
            this.pathMatcher = pathMatcher;
        }

        PathMatcher getPathMatcher() {
            return pathMatcher;
        }

        //Modifications:
        // - Renamed from isSatisfied to test as per Predicate migration.
        // - Renamed renamed parameter from element to path.
        // - Use Files.isRegularFile instead of RelativePath.isFile
        // - Generate segments at top of method.
        @Override
        public boolean test(Path path) {
            String[] segments = StringUtils.split(path.toString(), PATH_SEPARATORS);
            if (Files.isRegularFile(path) || !partialMatchDirs) {
                return pathMatcher.matches(segments, 0);
            } else {
                return pathMatcher.isPrefix(segments, 0);
            }
        }
    }
}

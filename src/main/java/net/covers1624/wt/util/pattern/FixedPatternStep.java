/*
 * Copyright 2014 the original author or authors.
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

/*
 * This class belongs to Gradle.
 * If you wish to retrieve a copy of the original code please see here:
 * https://github.com/gradle/gradle/tree/a138710ab109e271b17ee48dc49b359284cd3f66/subprojects/core-api/src/main/java/org/gradle/api/internal/file/pattern
 *
 * The following changes have been made:
 * - Gradle specific references have been stripped in 'PatternMatcherFactory'
 * - Code has had a formatter applied.
 */

/**
 * A pattern step for a fixed pattern segment that does not contain any wildcards.
 */
public class FixedPatternStep implements PatternStep {

    private final String value;
    private final boolean caseSensitive;

    public FixedPatternStep(String value, boolean caseSensitive) {
        this.value = value;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public String toString() {
        return "{match: " + value + "}";
    }

    @Override
    public boolean matches(String candidate) {
        return caseSensitive ? candidate.equals(value) : candidate.equalsIgnoreCase(value);
    }
}

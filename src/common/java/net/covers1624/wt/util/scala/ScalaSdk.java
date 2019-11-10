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

package net.covers1624.wt.util.scala;

import com.google.common.collect.Iterables;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.util.MavenNotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Basic representation of a ScalaSDK.
 *
 * Created by covers1624 on 30/01/19.
 */
@Deprecated
public class ScalaSdk {

    private ScalaVersion scalaVersion;
    private String version;
    private String sdkName;

    private MavenDependency scalac;
    private List<MavenDependency> libraries = new ArrayList<>();

    public void consume(Module host) {
        ScalaVersion scalaVersion;
        String version;
        String sdkName;
        MavenDependency scalac = null;
        List<MavenDependency> libraries = new ArrayList<>();

        Iterable<Dependency> dependencies = host.getSourceSets().get("main")//
                .getCompileConfiguration()//
                .getAllDependencies();
        Iterator<Dependency> itr = dependencies.iterator();
        while (itr.hasNext()) {
            Dependency dep = itr.next();
            if (dep instanceof MavenDependency) {
                MavenDependency mavenDep = (MavenDependency) dep;
                MavenNotation notation = mavenDep.getNotation();
                if (notation.group.startsWith("org.scala-lang")) {
                    itr.remove();
                    if (!notation.module.equals("scala-compiler")) {
                        libraries.add(mavenDep);
                    } else {
                        scalac = mavenDep;
                    }
                }
            }
        }
        if (scalac != null) {
            version = scalac.getNotation().version;
            sdkName = "scala-sdk-" + version;
            scalaVersion = ScalaVersion.findByVersion(version)//
                    .orElseThrow(() -> new RuntimeException("Unknown scala version: " + version));
            if (this.scalaVersion == null || scalaVersion.ordinal() > this.scalaVersion.ordinal()) {
                this.scalaVersion = scalaVersion;
                this.version = version;
                this.sdkName = sdkName;
                this.libraries = libraries;
                this.scalac = scalac;
            }
        }
    }

    public ScalaVersion getScalaVersion() {
        return scalaVersion;
    }

    public String getVersion() {
        return version;
    }

    public String getSdkName() {
        return sdkName;
    }

    public MavenDependency getScalac() {
        return scalac;
    }

    public List<MavenDependency> getLibraries() {
        return libraries;
    }

    public Iterable<MavenDependency> getClasspath() {
        return Iterables.concat(Collections.singleton(scalac), libraries);
    }
}

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

package net.covers1624.wt;

/**
 * Created by covers1624 on 30/01/19.
 */
public class TestDependencyResolver {

    //    @Test
    //    public void doTest() {
    //        WorkspaceContext context = new WorkspaceContext(null);
    //        context.workspaceJson = new WorkspaceJson();
    //        context.workspaceJson.dep_overrides.put("mezz.jei:jei_1.12:", "mezz.jei:jei_1.12.2:");
    //        DependencyResolver depResolver = new DependencyResolver(context);
    //        GradleInfoJson info = new GradleInfoJson();
    //        info.dependencies.add(makeDep("net.covers1624:TestDep:1.0.0"));
    //        info.dependencies.add(makeDep("net.covers1624:TestDep:1.1.0"));
    //        info.dependencies.add(makeDep("net.covers1624:TestDep:4.0.0"));
    //        info.dependencies.add(makeDep("net.covers1624:TestDep:2.1.0"));
    //        info.dependencies.add(makeDep("net.covers1624:TestDep:1.12.2-3.4.0.111"));
    //        info.dependencies.add(makeDep("mezz.jei:jei_1.12.2:2.0.0"));
    //        info.dependencies.add(makeDep("mezz.jei:jei_1.12:1.0.0"));
    //        depResolver.consume(info);
    //
    //        Assert.assertEquals("net.covers1624:TestDep:4.0.0", depResolver.resolve("net.covers1624:TestDep:1.1.0").getMavenDep());
    //        Assert.assertEquals("mezz.jei:jei_1.12.2:2.0.0", depResolver.resolve("mezz.jei:jei_1.12:1.0.0").getMavenDep());
    //    }
    //
    //    private static DependencyJson makeDep(String mavenDep) {
    //        DependencyJson json = new DependencyJson();
    //        json.mavenDep = mavenDep;
    //        return json;
    //    }

}

//import net.covers1624.wt.api.impl.module.ModuleImpl
//import net.covers1624.wt.api.impl.module.SourceSetImpl
//import net.covers1624.wt.api.module.Module
//import net.covers1624.wt.intellij.util.ContentRootMerger
//import org.junit.Test
//
//import java.nio.file.Path
//import java.nio.file.Paths
//
//import static org.hamcrest.CoreMatchers.hasItems
//import static org.junit.Assert.assertEquals
//import static org.junit.Assert.assertThat
//
///**
// * Created by covers1624 on 16/8/19.
// */
//class TestContentRootMerger {
//
//    @Test
//    void doTest() {
//        Path moduleRoot = Paths.get('Forge').normalize().toAbsolutePath()
//        Module forgeClean = new ModuleImpl("Forge.clean", '', moduleRoot.resolve('projects/clean')).with {
//            addSourceSet('main', new SourceSetImpl('main').with {
//                addSource('java', moduleRoot.resolve('projects/clean/src/main/java'))
//                it
//            })
//            addSourceSet('test', new SourceSetImpl('test').with {
//                addSource('java', moduleRoot.resolve('projects/clean/src/test'))
//                it
//            })
//            it
//        }
//
//        Module forgeSub = new ModuleImpl('Forge.forge', '', moduleRoot.resolve('projects/forge'))
//        def fmllauncherSS = new SourceSetImpl('fmllauncher').with {
//            addSource('java', moduleRoot.resolve('src/fmllauncher/java'))
//            addResource(moduleRoot.resolve('src/fmllauncher/resources'))
//            it
//        }
//        def mainSS = new SourceSetImpl('main').with {
//            addSource('java', moduleRoot.resolve('projects/forge/src/main/java'))
//            addSource('java', moduleRoot.resolve('src/main/java'))
//            addResource(moduleRoot.resolve('src/main/resources'))
//            addResource(moduleRoot.resolve('src/generated/resources'))
//            it
//        }
//        def testSS = new SourceSetImpl('test').with {
//            addSource('java', moduleRoot.resolve('src/fmllaunchertest/java'))
//            addSource('java', moduleRoot.resolve('src/test/java'))
//            addResource(moduleRoot.resolve('src/test/resources'))
//            it
//        }
//        def userdevSS = new SourceSetImpl('userdev').with {
//            addSource('java', moduleRoot.resolve('src/userdev/java'))
//            addResource(moduleRoot.resolve('src/userdev/resources'))
//            it
//        }
//        forgeSub.addSourceSet('fmllauncher', fmllauncherSS)
//        forgeSub.addSourceSet('main', mainSS)
//        forgeSub.addSourceSet('test', testSS)
//        forgeSub.addSourceSet('userdev', userdevSS)
//
//        def mergeResults = ContentRootMerger.mergeContentRoots([forgeClean, forgeSub])
//
//    }
//
//
//}

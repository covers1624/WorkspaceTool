import net.covers1624.wt.util.Utils
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Created by covers1624 on 16/8/19.
 */
class TestUtils {


    @Test
    void testIsAncestor() {
        Path root = Paths.get("SomeFolder").normalize().toAbsolutePath()
        assertTrue(Utils.isAncestor(root.resolve("doot"), root, true))
        assertFalse(Utils.isAncestor(root, root.resolve("doot"), true))
    }

}

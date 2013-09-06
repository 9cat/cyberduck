package ch.cyberduck.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class PathNormalizerTest extends AbstractTestCase {

    @Test
    public void testNormalize() throws Exception {
        assertEquals(PathNormalizer.normalize("relative/path", false), "relative/path");
        assertEquals(PathNormalizer.normalize("/absolute/path", true), "/absolute/path");
        assertEquals(PathNormalizer.normalize("/absolute/path", false), "/absolute/path");
    }

    @Test
    public void test972() throws Exception {
        assertEquals("//home/path", PathNormalizer.normalize("//home/path"));
    }

    @Test
    public void testName() throws Exception {
        assertEquals("p", PathNormalizer.name("/p"));
        assertEquals("n", PathNormalizer.name("/p/n"));
        assertEquals("p", PathNormalizer.name("p"));
        assertEquals("n", PathNormalizer.name("p/n"));
    }
}

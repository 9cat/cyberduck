package ch.cyberduck.core.local;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Local;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class LocalTest extends AbstractTestCase {

    @Test
    public void testList() throws Exception {
        assertFalse(new Local("profiles") {

        }.list().isEmpty());
    }

    private final class TestLocal extends Local {
        private TestLocal(final String name) {
            super(name);
        }
    }

    @Test
    public void testEqual() throws Exception {
        assertEquals(new TestLocal("/p/1"), new TestLocal("/p/1"));
        assertNotEquals(new TestLocal("/p/1"), new TestLocal("/p/2"));
        assertEquals(new TestLocal("/p/1"), new TestLocal("/P/1"));
    }

    @Test
    public void testHashCode() throws Exception {
        assertEquals(new TestLocal("/p/1").hashCode(), new TestLocal("/P/1").hashCode());
    }

    @Test
    public void testAttributes() throws Exception {
        final TestLocal l = new TestLocal("/p/1");
        assertNotNull(l.attributes());
        assertSame(l.attributes(), l.attributes());
    }
}

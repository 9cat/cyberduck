package ch.cyberduck.core.transfer;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Preferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class SpeedometerTest extends AbstractTestCase {

    @Before
    public void preference() {
        Preferences.instance().setProperty("browser.filesize.decimal", true);
    }

    @Test
    public void testProgressRemaining() throws Exception {
        final long start = System.currentTimeMillis();
        Speedometer m = new Speedometer(start);
        assertEquals("1 B of 5 B (20%, 1 B/sec, 4 seconds remaining)", m.getProgress(start + 1000L, true, 5L, 1L));
        assertEquals("4 B of 5 B (80%, 2 B/sec, 1 seconds remaining)", m.getProgress(start + 3000L, true, 5L, 4L));
        assertEquals("4 B of 5 B (80%, 0 B/sec)", m.getProgress(start + 4000L, true, 5L, 4L));
    }

    @Test
    public void testStopped() throws Exception {
        final long start = System.currentTimeMillis();
        Speedometer m = new Speedometer(start);
        assertEquals("0 B of 1.0 MB", m.getProgress(true, 1000000L, 0L));
        assertEquals("500.0 KB of 1.0 MB", m.getProgress(false, 1000000L, 1000000L / 2));
    }

    @Test
    public void testProgressRemaining2() throws Exception {
        final long start = System.currentTimeMillis();
        Speedometer m = new Speedometer(start);
        assertEquals("500.0 KB (500,000 bytes) of 1.0 MB (50%, 500.0 KB/sec, 1 seconds remaining)",
                m.getProgress(start + 1000L, true, 1000000L, 1000000L / 2));
        assertEquals("900.0 KB (900,000 bytes) of 1.0 MB (90%, 400.0 KB/sec, 1 seconds remaining)",
                m.getProgress(start + 2000L, true, 1000000L, 900000L));
    }

    @Test
    public void testProgressRemaining3() throws Exception {
        final long start = System.currentTimeMillis();
        Speedometer m = new Speedometer(start);
        assertEquals("900.0 KB (900,000 bytes) of 1.0 MB (90%, 450.0 KB/sec, 1 seconds remaining)",
                m.getProgress(start + 2000L, true, 1000000L, 900000L));
    }
}
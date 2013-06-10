package ch.cyberduck.core.local;

import ch.cyberduck.core.AbstractTestCase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id:$
 */
public class WorkspaceBrowserLauncherTest extends AbstractTestCase {

    @BeforeClass
    public static void register() {
        WorkspaceBrowserLauncher.register();
    }

    @Test
    public void testOpen() throws Exception {
        assertTrue(new WorkspaceBrowserLauncher().open("http://cyberduck.ch"));
        assertFalse(new WorkspaceBrowserLauncher().open(""));
        assertFalse(new WorkspaceBrowserLauncher().open(null));
    }
}

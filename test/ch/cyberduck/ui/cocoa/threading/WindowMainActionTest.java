package ch.cyberduck.ui.cocoa.threading;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.ui.cocoa.WindowController;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @version $Id:$
 */
public class WindowMainActionTest extends AbstractTestCase {

    @Test
    public void testIsValid() throws Exception {
        assertFalse(new WindowMainAction(new WindowController() {
            @Override
            protected String getBundleName() {
                return null;
            }
        }) {

            @Override
            public void run() {
                //
            }
        }.isValid());
    }
}

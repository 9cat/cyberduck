package ch.cyberduck.ui.growl;

import ch.cyberduck.core.AbstractTestCase;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @version $Id$
 */
public class NotificationCenterTest extends AbstractTestCase {

    @Test
    @Ignore
    public void testNotify() throws Exception {
        final Growl growl = new NotificationCenter();
        growl.notify("title", "test");
    }
}
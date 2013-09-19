package ch.cyberduck.core.importer;

import ch.cyberduck.core.AbstractHostCollection;
import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.local.FinderLocal;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id$
 */
public class ThirdpartyBookmarkCollectionTest extends AbstractTestCase {

    @Test
    public void testLoad() throws Exception {
        final FinderLocal source = new FinderLocal(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        source.touch();
        IOUtils.write(RandomStringUtils.random(1000), source.getOutputStream(false));
        final AtomicBoolean r = new AtomicBoolean();
        final ThirdpartyBookmarkCollection c = new ThirdpartyBookmarkCollection() {
            @Override
            public Local getFile() {
                return source;
            }

            @Override
            protected void parse(final Local file) {
                this.add(new Host("h"));
                r.set(true);
            }

            @Override
            public String getBundleIdentifier() {
                return "t";
            }
        };
        c.load();
        assertTrue(r.get());
        assertTrue(c.iterator().next().compareTo(new Host("h")) == 0);
        r.set(false);
        Preferences.instance().setProperty(c.getConfiguration(), true);
        c.load();
        assertFalse(r.get());
        // Modify bookmarks file
        IOUtils.write(RandomStringUtils.random(1), source.getOutputStream(true));
        c.load();
        assertTrue(r.get());
        AbstractHostCollection bookmarks = new AbstractHostCollection() {
            @Override
            public String getName() {
                return "b";
            }
        };
        bookmarks.add(new Host("h"));
        c.filter(bookmarks);
        assertTrue(c.isEmpty());
    }
}

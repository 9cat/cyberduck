package ch.cyberduck.core.transfer.download;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.NullLocal;
import ch.cyberduck.core.NullSession;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.UserDateFormatterFactory;
import ch.cyberduck.core.local.WorkspaceApplicationLauncher;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.symlink.NullSymlinkResolver;
import ch.cyberduck.ui.cocoa.UserDefaultsDateFormatter;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class RenameExistingFilterTest extends AbstractTestCase {

    @BeforeClass
    public static void register() {
        UserDefaultsDateFormatter.register();
        WorkspaceApplicationLauncher.register();
    }

    @Test
    public void testPrepare() throws Exception {
        RenameExistingFilter f = new RenameExistingFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        final NullLocal local = new NullLocal("t") {
            @Override
            public boolean exists() {
                return false;
            }

            @Override
            public void rename(final Local renamed) {
                fail();
            }
        };
        final Path p = new Path("t", EnumSet.of(Path.Type.file));
        final TransferStatus status = f.prepare(p, local, new TransferStatus());
        assertNull(status.getRename().local);
        f.apply(p, local, new TransferStatus());
    }

    @Test
    public void testPrepareRename() throws Exception {
        final AtomicBoolean r = new AtomicBoolean();
        RenameExistingFilter f = new RenameExistingFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        final NullLocal local = new NullLocal(System.getProperty("java.io.tmpdir"), "t") {
            @Override
            public boolean exists() {
                return "t".equals(this.getName());
            }

            @Override
            public void rename(final Local renamed) {
                assertEquals(String.format("t (%s)", UserDateFormatterFactory.get().getLongFormat(System.currentTimeMillis(), false)), renamed.getName());
                r.set(true);
            }
        };
        final Path p = new Path("t", EnumSet.of(Path.Type.file));
        final TransferStatus status = f.prepare(p, local, new TransferStatus());
        assertNotNull(status.getRename().local);
        assertFalse(r.get());
        f.apply(p, local, status);
        assertEquals("t", local.getName());
        assertEquals("t", local.getName());
        assertNotEquals("t", status.getRename().local.getName());
        assertTrue(r.get());
    }
}
package ch.cyberduck.core.transfer.download;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LocalAttributes;
import ch.cyberduck.core.NullLocal;
import ch.cyberduck.core.NullSession;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Permission;
import ch.cyberduck.core.local.WorkspaceApplicationLauncher;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.symlink.NullSymlinkResolver;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class OverwriteFilterTest extends AbstractTestCase {

    @BeforeClass
    public static void register() {
        WorkspaceApplicationLauncher.register();
    }

    @Test
    public void testAccept() throws Exception {
        OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        final Path p = new Path("a", EnumSet.of(Path.Type.file));
        p.attributes().setSize(8L);
        assertTrue(f.accept(p, new NullLocal("a"), new TransferStatus()));
    }

    @Test
    public void testAcceptDirectory() throws Exception {
        OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        assertTrue(f.accept(new Path("a", EnumSet.of(Path.Type.directory)), new NullLocal("/", "a") {
            @Override
            public boolean exists() {
                return false;
            }
        }, new TransferStatus()));
        assertTrue(f.accept(new Path("a", EnumSet.of(Path.Type.directory)), new NullLocal("/", "a") {
            @Override
            public boolean exists() {
                return true;
            }
        }, new TransferStatus()));
    }

    @Test
    public void testPrepare() throws Exception {
        OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        final Path p = new Path("a", EnumSet.of(Path.Type.file));
        p.attributes().setSize(8L);
        final TransferStatus status = f.prepare(p, new NullLocal("a"), new TransferStatus());
        assertEquals(8L, status.getLength(), 0L);
    }

    @Test
    public void testPrepareAttributes() throws Exception {
        OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        final Path p = new Path("a", EnumSet.of(Path.Type.file));
        p.attributes().setSize(8L);
        p.attributes().setModificationDate(1L);
        p.attributes().setPermission(new Permission(777));
        final TransferStatus status = f.prepare(p, new NullLocal("a") {
            @Override
            public LocalAttributes attributes() {
                return new LocalAttributes("a") {
                    @Override
                    public void setModificationDate(long timestamp) {
                        fail();
                    }

                    @Override
                    public void setPermission(Permission permission) {
                        fail();
                    }
                };
            }
        }, new TransferStatus());
        assertEquals(8L, status.getLength(), 0L);
        assertEquals(1L, status.getTimestamp(), 0L);
        assertEquals(new Permission(777), status.getPermission());
    }
}

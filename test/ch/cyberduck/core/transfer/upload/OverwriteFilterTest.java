package ch.cyberduck.core.transfer.upload;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Acl;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LocalAttributes;
import ch.cyberduck.core.NullLocal;
import ch.cyberduck.core.NullSession;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Permission;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.openstack.SwiftSession;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.symlink.NullSymlinkResolver;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class OverwriteFilterTest extends AbstractTestCase {

    @Test(expected = NotfoundException.class)
    public void testAcceptNotFoundFile() throws Exception {
        final OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        // Local file does not exist
        assertFalse(f.accept(new Path("a", EnumSet.of(Path.Type.file)) {
                             }, new NullLocal("t") {
                                 @Override
                                 public boolean exists() {
                                     return false;
                                 }
                             }, new TransferStatus()
        ));
    }

    @Test(expected = NotfoundException.class)
    public void testAcceptNotFoundDirectory() throws Exception {
        final OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        // Local file does not exist
        assertFalse(f.accept(new Path("a", EnumSet.of(Path.Type.directory)) {
                             }, new NullLocal("t") {
                                 @Override
                                 public boolean exists() {
                                     return false;
                                 }
                             }, new TransferStatus()
        ));
    }

    @Test
    public void testAcceptRemoteExists() throws Exception {
        final OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        assertTrue(f.accept(new Path("a", EnumSet.of(Path.Type.directory)) {
        }, new NullLocal("t"), new TransferStatus()));
        assertTrue(f.accept(new Path("a", EnumSet.of(Path.Type.directory)) {
        }, new NullLocal("t"), new TransferStatus()
        ));
    }

    @Test
    public void testSize() throws Exception {
        final OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        assertEquals(1L, f.prepare(new Path("/t", EnumSet.of(Path.Type.file)) {
                                   }, new NullLocal("/t") {
                                       @Override
                                       public LocalAttributes attributes() {
                                           return new LocalAttributes("/t") {
                                               @Override
                                               public long getSize() {
                                                   return 1L;
                                               }

                                           };
                                       }

                                       @Override
                                       public boolean isFile() {
                                           return true;
                                       }
                                   }, new TransferStatus()
        ).getLength(), 0L);
    }

    @Test
    public void testPermissionsNoChange() throws Exception {
        final OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        final Path file = new Path("/t", EnumSet.of(Path.Type.file));
        assertFalse(f.prepare(file, new NullLocal("t"), new TransferStatus()).isComplete());
        assertEquals(Acl.EMPTY, file.attributes().getAcl());
        assertEquals(Permission.EMPTY, file.attributes().getPermission());
    }

    @Test
    public void testPermissionsExistsNoChange() throws Exception {
        final OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        final Path file = new Path("/t", EnumSet.of(Path.Type.file));
        assertFalse(f.prepare(file, new NullLocal("/t"), new TransferStatus()).isComplete());
        assertEquals(Acl.EMPTY, file.attributes().getAcl());
        assertEquals(Permission.EMPTY, file.attributes().getPermission());
    }

    @Test
    public void testTemporary() throws Exception {
        final OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new NullSession(new Host("h")),
                new UploadFilterOptions().withTemporary(true));
        final Path file = new Path("/t", EnumSet.of(Path.Type.file));
        final TransferStatus status = f.prepare(file, new NullLocal("t"), new TransferStatus());
        assertFalse(f.temporary.isEmpty());
        assertNotNull(status.getRename());
        assertTrue(status.isRename());
        assertNotEquals(file, status.getRename());
        assertNull(status.getRename().local);
        assertNotNull(status.getRename().remote);
    }

    @Test
    public void testTemporaryDisabledLageUpload() throws Exception {
        final OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new SwiftSession(new Host("h")),
                new UploadFilterOptions().withTemporary(true));
        final Path file = new Path("/t", EnumSet.of(Path.Type.file));
        final TransferStatus status = f.prepare(file, new NullLocal("t"), new TransferStatus());
        assertTrue(f.temporary.isEmpty());
        assertNull(status.getRename().remote);
    }

    @Test
    public void testTemporaryDisabledMultipartUpload() throws Exception {
        final OverwriteFilter f = new OverwriteFilter(new NullSymlinkResolver(), new S3Session(new Host("h")),
                new UploadFilterOptions().withTemporary(true));
        final Path file = new Path("/t", EnumSet.of(Path.Type.file));
        final TransferStatus status = f.prepare(file, new NullLocal("t"), new TransferStatus());
        assertTrue(f.temporary.isEmpty());
        assertNull(status.getRename().local);
        assertNull(status.getRename().remote);
    }
}
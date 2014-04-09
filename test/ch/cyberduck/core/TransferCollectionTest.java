package ch.cyberduck.core;

import ch.cyberduck.core.local.FinderLocal;
import ch.cyberduck.core.transfer.CopyTransfer;
import ch.cyberduck.core.transfer.Transfer;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class TransferCollectionTest extends AbstractTestCase {

    @Test
    public void testEmpty() throws Exception {
        TransferCollection c = new TransferCollection(new FinderLocal("test/ch/cyberduck/core/transfer/TransferCollectionEmpty.plist")) {
            @Override
            public void trash() {
                //
            }
        };
        c.clear();
        assertEquals(0, c.size());
        c.load();
        assertEquals(0, c.size());
    }

    @Test
    public void testLoadCopyDeprecated() throws Exception {
        TransferCollection c = new TransferCollection(new FinderLocal("test/ch/cyberduck/core/transfer/TransferCollectionCopyFormatDeprecated.plist")) {
            @Override
            public void trash() {
                //
            }
        };
        c.clear();
        assertEquals(0, c.size());
        c.load();
        assertEquals(0, c.size());
    }

    @Test
    public void testLoadCopyInvalidCopyTransfer() throws Exception {
        TransferCollection c = new TransferCollection(new FinderLocal("test/ch/cyberduck/core/transfer/TransferCollectionCopyFormatInvalid.plist")) {
            @Override
            public void trash() {
                //
            }
        };
        c.clear();
        assertEquals(0, c.size());
        c.load();
        assertEquals(0, c.size());
    }

    @Test
    public void testSaveDeprecated() throws Exception {
        TransferCollection c = new TransferCollection(new FinderLocal("test/ch/cyberduck/core/transfer/TransferCollectionCopyFormatDeprecated.plist")) {
            @Override
            public void trash() {
                //
            }
        };
        c.clear();
        assertEquals(0, c.size());
        c.load();
        assertEquals(0, c.size());
    }

    @Test
    public void testLoadCopyWithDestination() throws Exception {
        TransferCollection c = new TransferCollection(new FinderLocal("test/ch/cyberduck/core/transfer/TransferCollectionCopyFormat.plist")) {
            @Override
            public void trash() {
                //
            }
        };
        c.clear();
        assertEquals(0, c.size());
        c.load();
        assertEquals(1, c.size());
        assertNotNull(c.get(0));
        assertEquals(1, (c.get(0).getRoots().size()));
        final Transfer transfer = c.get(0);
        assertTrue(transfer instanceof CopyTransfer);
        assertEquals("/pub/hacks/listings/1301-130.zip", transfer.getRoot().remote.getAbsolute());
        assertNull(transfer.getLocal());
        assertNotNull(transfer.getRoot().remote);
        assertNull(transfer.getRoot().local);
        assertEquals("ftp://ftp.heise.de/pub/hacks/listings/1301-130.zip", transfer.getRemote());
        assertEquals(109648L, transfer.getSize());
        assertEquals(109648L, transfer.getTransferred());
        assertEquals("ftp.heise.de", transfer.getHost().getHostname());
        assertEquals("sudo.ch", ((CopyTransfer) transfer).getDestination().getHost().getHostname());
        assertEquals("1301-130.zip", transfer.getName());
    }
}

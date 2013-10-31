package ch.cyberduck.core.transfer.upload;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.NullLocal;
import ch.cyberduck.core.NullSession;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Attributes;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.symlink.NullSymlinkResolver;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class RenameFilterTest extends AbstractTestCase {

    @Test
    public void testPrepare() throws Exception {
        RenameFilter f = new RenameFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        final Path t = new Path("t", Path.FILE_TYPE);
        t.setLocal(new NullLocal(null, "t"));
        f.prepare(t, new TransferStatus());
        assertNotSame("t", t.getName());
    }

    @Test
    public void testDirectoryUpload() throws Exception {
        final Path file = new Path("/t", Path.DIRECTORY_TYPE);
        file.setLocal(new NullLocal(null, "a"));
        final AtomicBoolean found = new AtomicBoolean();
        final AtomicBoolean moved = new AtomicBoolean();
        final NullSession session = new NullSession(new Host("h")) {
            @Override
            public <T> T getFeature(final Class<T> type) {
                if(type.equals(Find.class)) {
                    return (T) new Find() {
                        @Override
                        public boolean find(final Path f) throws BackgroundException {
                            if(f.equals(file)) {
                                found.set(true);
                                return true;
                            }
                            return false;
                        }
                    };
                }
                if(type.equals(Attributes.class)) {
                    return (T) new Attributes() {
                        @Override
                        public PathAttributes find(final Path file) throws BackgroundException {
                            return new PathAttributes(Path.FILE_TYPE);
                        }
                    };
                }
                return null;
            }
        };
        final RenameFilter f = new RenameFilter(new NullSymlinkResolver(), session);
        final TransferStatus status = f.prepare(file, new TransferStatus().exists(true));
        assertTrue(found.get());
        assertNotNull(status.getRenamed());
        assertNotNull(status.getRenamed().getLocal());
    }
}
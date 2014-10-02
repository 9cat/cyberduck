package ch.cyberduck.core.s3;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledProgressListener;
import ch.cyberduck.core.DisabledTranscriptListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginConnectionService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.io.DisabledStreamListener;
import ch.cyberduck.core.local.FinderLocal;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class S3ThresholdUploadServiceTest extends AbstractTestCase {

    @Test
    @Ignore
    public void testInteroperabilityEvault() throws Exception {
        final Host host = new Host(new S3Protocol(), "s3.lts2.evault.com", new Credentials(
                properties.getProperty("evault.s3.key"), properties.getProperty("evault.s3.secret")
        ));
        final S3Session session = new S3Session(host);
        new LoginConnectionService(new DisabledLoginController(), new DisabledHostKeyCallback(),
                new DisabledPasswordStore(), new DisabledProgressListener(), new DisabledTranscriptListener()).connect(session, Cache.<Path>empty());

        final S3ThresholdUploadService m = new S3ThresholdUploadService(session, 1L);
        final Path container = new Path("cyberduck", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final String name = UUID.randomUUID().toString() + ".txt";
        final Path test = new Path(container, name, EnumSet.of(Path.Type.file));
        final FinderLocal local = new FinderLocal(System.getProperty("java.io.tmpdir"), name);
        final String random = RandomStringUtils.random(1000);
        IOUtils.write(random, local.getOutputStream(false));
        final TransferStatus status = new TransferStatus();
        status.setLength((long) random.getBytes().length);
        status.setMime("text/plain");
        m.upload(test, local, new BandwidthThrottle(BandwidthThrottle.UNLIMITED), new DisabledStreamListener(), status,
                new DisabledLoginController());
        assertEquals((long) random.getBytes().length, status.getCurrent(), 0L);
        assertTrue(status.isComplete());
        assertTrue(new S3FindFeature(session).find(test));
        final PathAttributes attributes = session.list(container,
                new DisabledListProgressListener()).get(test.getReference()).attributes();
        assertEquals(random.getBytes().length, attributes.getSize());
        final Map<String, String> metadata = new S3MetadataFeature(session).getMetadata(test);
        assertFalse(metadata.isEmpty());
        assertEquals("text/plain", metadata.get("Content-Type"));
        new S3DefaultDeleteFeature(session).delete(Collections.<Path>singletonList(test), new DisabledLoginController());
        session.close();
    }
}

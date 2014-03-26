package ch.cyberduck.core.dav;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DefaultHostKeyController;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.http.HttpUploadFeature;
import ch.cyberduck.core.http.RedirectCallback;
import ch.cyberduck.core.http.ResponseOutputStream;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.io.DisabledStreamListener;
import ch.cyberduck.core.io.StreamCopier;
import ch.cyberduck.core.local.FinderLocal;
import ch.cyberduck.core.shared.DefaultHomeFinderService;
import ch.cyberduck.core.shared.DefaultTouchFeature;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class DAVWriteFeatureTest extends AbstractTestCase {

    @Test
    public void testReadWrite() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("webdav.user"), properties.getProperty("webdav.password")
        ));
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
        final TransferStatus status = new TransferStatus();
        final FinderLocal local = new FinderLocal(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        final byte[] content = "test".getBytes("UTF-8");
        final OutputStream out = local.getOutputStream(false);
        IOUtils.write(content, out);
        IOUtils.closeQuietly(out);
        status.setLength(content.length);
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final HttpUploadFeature upload = new DAVUploadFeature(session);
        upload.upload(test, local, new BandwidthThrottle(BandwidthThrottle.UNLIMITED), new DisabledStreamListener(), status);
        assertTrue(session.getFeature(Find.class).find(test));
        assertEquals(content.length, session.list(test.getParent(), new DisabledListProgressListener()).get(test.getReference()).attributes().getSize(), 0L);
        assertEquals(content.length, new DAVWriteFeature(session).append(test, status.getLength(), Cache.empty()).size, 0L);
        {
            final byte[] buffer = new byte[content.length];
            IOUtils.readFully(new DAVReadFeature(session).read(test, new TransferStatus()), buffer);
            assertArrayEquals(content, buffer);
        }
        {
            final byte[] buffer = new byte[content.length - 1];
            final InputStream in = new DAVReadFeature(session).read(test, new TransferStatus().length(content.length).append(true).current(1L));
            IOUtils.readFully(in, buffer);
            IOUtils.closeQuietly(in);
            final byte[] reference = new byte[content.length - 1];
            System.arraycopy(content, 1, reference, 0, content.length - 1);
            assertArrayEquals(reference, buffer);
        }
        new DAVDeleteFeature(session).delete(Collections.<Path>singletonList(test), new DisabledLoginController());
        session.close();
    }

    @Test(expected = AccessDeniedException.class)
    public void testWriteNotFound() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("webdav.user"), properties.getProperty("webdav.password")
        ));
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
        final Path test = new Path(new DefaultHomeFinderService(session).find().getAbsolute() + "/nosuchdirectory/" + UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        new DAVWriteFeature(session).write(test, new TransferStatus());
    }

    @Test
    public void testAppend() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("webdav.user"), properties.getProperty("webdav.password")
        ));
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
        final DAVWriteFeature feature = new DAVWriteFeature(session);
        assertFalse(feature.append(
                new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file)), 0L, Cache.empty()).append);
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        new DefaultTouchFeature(session).touch(test);
        assertTrue(feature.append(test, 0L, Cache.empty()).append);
        new DAVDeleteFeature(session).delete(Collections.singletonList(test), new DisabledLoginController());
    }

    @Test
    public void testUploadRedirect() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("webdav.user"), properties.getProperty("webdav.password")
        ));
        host.setDefaultPath("/dav/basic");
        final AtomicBoolean redirected = new AtomicBoolean();
        final DAVSession session = new DAVSession(host, new RedirectCallback() {
            @Override
            public boolean redirect(String method) {
                if("PUT".equals(method)) {
                    redirected.set(true);
                }
                return true;
            }
        });
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
        final DAVWriteFeature feature = new DAVWriteFeature(session);
        final Path test = new Path("/redir-tmp/" + UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final TransferStatus status = new TransferStatus();
        final byte[] content = RandomStringUtils.random(1000).getBytes();
        status.setLength(content.length);
        final ResponseOutputStream<String> out = feature.write(test, status);
        assertNotNull(out);
        new StreamCopier(status, status).transfer(new ByteArrayInputStream(content), 0, out, new DisabledStreamListener(), -1);
        assertEquals(content.length, status.getCurrent());
        assertTrue(status.isComplete());
        final PathAttributes attributes = new DAVAttributesFeature(session).find(test);
        assertEquals(content.length, attributes.getSize());
        assertTrue(redirected.get());
        new DAVDeleteFeature(session).delete(Collections.singletonList(test), new DisabledLoginController());
    }
}

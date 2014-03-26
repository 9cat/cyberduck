package ch.cyberduck.core.s3;

import ch.cyberduck.core.*;
import ch.cyberduck.core.analytics.AnalyticsProvider;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.features.AclPermission;
import ch.cyberduck.core.features.Copy;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Encryption;
import ch.cyberduck.core.features.Lifecycle;
import ch.cyberduck.core.features.Location;
import ch.cyberduck.core.features.Logging;
import ch.cyberduck.core.features.Redundancy;
import ch.cyberduck.core.features.Versioning;
import ch.cyberduck.core.identity.IdentityConfiguration;

import org.junit.Test;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class S3SessionTest extends AbstractTestCase {

    @Test
    public void testHttpProfile() throws Exception {
        final Profile profile = ProfileReaderFactory.get().read(
                LocalFactory.createLocal("profiles/S3 (HTTP).cyberduckprofile"));
        final Host host = new Host(profile, profile.getDefaultHostname(), new Credentials(
                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
        ));
        assertFalse(host.getProtocol().isSecure());
        final S3Session session = new S3Session(host, new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                fail();
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                fail();
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                fail();
                return null;
            }
        });
        assertNotNull(session.open(new DefaultHostKeyController()));
        assertTrue(session.isConnected());
        session.close();
        assertFalse(session.isConnected());
    }

    @Test
    public void testConnectUnsecured() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(), new Credentials(
                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
        ));
        final S3Session session = new S3Session(host);
        assertNotNull(session.open(new DefaultHostKeyController()));
        assertTrue(session.isConnected());
        assertNotNull(session.getClient());
        Cache cache = new Cache();
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), cache);
        assertNotNull(session.workdir());
        assertTrue(cache.containsKey(new Path("/", EnumSet.of(Path.Type.directory, Path.Type.volume)).getReference()));
        assertNotNull(cache.lookup(new Path("/test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume)).getReference()));
        assertTrue(session.isConnected());
        session.close();
        assertFalse(session.isConnected());
        assertEquals(Session.State.closed, session.getState());
        session.open(new DefaultHostKeyController());
        assertTrue(session.isConnected());
        assertNotNull(session.workdir());
        session.close();
        assertFalse(session.isConnected());
    }

    @Test
    public void testConnectDefaultPath() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(), new Credentials(
                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
        ));
        host.setDefaultPath("/test.cyberduck.ch");
        final S3Session session = new S3Session(host);
        session.open(new DefaultHostKeyController());
        Cache cache = new Cache();
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), cache);
        assertFalse(cache.containsKey(new Path("/", EnumSet.of(Path.Type.directory, Path.Type.volume)).getReference()));
        assertTrue(cache.containsKey(new Path("/test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume)).getReference()));
        session.close();
    }

    @Test(expected = LoginFailureException.class)
    public void testLoginFailure() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(), new Credentials(
                properties.getProperty("s3.key"), "s"
        ));
        final S3Session session = new S3Session(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
    }

    @Test
    public void testLoginFailureFix() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(), new Credentials(
                properties.getProperty("s3.key"), "s"
        ));
        final AtomicBoolean p = new AtomicBoolean();
        final S3Session session = new S3Session(host);
        new LoginConnectionService(new DisabledLoginController() {
            @Override
            public void prompt(final Protocol protocol, final Credentials credentials, final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
                p.set(true);
                credentials.setPassword(properties.getProperty("s3.secret"));
            }
        }, new DefaultHostKeyController(), new DisabledPasswordStore(), new DisabledProgressListener()).connect(session, Cache.empty());
        assertTrue(p.get());
        session.close();
    }

    @Test(expected = BackgroundException.class)
    public void testCustomHostnameUnknown() throws Exception {
        final Host host = new Host(new S3Protocol(), "testu.cyberduck.ch", new Credentials(
                properties.getProperty("s3.key"), "s"
        ));
        final S3Session session = new S3Session(host);
        try {
            session.open(new DefaultHostKeyController());
            session.login(new DisabledPasswordStore(), new DisabledLoginController());
        }
        catch(BackgroundException e) {
            assertTrue(e.getCause() instanceof UnknownHostException);
            throw e;
        }
    }

    @Test(expected = LoginFailureException.class)
    public void testCustomHostname() throws Exception {
        final Host host = new Host(new S3Protocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("s3.key"), "s"
        ));
        final AtomicBoolean set = new AtomicBoolean();
        final S3Session session = new S3Session(host);
        session.addTranscriptListener(new TranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                if(request) {
                    if(message.contains("Host:")) {
                        assertEquals("Host: test.cyberduck.ch:443", message);
                        set.set(true);
                    }
                }
            }
        });
        session.open(new HostKeyCallback() {
            @Override
            public boolean verify(final String hostname, final int port, final String serverHostKeyAlgorithm, final byte[] serverHostKey)
                    throws IOException, ConnectionCanceledException {
                assertEquals("test.cyberduck.ch", hostname);
                return true;
            }
        });
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
        assertTrue(set.get());
        session.close();
    }

    @Test
    public void testFeatures() throws Exception {
        final S3Session aws = new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()));
        assertNotNull(aws.getFeature(Copy.class));
        assertNotNull(aws.getFeature(AclPermission.class));
        assertNotNull(aws.getFeature(Versioning.class));
        assertNotNull(aws.getFeature(AnalyticsProvider.class));
        assertNotNull(aws.getFeature(Lifecycle.class));
        assertNotNull(aws.getFeature(Location.class));
        assertNotNull(aws.getFeature(Encryption.class));
        assertNotNull(aws.getFeature(Redundancy.class));
        assertNotNull(aws.getFeature(Logging.class));
        assertNotNull(aws.getFeature(DistributionConfiguration.class));
        assertNotNull(aws.getFeature(IdentityConfiguration.class));
        assertNotNull(aws.getFeature(IdentityConfiguration.class));
        assertEquals(S3MultipleDeleteFeature.class, aws.getFeature(Delete.class).getClass());
        final S3Session o = new S3Session(new Host(new S3Protocol(), "o"));
        assertNotNull(o.getFeature(Copy.class));
        assertNotNull(o.getFeature(AclPermission.class));
        assertNull(o.getFeature(Versioning.class));
        assertNull(o.getFeature(AnalyticsProvider.class));
        assertNull(o.getFeature(Lifecycle.class));
        assertNull(o.getFeature(Location.class));
        assertNull(o.getFeature(Encryption.class));
        assertNull(o.getFeature(Redundancy.class));
        assertNull(o.getFeature(Logging.class));
        assertNotNull(o.getFeature(DistributionConfiguration.class));
        assertNull(o.getFeature(IdentityConfiguration.class));
        assertEquals(S3DefaultDeleteFeature.class, o.getFeature(Delete.class).getClass());
    }
}

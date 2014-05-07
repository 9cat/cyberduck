package ch.cyberduck.core.cloudfront;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginConnectionService;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.cdn.Distribution;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.cdn.features.Cname;
import ch.cyberduck.core.cdn.features.DistributionLogging;
import ch.cyberduck.core.cdn.features.Index;
import ch.cyberduck.core.cdn.features.Purge;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.identity.IdentityConfiguration;
import ch.cyberduck.core.s3.S3Protocol;
import ch.cyberduck.core.s3.S3Session;

import org.jets3t.service.CloudFrontService;
import org.jets3t.service.CloudFrontServiceException;
import org.jets3t.service.model.cloudfront.LoggingStatus;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class CloudFrontDistributionConfigurationTest extends AbstractTestCase {

    @Test
    public void testGetMethods() throws Exception {
        final S3Session session = new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()));
        assertEquals(Arrays.asList(Distribution.DOWNLOAD, Distribution.STREAMING),
                new CloudFrontDistributionConfiguration(session).getMethods(new Path("/bbb", EnumSet.of(Path.Type.directory, Path.Type.volume))));
    }

    @Test
    public void testGetName() throws Exception {
        final S3Session session = new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()));
        final DistributionConfiguration configuration = new CloudFrontDistributionConfiguration(
                session);
        assertEquals("Amazon CloudFront", configuration.getName());
        assertEquals("Amazon CloudFront", configuration.getName(Distribution.CUSTOM));
    }

    @Test
    public void testGetOrigin() throws Exception {
        final S3Session session = new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()));
        final CloudFrontDistributionConfiguration configuration
                = new CloudFrontDistributionConfiguration(session);
        assertEquals("bbb.s3.amazonaws.com",
                configuration.getOrigin(new Path("/bbb", EnumSet.of(Path.Type.directory, Path.Type.volume)), Distribution.DOWNLOAD).getHost());
    }

    @Test
    public void testReadDownload() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        host.setCredentials(properties.getProperty("s3.key"), properties.getProperty("s3.secret"));
        final S3Session session = new S3Session(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final DistributionConfiguration configuration
                = new CloudFrontDistributionConfiguration(session);
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Distribution distribution = configuration.read(container, Distribution.DOWNLOAD, new DisabledLoginController());
        assertEquals("E2N9XG26504TZI", distribution.getId());
        assertEquals(Distribution.DOWNLOAD, distribution.getMethod());
        assertEquals("Deployed", distribution.getStatus());
        assertEquals("test.cyberduck.ch.s3.amazonaws.com", distribution.getOrigin().getHost());
        assertEquals(URI.create("http://d8s2h7wj83mnt.cloudfront.net"), distribution.getUrl());
        assertEquals("1a0764da-1790-4ca9-a977-e40752ae04cd", distribution.getIndexDocument());
        assertEquals(null, distribution.getErrorDocument());
    }

    @Test
    public void testReadStreaming() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        host.setCredentials(properties.getProperty("s3.key"), properties.getProperty("s3.secret"));
        final S3Session session = new S3Session(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final DistributionConfiguration configuration
                = new CloudFrontDistributionConfiguration(session);
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Distribution distribution = configuration.read(container, Distribution.STREAMING, new DisabledLoginController());
        assertEquals("EB86EC8N0TBBE", distribution.getId());
        assertEquals("test.cyberduck.ch.s3.amazonaws.com", distribution.getOrigin().getHost());
        assertEquals(URI.create("rtmp://s2o9ssk5sk7hj5.cloudfront.net/cfx/st"), distribution.getUrl());
        assertEquals(null, distribution.getIndexDocument());
        assertEquals(null, distribution.getErrorDocument());
    }

    @Test(expected = LoginCanceledException.class)
    public void testReadLoginFailure() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        final S3Session session = new S3Session(host);
        final DistributionConfiguration configuration
                = new CloudFrontDistributionConfiguration(session);
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        configuration.read(container, Distribution.DOWNLOAD, new DisabledLoginController());
    }

    @Test
    public void testReadLoginFailureFix() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        host.setCredentials(properties.getProperty("s3.key"), properties.getProperty("s3.secret"));
        final S3Session session = new S3Session(host);
        new LoginConnectionService(new DisabledLoginController() {
            @Override
            public void prompt(final Protocol protocol, final Credentials credentials, final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
                credentials.setPassword(properties.getProperty("s3.secret"));
            }
        }, new DisabledHostKeyCallback(), new DisabledPasswordStore(), new DisabledProgressListener()).connect(session, Cache.empty());
        assertTrue(session.isConnected());
        assertNull(host.getCredentials().getPassword());
        final DistributionConfiguration configuration
                = new CloudFrontDistributionConfiguration(session);
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final AtomicBoolean set = new AtomicBoolean();
        configuration.read(container, Distribution.DOWNLOAD, new DisabledLoginController() {
            @Override
            public void prompt(final Protocol protocol, final Credentials credentials, final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
                credentials.setPassword(properties.getProperty("s3.secret"));
                set.set(true);
            }
        });
        assertTrue(set.get());
        session.close();
    }

    @Test
    public void testWriteExists() throws Exception {
        final AtomicBoolean set = new AtomicBoolean();
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        host.setCredentials(properties.getProperty("s3.key"), properties.getProperty("s3.secret"));
        final S3Session session = new S3Session(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final DistributionConfiguration configuration = new CloudFrontDistributionConfiguration(session) {
            @Override
            protected void updateDistribution(final Distribution current, final CloudFrontService client, final Path container, final Distribution distribution, final LoggingStatus logging) throws CloudFrontServiceException, IOException, ConnectionCanceledException {
                set.set(true);
            }

            @Override
            protected org.jets3t.service.model.cloudfront.Distribution createDistribution(final CloudFrontService client, final Path container, final Distribution distribution, final LoggingStatus logging) throws ConnectionCanceledException, CloudFrontServiceException {
                fail();
                return null;
            }
        };
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        configuration.write(container, new Distribution(Distribution.DOWNLOAD, true), new DisabledLoginController());
        assertTrue(set.get());
    }

    @Test
    public void testWriteNew() throws Exception {
        final AtomicBoolean set = new AtomicBoolean();
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        host.setCredentials(properties.getProperty("s3.key"), properties.getProperty("s3.secret"));
        final S3Session session = new S3Session(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final DistributionConfiguration configuration = new CloudFrontDistributionConfiguration(session) {
            @Override
            protected void updateDistribution(final Distribution current, final CloudFrontService client, final Path container, final Distribution distribution, final LoggingStatus logging) throws CloudFrontServiceException, IOException, ConnectionCanceledException {
                fail();
            }

            @Override
            protected org.jets3t.service.model.cloudfront.Distribution createDistribution(final CloudFrontService client, final Path container, final Distribution distribution, final LoggingStatus logging) throws ConnectionCanceledException, CloudFrontServiceException {
                set.set(true);
                return null;
            }
        };
        final Path container = new Path("test2.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        configuration.write(container, new Distribution(Distribution.STREAMING, true), new DisabledLoginController());
        assertTrue(set.get());
    }

    @Test
    public void testProtocol() {
        assertEquals("cloudfront.amazonaws.com", new CloudFrontDistributionConfiguration(
                new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()))
        ).getHostname());
    }

    @Test
    public void testFeatures() {
        final CloudFrontDistributionConfiguration d = new CloudFrontDistributionConfiguration(
                new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()))
        );
        assertNotNull(d.getFeature(Purge.class, Distribution.DOWNLOAD));
        assertNotNull(d.getFeature(Purge.class, Distribution.WEBSITE_CDN));
        assertNull(d.getFeature(Purge.class, Distribution.STREAMING));
        assertNull(d.getFeature(Purge.class, Distribution.WEBSITE));
        assertNotNull(d.getFeature(Index.class, Distribution.DOWNLOAD));
        assertNotNull(d.getFeature(Index.class, Distribution.WEBSITE_CDN));
        assertNull(d.getFeature(Index.class, Distribution.STREAMING));
        assertNull(d.getFeature(Index.class, Distribution.WEBSITE));
        assertNotNull(d.getFeature(DistributionLogging.class, Distribution.DOWNLOAD));
        assertNotNull(d.getFeature(Cname.class, Distribution.DOWNLOAD));
        assertNotNull(d.getFeature(IdentityConfiguration.class, Distribution.DOWNLOAD));
    }
}

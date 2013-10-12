package ch.cyberduck.core.cloudfront;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DefaultHostKeyController;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
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
import java.util.Arrays;
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
                new CloudFrontDistributionConfiguration(session).getMethods(new Path("/bbb", Path.VOLUME_TYPE)));
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
                configuration.getOrigin(new Path("/bbb", Path.VOLUME_TYPE), Distribution.DOWNLOAD).getHost());
    }

    @Test
    public void testReadDownload() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        host.setCredentials(new Credentials(
                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
        ));
        final S3Session session = new S3Session(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
        final DistributionConfiguration configuration
                = new CloudFrontDistributionConfiguration(session);
        final Path container = new Path("test.cyberduck.ch", Path.VOLUME_TYPE);
        final Distribution distribution = configuration.read(container, Distribution.DOWNLOAD, new DisabledLoginController());
        assertEquals("E2N9XG26504TZI", distribution.getId());
        assertEquals(Distribution.DOWNLOAD, distribution.getMethod());
        assertEquals("Deployed", distribution.getStatus());
        assertEquals("test.cyberduck.ch.s3.amazonaws.com", distribution.getOrigin());
        assertEquals("http://d8s2h7wj83mnt.cloudfront.net", distribution.getUrl());
        assertEquals(null, distribution.getIndexDocument());
        assertEquals(null, distribution.getErrorDocument());
        assertEquals("E1G1TCL9X2DSET", distribution.getEtag());
    }

    @Test
    public void testReadStreaming() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        host.setCredentials(new Credentials(
                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
        ));
        final S3Session session = new S3Session(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
        final DistributionConfiguration configuration
                = new CloudFrontDistributionConfiguration(session);
        final Path container = new Path("test.cyberduck.ch", Path.VOLUME_TYPE);
        final Distribution distribution = configuration.read(container, Distribution.STREAMING, new DisabledLoginController());
        assertEquals("EB86EC8N0TBBE", distribution.getId());
        assertEquals("test.cyberduck.ch.s3.amazonaws.com", distribution.getOrigin());
        assertEquals("rtmp://s2o9ssk5sk7hj5.cloudfront.net/cfx/st", distribution.getUrl());
        assertEquals(null, distribution.getIndexDocument());
        assertEquals(null, distribution.getErrorDocument());
    }

    @Test(expected = LoginCanceledException.class)
    public void testReadLoginFailure() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        final S3Session session = new S3Session(host);
        final DistributionConfiguration configuration
                = new CloudFrontDistributionConfiguration(session);
        final Path container = new Path("test.cyberduck.ch", Path.VOLUME_TYPE);
        configuration.read(container, Distribution.DOWNLOAD, new DisabledLoginController());
    }

    @Test
    public void testWriteExists() throws Exception {
        final AtomicBoolean set = new AtomicBoolean();
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        host.setCredentials(new Credentials(
                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
        ));
        final S3Session session = new S3Session(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
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
        final Path container = new Path("test.cyberduck.ch", Path.VOLUME_TYPE);
        configuration.write(container, new Distribution("test.cyberduck.ch.s3.amazonaws.com", Distribution.DOWNLOAD),
                new DisabledLoginController());
        assertTrue(set.get());
    }

    @Test
    public void testWriteNew() throws Exception {
        final AtomicBoolean set = new AtomicBoolean();
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname());
        host.setCredentials(new Credentials(
                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
        ));
        final S3Session session = new S3Session(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController());
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
        final Path container = new Path("test2.cyberduck.ch", Path.VOLUME_TYPE);
        configuration.write(container, new Distribution("test2.cyberduck.ch.s3.amazonaws.com", Distribution.STREAMING),
                new DisabledLoginController());
        assertTrue(set.get());
    }

    @Test
    public void testProtocol() {
        assertEquals("cloudfront.amazonaws.com", new CloudFrontDistributionConfiguration(
                new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()))
        ).getProtocol().getDefaultHostname());
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

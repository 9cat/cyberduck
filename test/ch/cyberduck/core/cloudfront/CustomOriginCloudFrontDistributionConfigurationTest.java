package ch.cyberduck.core.cloudfront;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.DescriptiveUrl;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.cdn.Distribution;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.s3.S3Protocol;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.sftp.SFTPProtocol;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @version $Id$
 */
public class CustomOriginCloudFrontDistributionConfigurationTest extends AbstractTestCase {

    @Test
    public void testGetMethods() throws Exception {
        assertEquals(Arrays.asList(Distribution.CUSTOM),
                new CustomOriginCloudFrontDistributionConfiguration(new Host("o")).getMethods(
                        new Path("/bbb", Path.VOLUME_TYPE)));
    }

    @Test
    public void testGetOrigin() throws Exception {
        final Host h = new Host("m");
        final Path container = new Path("/", Path.VOLUME_TYPE);
        h.setWebURL("http://w.example.net");
        final S3Session session = new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()));
        final CustomOriginCloudFrontDistributionConfiguration configuration
                = new CustomOriginCloudFrontDistributionConfiguration(h);
        assertEquals("w.example.net", configuration.getOrigin(container, Distribution.CUSTOM).getHost());
        h.setWebURL(null);
        assertEquals("m", configuration.getOrigin(container, Distribution.CUSTOM).getHost());
        h.setWebURL("f");
        assertEquals("f", configuration.getOrigin(container, Distribution.CUSTOM).getHost());
    }

    @Test
    public void testGetOriginCustomHttpPort() throws Exception {
        final Host h = new Host("m");
        final Path container = new Path("/", Path.VOLUME_TYPE);
        h.setWebURL("http://w.example.net:8080");
        final S3Session session = new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()));
        final CustomOriginCloudFrontDistributionConfiguration configuration
                = new CustomOriginCloudFrontDistributionConfiguration(h);
        assertEquals("w.example.net", configuration.getOrigin(container, Distribution.CUSTOM).getHost());
        assertEquals(8080, configuration.getOrigin(container, Distribution.CUSTOM).getPort());
        h.setWebURL(null);
        assertEquals("m", configuration.getOrigin(container, Distribution.CUSTOM).getHost());
        assertEquals(-1, configuration.getOrigin(container, Distribution.CUSTOM).getPort());
    }

    @Test
    public void testGetOriginCustomHttpsPort() throws Exception {
        final Host h = new Host("m");
        final Path container = new Path("/", Path.VOLUME_TYPE);
        h.setWebURL("https://w.example.net:4444");
        final S3Session session = new S3Session(new Host(new S3Protocol(), new S3Protocol().getDefaultHostname()));
        final CustomOriginCloudFrontDistributionConfiguration configuration
                = new CustomOriginCloudFrontDistributionConfiguration(h);
        assertEquals("w.example.net", configuration.getOrigin(container, Distribution.CUSTOM).getHost());
        assertEquals("https", configuration.getOrigin(container, Distribution.CUSTOM).getScheme());
        assertEquals(4444, configuration.getOrigin(container, Distribution.CUSTOM).getPort());
        h.setWebURL(null);
        assertEquals("m", configuration.getOrigin(container, Distribution.CUSTOM).getHost());
        assertEquals(-1, configuration.getOrigin(container, Distribution.CUSTOM).getPort());
    }

    @Test
    public void testReadNoConfiguredDistributionForOrigin() throws Exception {
        final Host origin = new Host("myhost.localdomain");
        origin.getCdnCredentials().setUsername(properties.getProperty("s3.key"));
        origin.getCdnCredentials().setPassword(properties.getProperty("s3.secret"));
        final CustomOriginCloudFrontDistributionConfiguration configuration
                = new CustomOriginCloudFrontDistributionConfiguration(origin);
        final Path container = new Path("unknown.cyberduck.ch", Path.VOLUME_TYPE);
        final Distribution distribution = configuration.read(container, Distribution.CUSTOM, new DisabledLoginController());
        assertNull(distribution.getId());
        assertEquals("myhost.localdomain", distribution.getOrigin().getHost());
        assertEquals("Unknown", distribution.getStatus());
        assertEquals(null, distribution.getId());
    }

    @Test
    public void testRead() throws Exception {
        final Host origin = new Host("myhost.localdomain");
        origin.setWebURL("http://test.cyberduck.ch");
        origin.setDefaultPath("public_html");
        origin.getCdnCredentials().setUsername(properties.getProperty("s3.key"));
        origin.getCdnCredentials().setPassword(properties.getProperty("s3.secret"));
        final CustomOriginCloudFrontDistributionConfiguration configuration
                = new CustomOriginCloudFrontDistributionConfiguration(origin);
        final Distribution distribution = configuration.read(new Path("/public_html", Path.DIRECTORY_TYPE), Distribution.CUSTOM, new DisabledLoginController());
        assertEquals("E230LC0UG2YLKV", distribution.getId());
        assertEquals("http://test.cyberduck.ch/public_html", distribution.getOrigin().toString());
        assertEquals("http://test.cyberduck.ch/f", configuration.toUrl(new Path("/public_html/f", Path.FILE_TYPE)).find(DescriptiveUrl.Type.origin).getUrl());
        assertEquals(Distribution.CUSTOM, distribution.getMethod());
        assertEquals("http://d1f6cbdjcbzyiu.cloudfront.net", distribution.getUrl().toString());
        assertEquals(null, distribution.getIndexDocument());
        assertEquals(null, distribution.getErrorDocument());
        assertEquals("log.test.cyberduck.ch", distribution.getLoggingContainer());
    }

    @Test(expected = LoginCanceledException.class)
    public void testReadMissingCredentials() throws Exception {
        final Host bookmark = new Host(new SFTPProtocol(), "myhost.localdomain");
        final CustomOriginCloudFrontDistributionConfiguration configuration
                = new CustomOriginCloudFrontDistributionConfiguration(bookmark);
        final Path container = new Path("test.cyberduck.ch", Path.VOLUME_TYPE);
        configuration.read(container, Distribution.CUSTOM, new DisabledLoginController());
    }
}

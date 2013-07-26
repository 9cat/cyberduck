package ch.cyberduck.core;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class DescriptiveUrlTest extends AbstractTestCase {

    @Test
    public void testEquals() throws Exception {
        assertTrue(new DescriptiveUrl(URI.create("http://host.domain"), DescriptiveUrl.Type.provider, "a").equals(
                new DescriptiveUrl(URI.create("http://host.domain"), DescriptiveUrl.Type.provider, "b")));
        assertFalse(new DescriptiveUrl(URI.create("http://host.domainb"), DescriptiveUrl.Type.provider, "a").equals(
                new DescriptiveUrl(URI.create("http://host.domain"), DescriptiveUrl.Type.provider, "b")));
    }

    @Test
    public void testDefault() throws Exception {
        assertEquals("Open in Web Browser", new DescriptiveUrl(URI.create("http://me")).getHelp());
    }
}

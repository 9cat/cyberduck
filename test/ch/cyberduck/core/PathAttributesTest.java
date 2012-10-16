package ch.cyberduck.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class PathAttributesTest extends AbstractTestCase {

    @Test
    public void testGetAsDictionary() throws Exception {
        PathAttributes attributes = new PathAttributes(Path.VOLUME_TYPE | Path.DIRECTORY_TYPE);
        attributes.setSize(3L);
        attributes.setModificationDate(5343L);
        assertEquals(attributes, new PathAttributes(attributes.getAsDictionary()));
    }
}

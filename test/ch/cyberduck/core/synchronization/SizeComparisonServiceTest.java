package ch.cyberduck.core.synchronization;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.LocalAttributes;
import ch.cyberduck.core.PathAttributes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class SizeComparisonServiceTest extends AbstractTestCase {

    @Test
    public void testCompare() throws Exception {
        ComparisonService s = new SizeComparisonService();
        assertEquals(Comparison.equal, s.compare(new PathAttributes() {
                                                     @Override
                                                     public long getSize() {
                                                         return 1L;
                                                     }
                                                 }, new LocalAttributes("/t") {
                                                     @Override
                                                     public long getSize() {
                                                         return 1L;
                                                     }
                                                 }
        ));

        assertEquals(Comparison.notequal, s.compare(new PathAttributes() {
                                                        @Override
                                                        public long getSize() {
                                                            return 2L;
                                                        }
                                                    }, new LocalAttributes("/t") {
                                                        @Override
                                                        public long getSize() {
                                                            return 1L;
                                                        }
                                                    }
        ));
    }

    @Test
    public void testDirectory() throws Exception {
        ComparisonService s = new SizeComparisonService();
        assertEquals(Comparison.notequal, s.compare(new PathAttributes(),
                new LocalAttributes("/t")));
    }
}

package ch.cyberduck.ui;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.threading.AbstractBackgroundAction;
import ch.cyberduck.core.threading.MainAction;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * @version $Id:$
 */
public class AbstractControllerTest extends AbstractTestCase {

    @Test
    public void testBackground() throws Exception {
        final AbstractController controller = new AbstractController() {
            @Override
            public void invoke(final MainAction runnable, final boolean wait) {
                assertEquals("main", Thread.currentThread().getName());
            }
        };

        final Object lock = new Object();

        final CountDownLatch entry = new CountDownLatch(1);
        final CountDownLatch exit = new CountDownLatch(1);
        final AbstractBackgroundAction action = new AbstractBackgroundAction() {
            @Override
            public void init() {
                assertEquals("main", Thread.currentThread().getName());
            }

            @Override
            public void run() throws BackgroundException {
                assertEquals("background-1", Thread.currentThread().getName());
                entry.countDown();
                try {
                    exit.await();
                }
                catch(InterruptedException e) {
                    fail();
                }
            }

            @Override
            public void cleanup() {
                assertEquals("main", Thread.currentThread().getName());
                assertFalse(controller.getActions().contains(this));
            }

            @Override
            public Object lock() {
                return lock;
            }
        };
        controller.background(action);
        controller.background(new AbstractBackgroundAction() {
            @Override
            public void run() throws BackgroundException {
                assertFalse(controller.getActions().contains(action));
            }

            @Override
            public Object lock() {
                return lock;
            }
        });
        entry.await();
        assertTrue(controller.getActions().contains(action));
        exit.countDown();
    }
}
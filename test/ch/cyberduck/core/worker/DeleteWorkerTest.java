package ch.cyberduck.core.worker;

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.ProgressListener;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.ftp.FTPSession;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @version $Id$
 */
public class DeleteWorkerTest extends AbstractTestCase {

    @Test
    public void testCompile() throws Exception {
        final FTPSession session = new FTPSession(new Host("t")) {
            @Override
            public <T> T getFeature(final Class<T> type) {
                return (T) new Delete() {
                    @Override
                    public void delete(final List<Path> files, final LoginCallback prompt, final ProgressListener listener) throws BackgroundException {
                        assertEquals(new Path("/t/a", EnumSet.of(Path.Type.file)), files.get(0));
                        assertEquals(new Path("/t/d/b", EnumSet.of(Path.Type.file)), files.get(1));
                        assertEquals(new Path("/t/d", EnumSet.of(Path.Type.directory)), files.get(2));
                        assertEquals(new Path("/t", EnumSet.of(Path.Type.directory)), files.get(3));
                    }
                };
            }

            @Override
            public AttributedList<Path> list(final Path file, final ListProgressListener listener) throws BackgroundException {
                if(file.equals(new Path("/t", EnumSet.of(Path.Type.directory)))) {
                    return new AttributedList<Path>(Arrays.asList(
                            new Path("/t/a", EnumSet.of(Path.Type.file)),
                            new Path("/t/d", EnumSet.of(Path.Type.directory))
                    ));
                }
                if(file.equals(new Path("/t/d", EnumSet.of(Path.Type.directory)))) {
                    return new AttributedList<Path>(Arrays.asList(
                            new Path("/t/d/b", EnumSet.of(Path.Type.file))
                    ));
                }
                fail();
                return null;
            }
        };
        final DeleteWorker worker = new DeleteWorker(session, new DisabledLoginCallback(),
                Collections.singletonList(new Path("/t", EnumSet.of(Path.Type.directory))),
                new DisabledProgressListener());
        worker.run();
    }

    @Test
    public void testSymlink() throws Exception {
        final FTPSession session = new FTPSession(new Host("t")) {
            @Override
            public <T> T getFeature(final Class<T> type) {
                return (T) new Delete() {
                    @Override
                    public void delete(final List<Path> files, final LoginCallback prompt, final ProgressListener listener) throws BackgroundException {
                        assertEquals(new Path("/s", EnumSet.of(Path.Type.directory, AbstractPath.Type.symboliclink)), files.get(0));
                    }
                };
            }

            @Override
            public AttributedList<Path> list(final Path file, final ListProgressListener listener) throws BackgroundException {
                fail();
                return null;
            }
        };
        final DeleteWorker worker = new DeleteWorker(session, new DisabledLoginCallback(),
                Collections.singletonList(new Path("/s", EnumSet.of(Path.Type.directory, AbstractPath.Type.symboliclink))),
                new DisabledProgressListener());
        worker.run();
    }
}

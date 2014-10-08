package ch.cyberduck.core.openstack;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledTranscriptListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;

import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

import ch.iterate.openstack.swift.model.StorageObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id$
 */
public class SwiftSegmentServiceTest extends AbstractTestCase {

    @Test
    public void testList() throws Exception {
        final SwiftSession session = new SwiftSession(
                new Host(new SwiftProtocol(), "identity.api.rackspacecloud.com",
                        new Credentials(
                                properties.getProperty("rackspace.key"), properties.getProperty("rackspace.secret")
                        )));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final Path container = new Path("/test.cyberduck.ch", EnumSet.of(Path.Type.volume, Path.Type.directory));
        container.attributes().setRegion("DFW");
        assertTrue(new SwiftSegmentService(session).list(new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file))).isEmpty());
        session.close();
    }

    @Test
    public void testManifest() throws Exception {
        final SwiftSession session = new SwiftSession(
                new Host(new SwiftProtocol(), "identity.api.rackspacecloud.com",
                        new Credentials(
                                properties.getProperty("rackspace.key"), properties.getProperty("rackspace.secret")
                        )));
        final SwiftSegmentService service = new SwiftSegmentService(session);
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final StorageObject a = new StorageObject("a");
        a.setMd5sum("m1");
        a.setSize(1L);
        final StorageObject b = new StorageObject("b");
        b.setMd5sum("m2");
        b.setSize(1L);
        final String manifest = service.manifest(container.getName(), Arrays.asList(a, b));
        assertEquals("[{\"path\":\"/test.cyberduck.ch/a\",\"etag\":\"m1\",\"size_bytes\":1},{\"path\":\"/test.cyberduck.ch/b\",\"etag\":\"m2\",\"size_bytes\":1}]", manifest);
    }

    @Test
    public void testBasename() throws Exception {
        final SwiftSession session = new SwiftSession(
                new Host(new SwiftProtocol(), "identity.api.rackspacecloud.com",
                        new Credentials(
                                properties.getProperty("rackspace.key"), properties.getProperty("rackspace.secret")
                        )));
        final SwiftSegmentService service = new SwiftSegmentService(session, ".prefix/");
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final String name = UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString();
        assertEquals(".prefix/" + name + "/3", service.basename(new Path(container, name, EnumSet.of(Path.Type.file)), 3L));
    }
}

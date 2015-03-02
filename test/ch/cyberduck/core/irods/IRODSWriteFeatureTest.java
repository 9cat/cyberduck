package ch.cyberduck.core.irods;

/*
 * Copyright (c) 2002-2013 David Kocher. All rights reserved.
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to feedback@cyberduck.ch
 */

import ch.cyberduck.core.*;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.io.StreamCopier;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @version $Id: IRODSReadFeatureTest.java 17029 2015-02-27 13:09:19Z dkocher $
 */
public class IRODSWriteFeatureTest extends AbstractTestCase {

    @Test
    public void testWrite() throws Exception {
        final Profile profile = ProfileReaderFactory.get().read(
                new Local("profiles/iRODS (iPlant Collaborative).cyberduckprofile"));
        final Host host = new Host(profile, profile.getDefaultHostname(), new Credentials(
                properties.getProperty("irods.key"), properties.getProperty("irods.secret")
        ));

        final IRODSSession session = new IRODSSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());

        final Path test = new Path(session.workdir(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        assertFalse(session.getFeature(Find.class).find(test));

        final byte[] content = RandomStringUtils.random(100).getBytes();
        {
            final TransferStatus status = new TransferStatus();
            status.setAppend(false);
            status.setLength(content.length);

            assertEquals(false, new IRODSWriteFeature(session).append(test, status.getLength(), PathCache.empty()).append);
            assertEquals(0L, new IRODSWriteFeature(session).append(test, status.getLength(), PathCache.empty()).size, 0L);

            final OutputStream out = new IRODSWriteFeature(session).write(test, status);
            assertNotNull(out);

            new StreamCopier(new TransferStatus(), new TransferStatus()).transfer(new ByteArrayInputStream(content), out);
            IOUtils.closeQuietly(out);
            assertTrue(session.getFeature(Find.class).find(test));

            final PathAttributes attributes = session.list(test.getParent(), new DisabledListProgressListener()).get(test).attributes();
            assertEquals(content.length, attributes.getSize());

            final InputStream in = session.getFeature(Read.class).read(test, new TransferStatus());
            final byte[] buffer = new byte[content.length];
            IOUtils.readFully(in, buffer);
            IOUtils.closeQuietly(in);
            assertArrayEquals(content, buffer);
        }
        {
            final byte[] newcontent = RandomStringUtils.random(10).getBytes();

            final TransferStatus status = new TransferStatus();
            status.setAppend(false);
            status.setLength(newcontent.length);

            assertEquals(true, new IRODSWriteFeature(session).append(test, status.getLength(), PathCache.empty()).append);
            assertEquals(content.length, new IRODSWriteFeature(session).append(test, status.getLength(), PathCache.empty()).size, 0L);

            final OutputStream out = new IRODSWriteFeature(session).write(test, status);
            assertNotNull(out);

            new StreamCopier(new TransferStatus(), new TransferStatus()).transfer(new ByteArrayInputStream(newcontent), out);
            IOUtils.closeQuietly(out);
            assertTrue(session.getFeature(Find.class).find(test));

            final PathAttributes attributes = session.list(test.getParent(), new DisabledListProgressListener()).get(test).attributes();
            assertEquals(newcontent.length, attributes.getSize());

            final InputStream in = session.getFeature(Read.class).read(test, new TransferStatus());
            final byte[] buffer = new byte[newcontent.length];
            IOUtils.readFully(in, buffer);
            IOUtils.closeQuietly(in);
            assertArrayEquals(newcontent, buffer);
        }

        session.getFeature(Delete.class).delete(Arrays.asList(test), new DisabledLoginCallback(), new DisabledProgressListener());
        assertFalse(session.getFeature(Find.class).find(test));
        session.close();
    }

    @Test
    public void testWriteAppend() throws Exception {
        final Profile profile = ProfileReaderFactory.get().read(
                new Local("profiles/iRODS (iPlant Collaborative).cyberduckprofile"));
        final Host host = new Host(profile, profile.getDefaultHostname(), new Credentials(
                properties.getProperty("irods.key"), properties.getProperty("irods.secret")
        ));

        final IRODSSession session = new IRODSSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());

        final Path test = new Path(session.workdir(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        assertFalse(session.getFeature(Find.class).find(test));

        final byte[] content = RandomStringUtils.random((int) (Math.random()*100)).getBytes();

        final TransferStatus status = new TransferStatus();
        status.setAppend(true);
        status.setLength(content.length);

        assertEquals(false, new IRODSWriteFeature(session).append(test, status.getLength(), PathCache.empty()).append);
        assertEquals(0L, new IRODSWriteFeature(session).append(test, status.getLength(), PathCache.empty()).size, 0L);

        final OutputStream out = new IRODSWriteFeature(session).write(test, status);
        assertNotNull(out);

        new StreamCopier(new TransferStatus(), new TransferStatus()).transfer(new ByteArrayInputStream(content), out);
        IOUtils.closeQuietly(out);
        assertTrue(session.getFeature(Find.class).find(test));

        final PathAttributes attributes = session.list(test.getParent(), new DisabledListProgressListener()).get(test).attributes();
        assertEquals(content.length, attributes.getSize());

        final InputStream in = session.getFeature(Read.class).read(test, new TransferStatus());
        final byte[] buffer = new byte[content.length];
        IOUtils.readFully(in, buffer);
        IOUtils.closeQuietly(in);
        assertArrayEquals(content, buffer);

        // Append

        final byte[] content_append = RandomStringUtils.random((int) (Math.random()*100)).getBytes();

        final TransferStatus status_append = new TransferStatus();
        status_append.setAppend(true);
        status_append.setLength(content_append.length);

        assertEquals(true, new IRODSWriteFeature(session).append(test, status_append.getLength(), PathCache.empty()).append);
        assertEquals(status.getLength(), new IRODSWriteFeature(session).append(test, status_append.getLength(), PathCache.empty()).size, 0L);

        final OutputStream out_append = new IRODSWriteFeature(session).write(test, status_append);
        assertNotNull(out_append);

        new StreamCopier(new TransferStatus(), new TransferStatus()).transfer(new ByteArrayInputStream(content_append), out_append);
        IOUtils.closeQuietly(out_append);
        assertTrue(session.getFeature(Find.class).find(test));

        final PathAttributes attributes_complete = session.list(test.getParent(), new DisabledListProgressListener()).get(test).attributes();
        assertEquals(content.length + content_append.length, attributes_complete.getSize());

        final InputStream in_append = session.getFeature(Read.class).read(test, new TransferStatus());
        final byte[] buffer_complete = new byte[content.length + content_append.length];
        IOUtils.readFully(in_append, buffer_complete);
        IOUtils.closeQuietly(in_append);

        byte[] complete = new byte[content.length + content_append.length];
        System.arraycopy(content, 0, complete, 0, content.length);
        System.arraycopy(content_append, 0, complete, content.length, content_append.length);
        assertArrayEquals(complete, buffer_complete);

        session.getFeature(Delete.class).delete(Arrays.asList(test), new DisabledLoginCallback(), new DisabledProgressListener());
        assertFalse(session.getFeature(Find.class).find(test));
        session.close();
    }
}

package ch.cyberduck.core.s3;

/*
 * Copyright (c) 2002-2015 David Kocher. All rights reserved.
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

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledTranscriptListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.io.SHA256ChecksumCompute;
import ch.cyberduck.core.io.StreamCopier;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.Assert.*;

public class S3MultipartCopyFeatureTest {

    @Test
    public void testCopyZeroLength() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(),
                new Credentials(System.getProperties().getProperty("s3.key"), System.getProperties().getProperty("s3.secret"))
        );
        final S3Session session = new S3Session(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());

        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path test = new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        test.attributes().setSize(0L);
        new S3TouchFeature(session).touch(test);
        final Path copy = new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        new S3MultipartCopyFeature(session).copy(test, copy);
        assertTrue(new S3FindFeature(session).find(test));
        new S3DefaultDeleteFeature(session).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
            }
        });
        assertTrue(new S3FindFeature(session).find(copy));
        new S3DefaultDeleteFeature(session).delete(Collections.singletonList(copy), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
            }
        });
        session.close();
    }

    @Test
    public void testCopy() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(),
                new Credentials(System.getProperties().getProperty("s3.key"), System.getProperties().getProperty("s3.secret"))
        );
        final S3Session session = new S3Session(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());

        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path test = new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final byte[] content = RandomStringUtils.random(1000).getBytes();
        final TransferStatus status = new TransferStatus().length(content.length);
        status.setChecksum(new SHA256ChecksumCompute().compute(new ByteArrayInputStream(content)));
        final OutputStream out = new S3WriteFeature(session).write(test, status);
        assertNotNull(out);
        new StreamCopier(new TransferStatus(), new TransferStatus()).transfer(new ByteArrayInputStream(content), out);
        IOUtils.closeQuietly(out);
        test.attributes().setSize(content.length);
        final Path copy = new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));

        final S3MultipartCopyFeature feature = new S3MultipartCopyFeature(session);
        feature.copy(test, copy);
        assertTrue(new S3FindFeature(session).find(test));
        assertEquals(content.length, new S3AttributesFeature(session).find(test).getSize());
        new S3DefaultDeleteFeature(session).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
            }
        });
        assertTrue(new S3FindFeature(session).find(copy));
        assertEquals(content.length, new S3AttributesFeature(session).find(copy).getSize());
        new S3DefaultDeleteFeature(session).delete(Collections.singletonList(copy), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
            }
        });
        session.close();
    }

    @Test
    public void testCopyAWS4Signature() throws Exception {
        final Host host = new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(),
                new Credentials(System.getProperties().getProperty("s3.key"), System.getProperties().getProperty("s3.secret"))
        );
        final S3Session session = new S3Session(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());

        final Path container = new Path("test.eu-central-1.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path test = new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final byte[] content = RandomStringUtils.random(1000).getBytes();
        final TransferStatus status = new TransferStatus().length(content.length);
        status.setChecksum(new SHA256ChecksumCompute().compute(new ByteArrayInputStream(content)));
        final OutputStream out = new S3WriteFeature(session).write(test, status);
        assertNotNull(out);
        new StreamCopier(new TransferStatus(), new TransferStatus()).transfer(new ByteArrayInputStream(content), out);
        IOUtils.closeQuietly(out);
        test.attributes().setSize(content.length);
        final Path copy = new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));

        final S3MultipartCopyFeature feature = new S3MultipartCopyFeature(session);
        feature.copy(test, copy);
        assertTrue(new S3FindFeature(session).find(test));
        assertEquals(content.length, new S3AttributesFeature(session).find(test).getSize());
        new S3DefaultDeleteFeature(session).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
            }
        });
        assertTrue(new S3FindFeature(session).find(copy));
        assertEquals(content.length, new S3AttributesFeature(session).find(copy).getSize());
        new S3DefaultDeleteFeature(session).delete(Collections.singletonList(copy), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
            }
        });
        session.close();
    }
}
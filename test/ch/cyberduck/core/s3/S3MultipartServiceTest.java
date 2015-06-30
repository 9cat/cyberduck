package ch.cyberduck.core.s3;

/*
 * Copyright (c) 2002-2014 David Kocher. All rights reserved.
 * http://cyberduck.io/
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
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.io
 */

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledTranscriptListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.transfer.TransferStatus;

import org.jets3t.service.model.MultipartUpload;
import org.jets3t.service.model.S3Object;
import org.junit.Ignore;
import org.junit.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.Assert.*;

public class S3MultipartServiceTest extends AbstractTestCase {

    @Test
    public void testFindNotFound() throws Exception {
        final S3Session session = new S3Session(
                new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(),
                        new Credentials(
                                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
                        )));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path test = new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final MultipartUpload part = new S3MultipartService(session).find(test);
        assertNull(part);
    }

    @Test
    public void testFind() throws Exception {
        final S3Session session = new S3Session(
                new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(),
                        new Credentials(
                                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
                        )));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path file = new Path(container, UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final S3Object object = new S3WriteFeature(session).getDetails(file.getName(), new TransferStatus());
        final MultipartUpload first = session.getClient().multipartStartUpload(container.getName(), object);
        assertNotNull(first);
        // Make sure timestamp is later.
        Thread.sleep(2000L);
        final MultipartUpload second = session.getClient().multipartStartUpload(container.getName(), object);
        assertNotNull(second);
        final S3MultipartService service = new S3MultipartService(session);
        final MultipartUpload multipart = service.find(file);
        assertEquals(second.getUploadId(), multipart.getUploadId());
        assertNotNull(multipart);
        service.delete(first);
        service.delete(second);
    }

    @Test
    public void testFindKeyWithSpace() throws Exception {
        final S3Session session = new S3Session(
                new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(),
                        new Credentials(
                                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
                        )));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path container = new Path("test.cyberduck.ch", EnumSet.of(Path.Type.directory, Path.Type.volume));
        final Path test = new Path(container, "t f", EnumSet.of(Path.Type.file));
        final MultipartUpload part = new S3MultipartService(session).find(test);
        assertNull(part);
    }

    @Test
    @Ignore
    public void testAbort() throws Exception {
        final S3Session session = new S3Session(
                new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(),
                        new Credentials(
                                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
                        )));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        for(Path container : new S3BucketListService(session).list(new DisabledListProgressListener())) {
            final Path key = new Path(container, "/", EnumSet.of(Path.Type.file));
            MultipartUpload part;
            final S3MultipartService service = new S3MultipartService(session);
            while((part = service.find(key)) != null) {
                service.delete(part);
            }
        }
    }
}
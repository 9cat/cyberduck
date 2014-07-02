package ch.cyberduck.core.s3;

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

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.ProtocolFactory;

import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class S3BucketCreateServiceTest extends AbstractTestCase {

    @Test
    public void testCreate() throws Exception {
        final S3Session session = new S3Session(
                new Host(new S3Protocol(), new S3Protocol().getDefaultHostname(),
                        new Credentials(
                                properties.getProperty("s3.key"), properties.getProperty("s3.secret")
                        )));
        assertNotNull(session.open(new DisabledHostKeyCallback()));
        final S3FindFeature find = new S3FindFeature(session);
        final S3DefaultDeleteFeature delete = new S3DefaultDeleteFeature(session);
        final S3BucketCreateService create = new S3BucketCreateService(session);
        for(String region : ProtocolFactory.S3_SSL.getRegions()) {
            final Path bucket = new Path(UUID.randomUUID().toString(), EnumSet.of(Path.Type.directory, Path.Type.volume));
            create.create(bucket, region);
            bucket.attributes().setRegion(region);
            assertTrue(find.find(bucket));
            delete.delete(Collections.<Path>singletonList(bucket), new DisabledLoginController());
            assertFalse(find.find(bucket));
        }
        session.close();
    }
}

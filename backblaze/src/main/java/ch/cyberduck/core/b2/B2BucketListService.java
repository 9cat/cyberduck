package ch.cyberduck.core.b2;

/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
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
 */

import ch.cyberduck.core.Acl;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.RootListService;
import ch.cyberduck.core.exception.BackgroundException;

import org.apache.log4j.Logger;

import java.util.EnumSet;
import java.util.List;

import synapticloop.b2.exception.B2ApiException;
import synapticloop.b2.response.B2BucketResponse;

public class B2BucketListService implements RootListService {
    private static final Logger log = Logger.getLogger(B2BucketListService.class);

    private final PathContainerService containerService
            = new B2PathContainerService();

    private final B2Session session;

    public B2BucketListService(final B2Session session) {
        this.session = session;
    }

    @Override
    public List<Path> list(final ListProgressListener listener) throws BackgroundException {
        try {
            final AttributedList<Path> buckets = new AttributedList<Path>();
            for(B2BucketResponse bucket : session.getClient().listBuckets()) {
                final PathAttributes attributes = new PathAttributes();
                attributes.setVersionId(bucket.getBucketId());
                switch(bucket.getBucketType()) {
                    case allPublic:
                        attributes.setAcl(new Acl(new Acl.GroupUser(Acl.GroupUser.EVERYONE, false), new Acl.Role(Acl.Role.READ)));
                        break;
                }
                buckets.add(new Path(bucket.getBucketName(), EnumSet.of(Path.Type.directory, Path.Type.volume), attributes));
            }
            listener.chunk(new Path(String.valueOf(Path.DELIMITER), EnumSet.of(Path.Type.volume, Path.Type.directory)), buckets);
            return buckets;
        }
        catch(B2ApiException e) {
            throw new B2ExceptionMappingService().map("Listing directory {0} failed", e,
                    new Path(String.valueOf(Path.DELIMITER), EnumSet.of(Path.Type.volume, Path.Type.directory)));
        }
    }
}

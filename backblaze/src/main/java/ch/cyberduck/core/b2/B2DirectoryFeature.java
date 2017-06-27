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

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.Acl;
import ch.cyberduck.core.DefaultIOExceptionMappingService;
import ch.cyberduck.core.DisabledConnectionCallback;
import ch.cyberduck.core.MimeTypeService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.io.Checksum;
import ch.cyberduck.core.io.DefaultStreamCloser;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.io.input.NullInputStream;

import java.io.IOException;
import java.util.EnumSet;

import synapticloop.b2.BucketType;
import synapticloop.b2.exception.B2ApiException;
import synapticloop.b2.response.B2BucketResponse;
import synapticloop.b2.response.BaseB2Response;

public class B2DirectoryFeature implements Directory<BaseB2Response> {

    protected static final String PLACEHOLDER = "/.bzEmpty";

    private final PathContainerService containerService
            = new PathContainerService();

    private final B2Session session;
    private Write<BaseB2Response> writer;

    public B2DirectoryFeature(final B2Session session) {
        this(session, new B2WriteFeature(session));
    }

    public B2DirectoryFeature(final B2Session session, final B2WriteFeature writer) {
        this.session = session;
        this.writer = writer;
    }

    @Override
    public Path mkdir(final Path folder, final String region, final TransferStatus status) throws BackgroundException {
        try {
            if(containerService.isContainer(folder)) {
                final B2BucketResponse response = session.getClient().createBucket(containerService.getContainer(folder).getName(),
                        null == region ? BucketType.valueOf(PreferencesFactory.get().getProperty("b2.bucket.acl.default")) : BucketType.valueOf(region));
                switch(response.getBucketType()) {
                    case allPublic:
                        folder.attributes().setAcl(new Acl(new Acl.GroupUser(Acl.GroupUser.EVERYONE, false), new Acl.Role(Acl.Role.READ)));
                }
                return new Path(folder.getParent(), folder.getName(), folder.getType(),
                        new PathAttributes(folder.attributes()).withVersionId(response.getBucketId()));
            }
            else {
                if(Checksum.NONE == status.getChecksum()) {
                    status.setChecksum(writer.checksum().compute(new NullInputStream(0L), status));
                }
                status.setMime(MimeTypeService.DEFAULT_CONTENT_TYPE);
                new DefaultStreamCloser().close(writer.write(folder, status, new DisabledConnectionCallback()));
                final EnumSet<AbstractPath.Type> type = EnumSet.copyOf(folder.getType());
                type.add(Path.Type.placeholder);
                return new Path(folder.getParent(), folder.getName(), type, new PathAttributes(folder.attributes()));
            }
        }
        catch(B2ApiException e) {
            throw new B2ExceptionMappingService(session).map("Cannot create folder {0}", e, folder);
        }
        catch(IOException e) {
            throw new DefaultIOExceptionMappingService().map(e);
        }
    }

    @Override
    public boolean isSupported(final Path workdir) {
        return true;
    }

    @Override
    public B2DirectoryFeature withWriter(final Write<BaseB2Response> writer) {
        this.writer = writer;
        return this;
    }
}


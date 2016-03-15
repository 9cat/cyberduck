package ch.cyberduck.core.googledrive;

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
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.PathNormalizer;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.io.Checksum;
import ch.cyberduck.core.preferences.Preferences;
import ch.cyberduck.core.preferences.PreferencesFactory;

import java.io.IOException;
import java.util.EnumSet;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

public class DriveListService implements ListService {

    private Preferences preferences
            = PreferencesFactory.get();

    private final DriveSession session;

    public DriveListService(final DriveSession session) {
        this.session = session;
    }

    @Override
    public AttributedList<Path> list(final Path directory, final ListProgressListener listener) throws
            BackgroundException {
        try {
            final AttributedList<Path> children = new AttributedList<Path>();
            String page = null;
            do {
                final Drive.Files.List list = session.getClient().files().list()
                        .setQ(String.format("'%s' in parents", directory.isRoot() ? "root" : new DriveFileidProvider().getFileid(directory)))
                        .setOauthToken(session.getAccessToken())
                        .setPageToken(page)
                        .setFields("files")
                        .setPageSize(preferences.getInteger("google.drive.list.limit"));
                for(File f : list.execute().getFiles()) {
                    final PathAttributes attributes = new PathAttributes();
                    if(f.getExplicitlyTrashed()) {
                        continue;
                    }
                    if(null != f.getQuotaBytesUsed()) {
                        attributes.setSize(f.getQuotaBytesUsed());
                    }
                    if(null != f.getSize()) {
                        attributes.setSize(f.getSize());
                    }
                    attributes.setVersionId(f.getId());
                    if(f.getModifiedTime() != null) {
                        attributes.setModificationDate(f.getModifiedTime().getValue());
                    }
                    if(f.getCreatedTime() != null) {
                        attributes.setCreationDate(f.getCreatedTime().getValue());
                    }
                    attributes.setChecksum(Checksum.parse(f.getMd5Checksum()));
                    final EnumSet<AbstractPath.Type> type = "application/vnd.google-apps.folder".equals(
                            f.getMimeType()) ? EnumSet.of(Path.Type.directory) : EnumSet.of(Path.Type.file);
                    final Path child = new Path(directory, PathNormalizer.name(f.getName()), type, attributes);
                    children.add(child);
                }
                listener.chunk(directory, children);
                page = list.getPageToken();
            }
            while(page != null);
            return children;
        }
        catch(IOException e) {
            throw new DriveExceptionMappingService().map("Listing directory failed", e, directory);
        }
    }
}

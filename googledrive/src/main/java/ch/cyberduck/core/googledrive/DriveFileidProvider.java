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

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.SimplePathPredicate;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.IdProvider;

import org.apache.commons.lang3.StringUtils;

public class DriveFileidProvider implements IdProvider {

    private final DriveSession session;
    private Cache<Path> cache = PathCache.empty();

    public DriveFileidProvider(final DriveSession session) {
        this.session = session;
    }

    @Override
    public String getFileid(final Path file, final ListProgressListener listener) throws BackgroundException {
        if(StringUtils.isNotBlank(file.attributes().getVersionId())) {
            return file.attributes().getVersionId();
        }
        if(file.isRoot()) {
            return DriveHomeFinderService.ROOT_FOLDER_ID;
        }
        final AttributedList<Path> list;
        if(!cache.isCached(file.getParent())) {
            list = new FileidDriveListService(file).list(file.getParent(), new DisabledListProgressListener());
            cache.put(file.getParent(), list);
        }
        else {
            list = cache.get(file.getParent());
        }
        final Path found = list.find(new SimplePathPredicate(file));
        if(null == found) {
            throw new NotfoundException(file.getAbsolute());
        }
        return found.attributes().getVersionId();
    }

    @Override
    public IdProvider withCache(final Cache<Path> cache) {
        this.cache = cache;
        return this;
    }

    private final class FileidDriveListService extends AbstractDriveListService {
        private final Path file;

        public FileidDriveListService(final Path file) {
            super(DriveFileidProvider.this.session, 1);
            this.file = file;
        }

        @Override
        protected String query(final Path directory, final ListProgressListener listener) throws BackgroundException {
            return String.format("name = '%s' and '%s' in parents", file.getName(), DriveFileidProvider.this.getFileid(directory, new DisabledListProgressListener()));
        }
    }
}

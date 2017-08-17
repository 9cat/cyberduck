package ch.cyberduck.core.shared;

/*
 * Copyright (c) 2013 David Kocher. All rights reserved.
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
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.ch
 */

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.NullFilter;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.SimplePathPredicate;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.Find;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class DefaultFindFeature implements Find {
    private static final Logger log = Logger.getLogger(DefaultFindFeature.class);

    private final Session<?> session;

    private Cache<Path> cache
            = PathCache.empty();

    public DefaultFindFeature(final Session<?> session) {
        this.session = session;
    }

    @Override
    public boolean find(final Path file) throws BackgroundException {
        if(file.isRoot()) {
            return true;
        }
        try {
            final AttributedList<Path> list;
            if(!cache.isCached(file.getParent())) {
                list = session.list(file.getParent(), new DisabledListProgressListener());
                cache.put(file.getParent(), list);
            }
            else {
                list = cache.get(file.getParent());
            }
            final Path found = list.filter(new NullFilter<>()).find(
                    session.getCase() == Session.Case.insensitive ? new CaseInsensitivePathPredicate(file) : new SimplePathPredicate(file));
            return found != null;
        }
        catch(NotfoundException e) {
            return false;
        }
    }

    @Override
    public DefaultFindFeature withCache(final Cache<Path> cache) {
        this.cache = cache;
        return this;
    }

    private final class CaseInsensitivePathPredicate extends SimplePathPredicate {
        public CaseInsensitivePathPredicate(final Path file) {
            super(file);
        }

        @Override
        public String toString() {
            return this.type() + "-" + StringUtils.lowerCase(file.getAbsolute());
        }

        @Override
        public boolean test(final Path test) {
            return this.hashCode() == new CaseInsensitivePathPredicate(test).hashCode();
        }
    }
}

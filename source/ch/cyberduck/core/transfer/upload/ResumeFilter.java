package ch.cyberduck.core.transfer.upload;

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

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.symlink.SymlinkResolver;

import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public class ResumeFilter extends AbstractUploadFilter {
    private static final Logger log = Logger.getLogger(ResumeFilter.class);

    private Write write;

    private Cache cache = Cache.empty();

    public ResumeFilter(final SymlinkResolver<Local> symlinkResolver, final Session<?> session) {
        this(symlinkResolver, session, new UploadFilterOptions());
    }

    public ResumeFilter(final SymlinkResolver<Local> symlinkResolver, final Session<?> session,
                        final UploadFilterOptions options) {
        this(symlinkResolver, session, options, session.getFeature(Write.class));
    }

    public ResumeFilter(final SymlinkResolver<Local> symlinkResolver, final Session<?> session,
                        final UploadFilterOptions options, final Write write) {
        super(symlinkResolver, session, options);
        this.write = write;
    }

    @Override
    public AbstractUploadFilter withCache(final Cache cache) {
        this.cache = cache;
        return super.withCache(cache);
    }

    @Override
    public boolean accept(final Path file, final Local local, final TransferStatus parent) throws BackgroundException {
        if(super.accept(file, local, parent)) {
            if(local.isFile()) {
                if(parent.isExists()) {
                    final Write.Append append = write.append(file, local.attributes().getSize(), cache);
                    if(append.append && append.size >= local.attributes().getSize()) {
                        if(log.isInfoEnabled()) {
                            log.info(String.format("Skip file %s with remote size %d", file, append.size));
                        }
                        // No need to resume completed transfers
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public TransferStatus prepare(final Path file, final Local local, final TransferStatus parent) throws BackgroundException {
        final TransferStatus status = super.prepare(file, local, parent);
        if(local.isFile()) {
            if(parent.isExists()) {
                final Write.Append append = write.append(file, status.getLength(), cache);
                if(append.append && append.size <= local.attributes().getSize()) {
                    // Append to existing file
                    status.setAppend(true);
                    status.setCurrent(append.size);
                }
            }
        }
        return status;
    }
}
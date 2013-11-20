package ch.cyberduck.core.transfer.download;

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

import ch.cyberduck.core.Local;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.symlink.SymlinkResolver;

import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public class ResumeFilter extends AbstractDownloadFilter {
    private static final Logger log = Logger.getLogger(ResumeFilter.class);

    private Read read;

    public ResumeFilter(final SymlinkResolver symlinkResolver, final Session<?> session) {
        super(symlinkResolver, session, new DownloadFilterOptions());
        this.read = session.getFeature(Read.class);
    }

    public ResumeFilter(final SymlinkResolver symlinkResolver, final Session<?> session,
                        final DownloadFilterOptions options, final Read read) {
        super(symlinkResolver, session, options);
        this.read = read;
    }

    @Override
    public boolean accept(final Path file, final TransferStatus parent) throws BackgroundException {
        if(file.attributes().isFile()) {
            if(file.getLocal().exists()) {
                final long local = file.getLocal().attributes().getSize();
                if(local >= file.attributes().getSize()) {
                    if(log.isInfoEnabled()) {
                        log.info(String.format("Skip file %s with local size %d", file, local));
                    }
                    // No need to resume completed transfers
                    return false;
                }
            }
        }
        return super.accept(file, parent);
    }

    @Override
    public TransferStatus prepare(final Path file, final TransferStatus parent) throws BackgroundException {
        final TransferStatus status = super.prepare(file, parent);
        if(read.append(file)) {
            if(file.attributes().isFile()) {
                final Local local = file.getLocal();
                if(local.exists()) {
                    if(local.attributes().getSize() > 0) {
                        status.setAppend(true);
                        status.setCurrent(local.attributes().getSize());
                    }
                }
            }
        }
        return status;
    }
}
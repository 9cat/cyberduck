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

import ch.cyberduck.core.Local;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.symlink.SymlinkResolver;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public class RenameFilter extends AbstractUploadFilter {
    private static final Logger log = Logger.getLogger(RenameFilter.class);

    public RenameFilter(final SymlinkResolver symlinkResolver, final Session<?> session) {
        this(symlinkResolver, session, new UploadFilterOptions());
    }

    public RenameFilter(final SymlinkResolver symlinkResolver, final Session<?> session,
                        final UploadFilterOptions options) {
        super(symlinkResolver, session, options);
    }

    @Override
    public TransferStatus prepare(final Path file, final Local local, final TransferStatus parent) throws BackgroundException {
        final TransferStatus status = super.prepare(file, local, parent);
        if(parent.isExists()) {
            final Path parentPath = file.getParent();
            final String filename = file.getName();
            int no = 0;
            if(find.find(file)) {
                do {
                    no++;
                    String proposal = String.format("%s-%d", FilenameUtils.getBaseName(filename), no);
                    if(StringUtils.isNotBlank(FilenameUtils.getExtension(filename))) {
                        proposal += String.format(".%s", FilenameUtils.getExtension(filename));
                    }
                    final Path renamed = new Path(parentPath, proposal,
                            new PathAttributes(file.attributes().getType()));
                    status.rename(renamed);
                    if(log.isInfoEnabled()) {
                        log.info(String.format("Change filename from %s to %s", file, renamed));
                    }
                }
                while(find.find(status.getRename().remote));
            }
        }
        return status;
    }
}

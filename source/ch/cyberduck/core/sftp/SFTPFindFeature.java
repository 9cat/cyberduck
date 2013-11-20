package ch.cyberduck.core.sftp;

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
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.Find;

/**
 * @version $Id$
 */
public class SFTPFindFeature implements Find {

    private SFTPAttributesFeature attributes;

    public SFTPFindFeature(final SFTPSession session) {
        this.attributes = new SFTPAttributesFeature(session);
    }

    @Override
    public boolean find(final Path file) throws BackgroundException {
        if(file.isRoot()) {
            return true;
        }
        try {
            return attributes.find(file) != null;
        }
        catch(NotfoundException e) {
            // We expect SSH_FXP_STATUS if the file is not found
            return false;
        }
    }

    @Override
    public Find withCache(final Cache cache) {
        this.attributes.withCache(cache);
        return this;
    }
}

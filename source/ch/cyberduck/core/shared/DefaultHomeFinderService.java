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

import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathNormalizer;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Home;

import org.apache.commons.lang3.StringUtils;

/**
 * @version $Id$
 */
public class DefaultHomeFinderService implements Home {

    private Session session;

    public DefaultHomeFinderService(final Session session) {
        this.session = session;
    }

    @Override
    public Path find() throws BackgroundException {
        final Host host = session.getHost();
        if(host.getWorkdir() != null) {
            return host.getWorkdir();
        }
        else {
            final String path = host.getDefaultPath();
            if(StringUtils.isNotBlank(path)) {
                return this.find(session.workdir(), path);
            }
            else {
                // No default path configured
                return session.workdir();
            }
        }
    }

    @Override
    public Path find(final Path workdir, final String path) {
        if(path.startsWith(String.valueOf(Path.DELIMITER))) {
            // Mount absolute path
            final String normalized = PathNormalizer.normalize(path);
            return new Path(normalized,
                    normalized.equals(String.valueOf(Path.DELIMITER)) ? Path.VOLUME_TYPE | Path.DIRECTORY_TYPE : Path.DIRECTORY_TYPE);
        }
        else {
            if(path.startsWith(Path.HOME)) {
                // Relative path to the home directory
                return new Path(workdir, PathNormalizer.normalize(StringUtils.removeStart(
                        StringUtils.removeStart(path, Path.HOME), String.valueOf(Path.DELIMITER)), false), Path.DIRECTORY_TYPE);
            }
            else {
                // Relative path
                return new Path(workdir, PathNormalizer.normalize(path), Path.DIRECTORY_TYPE);
            }
        }
    }
}

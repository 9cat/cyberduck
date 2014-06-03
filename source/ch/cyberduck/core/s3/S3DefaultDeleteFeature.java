package ch.cyberduck.core.s3;

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

import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.Delete;

import org.apache.log4j.Logger;
import org.jets3t.service.ServiceException;

import java.text.MessageFormat;
import java.util.List;

/**
 * @version $Id$
 */
public class S3DefaultDeleteFeature implements Delete {
    private static final Logger log = Logger.getLogger(S3DefaultDeleteFeature.class);

    private S3Session session;

    private PathContainerService containerService
            = new S3PathContainerService();

    public S3DefaultDeleteFeature(final S3Session session) {
        this.session = session;
    }

    public void delete(final List<Path> files, final LoginCallback prompt) throws BackgroundException {
        for(Path file : files) {
            if(containerService.isContainer(file)) {
                continue;
            }
            session.message(MessageFormat.format(LocaleFactory.localizedString("Deleting {0}", "Status"),
                    file.getName()));
            try {
                try {
                    if(file.isDirectory()) {
                        // Because we normalize paths and remove a trailing delimiter we add it here again as the
                        // default directory placeholder formats has the format `/placeholder/' as a key.
                        session.getClient().deleteObject(containerService.getContainer(file).getName(), containerService.getKey(file) + Path.DELIMITER);
                        // Always returning 204 even if the key does not exist.
                        // Fallback to legacy directory placeholders with metadata instead of key with trailing delimiter
                        session.getClient().deleteObject(containerService.getContainer(file).getName(), containerService.getKey(file));
                        // AWS does not return 404 for non-existing keys
                    }
                    else {
                        session.getClient().deleteObject(containerService.getContainer(file).getName(), containerService.getKey(file));
                    }
                }
                catch(ServiceException e) {
                    throw new ServiceExceptionMappingService().map("Cannot delete {0}", e, file);
                }
            }
            catch(NotfoundException e) {
                // No real placeholder but just a delimiter returned in the object listing.
                log.warn(e.getMessage());
            }
        }
        for(Path file : files) {
            if(containerService.isContainer(file)) {
                session.message(MessageFormat.format(LocaleFactory.localizedString("Deleting {0}", "Status"),
                        file.getName()));
                // Finally delete bucket itself
                try {
                    session.getClient().deleteBucket(containerService.getContainer(file).getName());
                }
                catch(ServiceException e) {
                    throw new ServiceExceptionMappingService().map("Cannot delete {0}", e, file);
                }
            }
        }
    }
}

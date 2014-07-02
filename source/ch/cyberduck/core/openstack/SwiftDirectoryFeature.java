package ch.cyberduck.core.openstack;

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

import ch.cyberduck.core.DefaultIOExceptionMappingService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Directory;

import java.io.IOException;

import ch.iterate.openstack.swift.exception.GenericException;

/**
 * @version $Id$
 */
public class SwiftDirectoryFeature implements Directory {

    private SwiftSession session;

    private PathContainerService containerService
            = new SwiftPathContainerService();

    private SwiftRegionService regionService;

    public SwiftDirectoryFeature(final SwiftSession session) {
        this.session = session;
        this.regionService = new SwiftRegionService(session);
    }

    @Override
    public void mkdir(final Path file) throws BackgroundException {
        this.mkdir(file, null);
    }

    @Override
    public void mkdir(final Path file, final String region) throws BackgroundException {
        try {
            if(containerService.isContainer(file)) {
                // Create container at top level
                session.getClient().createContainer(regionService.lookup(region), file.getName());
            }
            else {
                // Create virtual directory. Use region of parent container.
                new SwiftTouchFeature(session).touch(file);
            }
        }
        catch(GenericException e) {
            throw new SwiftExceptionMappingService().map("Cannot create folder {0}", e, file);
        }
        catch(IOException e) {
            throw new DefaultIOExceptionMappingService().map("Cannot create folder {0}", e, file);
        }
    }
}

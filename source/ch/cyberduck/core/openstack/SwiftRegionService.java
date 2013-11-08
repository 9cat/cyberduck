package ch.cyberduck.core.openstack;

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

import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import ch.iterate.openstack.swift.model.Region;

/**
 * @version $Id$
 */
public class SwiftRegionService {
    private static final Logger log = Logger.getLogger(SwiftRegionService.class);

    private SwiftSession session;

    public SwiftRegionService(final SwiftSession session) {
        this.session = session;
    }

    public Region lookup(final Path file) throws BackgroundException {
        return this.lookup(file.attributes().getRegion());
    }

    public Region lookup(final String location) throws BackgroundException {
        if(null == session.getClient()) {
            log.warn("Cannot determine region if not connected");
            return null;
        }
        for(Region region : session.getClient().getRegions()) {
            if(StringUtils.isBlank(region.getRegionId())) {
                continue;
            }
            if(region.getRegionId().equals(location)) {
                return region;
            }
        }
        log.warn(String.format("Unknown region %s in authentication context", location));
        if(session.getClient().getRegions().isEmpty()) {
            throw new InteroperabilityException("No region found in authentication context");
        }
        final Region region = session.getClient().getRegions().iterator().next();
        log.warn(String.format("Fallback to first region found %s", region.getRegionId()));
        if(null == region.getStorageUrl()) {
            throw new InteroperabilityException(String.format("No storage endpoint found for region %s", region.getRegionId()));
        }
        if(null == region.getCDNManagementUrl()) {
            log.warn(String.format("No CDN management endpoint found for region %s", region.getRegionId()));
        }
        return region;
    }
}

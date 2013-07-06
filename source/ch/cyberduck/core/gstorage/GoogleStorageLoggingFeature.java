package ch.cyberduck.core.gstorage;

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
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ServiceExceptionMappingService;
import ch.cyberduck.core.logging.LoggingConfiguration;
import ch.cyberduck.core.s3.S3LoggingFeature;

import org.apache.commons.lang.StringUtils;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.acl.gs.GroupByEmailAddressGrantee;
import org.jets3t.service.model.GSBucketLoggingStatus;

/**
 * @version $Id:$
 */
public class GoogleStorageLoggingFeature extends S3LoggingFeature {

    final GSSession session;

    public GoogleStorageLoggingFeature(final GSSession session) {
        super(session);
        this.session = session;
    }

    @Override
    public void setConfiguration(final Path container, final LoggingConfiguration configuration) throws BackgroundException {
        try {
            // Logging target bucket
            final GSBucketLoggingStatus status = new GSBucketLoggingStatus(
                    StringUtils.isNotBlank(configuration.getLoggingTarget()) ? configuration.getLoggingTarget() : container.getName(), null);
            if(configuration.isEnabled()) {
                status.setLogfilePrefix(Preferences.instance().getProperty("google.logging.prefix"));
            }
            // Grant write for Google to logging target bucket
            final AccessControlList acl = session.getClient().getBucketAcl(container.getName());
            final GroupByEmailAddressGrantee grantee = new GroupByEmailAddressGrantee(
                    "cloud-storage-analytics@google.com");
            if(!acl.getPermissionsForGrantee(grantee).contains(Permission.PERMISSION_WRITE)) {
                acl.grantPermission(grantee, Permission.PERMISSION_WRITE);
                session.getClient().putBucketAcl(container.getName(), acl);
            }
            session.getClient().setBucketLoggingStatusImpl(container.getName(), status);
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map("Cannot write file attributes", e);
        }
    }
}

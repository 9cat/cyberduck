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

import ch.cyberduck.core.Path;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Logging;
import ch.cyberduck.core.logging.LoggingConfiguration;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3BucketLoggingStatus;
import org.jets3t.service.model.StorageBucketLoggingStatus;

/**
 * @version $Id$
 */
public class S3LoggingFeature implements Logging {
    private static final Logger log = Logger.getLogger(S3LoggingFeature.class);

    private S3Session session;

    public S3LoggingFeature(final S3Session session) {
        this.session = session;
    }

    @Override
    public LoggingConfiguration getConfiguration(final Path container) throws BackgroundException {
        if(session.getHost().getCredentials().isAnonymousLogin()) {
            log.info("Anonymous cannot access logging status");
            return new LoggingConfiguration(false);
        }
        try {
            final StorageBucketLoggingStatus status
                    = session.getClient().getBucketLoggingStatusImpl(container.getName());
            return new LoggingConfiguration(status.isLoggingEnabled(),
                    status.getTargetBucketName());
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map("Cannot read container configuration", e);
        }
    }


    @Override
    public void setConfiguration(final Path container, final LoggingConfiguration configuration) throws BackgroundException {
        try {
            // Logging target bucket
            final S3BucketLoggingStatus status = new S3BucketLoggingStatus(
                    StringUtils.isNotBlank(configuration.getLoggingTarget()) ? configuration.getLoggingTarget() : container.getName(), null);
            if(configuration.isEnabled()) {
                status.setLogfilePrefix(Preferences.instance().getProperty("s3.logging.prefix"));
            }
            session.getClient().setBucketLoggingStatus(container.getName(), status, true);
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map("Cannot write file attributes", e);
        }
    }
}

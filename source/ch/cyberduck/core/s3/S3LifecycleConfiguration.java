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
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ServiceExceptionMappingService;
import ch.cyberduck.core.features.Lifecycle;
import ch.cyberduck.core.lifecycle.LifecycleConfiguration;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.LifecycleConfig;

import java.util.UUID;

/**
 * @version $Id:$
 */
public class S3LifecycleConfiguration implements Lifecycle {
    private static final Logger log = Logger.getLogger(S3LifecycleConfiguration.class);

    private S3Session session;

    public S3LifecycleConfiguration(final S3Session session) {
        this.session = session;
    }

    @Override
    public void setConfiguration(final Path container, final LifecycleConfiguration configuration) throws BackgroundException {
        try {
            if(configuration.getTransition() != null || configuration.getExpiration() != null) {
                final LifecycleConfig config = new LifecycleConfig();
                // Unique identifier for the rule. The value cannot be longer than 255 characters. When you specify an empty prefix, the rule applies to all objects in the bucket
                final LifecycleConfig.Rule rule = config.newRule(UUID.randomUUID().toString(), StringUtils.EMPTY, true);
                if(configuration.getTransition() != null) {
                    rule.newTransition().setDays(configuration.getTransition());
                }
                if(configuration.getExpiration() != null) {
                    rule.newExpiration().setDays(configuration.getExpiration());
                }
                session.getClient().setLifecycleConfig(container.getName(), config);
            }
            else {
                session.getClient().deleteLifecycleConfig(container.getName());
            }
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map("Cannot read container configuration", e);
        }
    }


    @Override
    public LifecycleConfiguration getConfiguration(final Path container) throws BackgroundException {
        if(session.getHost().getCredentials().isAnonymousLogin()) {
            log.info("Anonymous cannot access logging status");
            return new LifecycleConfiguration();
        }
        try {
            final LifecycleConfig status = session.getClient().getLifecycleConfig(container.getName());
            if(null != status) {
                Integer transition = null;
                Integer expiration = null;
                for(LifecycleConfig.Rule rule : status.getRules()) {
                    if(rule.getTransition() != null) {
                        transition = rule.getTransition().getDays();
                    }
                    if(rule.getExpiration() != null) {
                        expiration = rule.getExpiration().getDays();
                    }
                }
                return new LifecycleConfiguration(transition, expiration);
            }
            return new LifecycleConfiguration();
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map(e);
        }
    }

}

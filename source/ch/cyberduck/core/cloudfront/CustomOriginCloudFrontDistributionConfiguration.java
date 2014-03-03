package ch.cyberduck.core.cloudfront;

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
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.DefaultHostKeyController;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.PathNormalizer;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.cdn.Distribution;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.s3.S3Session;

import org.apache.log4j.Logger;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @version $Id$
 */
public class CustomOriginCloudFrontDistributionConfiguration extends CloudFrontDistributionConfiguration {
    private static final Logger log = Logger.getLogger(CustomOriginCloudFrontDistributionConfiguration.class);

    private Host origin;

    private PathContainerService containerService
            = new PathContainerService();

    public CustomOriginCloudFrontDistributionConfiguration(final Host origin) {
        // Configure with the same host as S3 to get the same credentials from the keychain.
        super(new S3Session(new Host(ProtocolFactory.S3_SSL, ProtocolFactory.S3_SSL.getDefaultHostname(), origin.getCdnCredentials())));
        this.origin = origin;
    }

    private static interface Connected<T> extends Callable<T> {
        T call() throws BackgroundException;
    }

    private <T> T connected(final Connected<T> run) throws BackgroundException {
        if(!session.isConnected()) {
            session.open(new DefaultHostKeyController());
        }
        return run.call();
    }


    @Override
    public Distribution read(final Path container, final Distribution.Method method, final LoginCallback prompt) throws BackgroundException {
        return this.connected(new Connected<Distribution>() {
            @Override
            public Distribution call() throws BackgroundException {
                return CustomOriginCloudFrontDistributionConfiguration.super.read(container, method, prompt);
            }
        });
    }

    @Override
    public void write(final Path container, final Distribution distribution, final LoginCallback prompt) throws BackgroundException {
        this.connected(new Connected<Void>() {
            @Override
            public Void call() throws BackgroundException {
                CustomOriginCloudFrontDistributionConfiguration.super.write(container, distribution, prompt);
                return null;
            }
        });
    }

    @Override
    public List<Distribution.Method> getMethods(final Path container) {
        return Arrays.asList(Distribution.CUSTOM);
    }

    @Override
    protected URI getOrigin(final Path container, final Distribution.Method method) {
        final URI url = URI.create(String.format("%s%s", origin.getWebURL(), PathNormalizer.normalize(origin.getDefaultPath(), true)));
        if(log.isDebugEnabled()) {
            log.debug(String.format("Use origin %s for distribution %s", url, method));
        }
        return url;
    }
}
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

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginController;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.features.Versioning;
import ch.cyberduck.core.versioning.VersioningConfiguration;

import org.apache.log4j.Logger;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3BucketVersioningStatus;
import org.jets3t.service.model.S3Object;

/**
 * @version $Id$
 */
public class S3VersioningFeature implements Versioning {
    private static final Logger log = Logger.getLogger(S3VersioningFeature.class);

    private S3Session session;

    public S3VersioningFeature(final S3Session session) {
        this.session = session;
    }

    @Override
    public void setConfiguration(final Path container, final LoginController prompt, final VersioningConfiguration configuration) throws BackgroundException {
        try {
            final VersioningConfiguration current = this.getConfiguration(container);
            if(current.isMultifactor()) {
                // The bucket is already MFA protected.
                final Credentials factor = this.getToken(prompt);
                if(configuration.isEnabled()) {
                    if(current.isEnabled()) {
                        log.debug("Versioning already enabled for bucket " + container);
                    }
                    else {
                        // Enable versioning if not already active.
                        log.debug("Enable bucket versioning with MFA " + factor.getUsername() + " for " + container);
                        session.getClient().enableBucketVersioningWithMFA(container.getName(),
                                factor.getUsername(), factor.getPassword());
                    }
                }
                else {
                    log.debug("Suspend bucket versioning with MFA " + factor.getUsername() + " for " + container);
                    session.getClient().suspendBucketVersioningWithMFA(container.getName(),
                            factor.getUsername(), factor.getPassword());
                }
                if(configuration.isEnabled() && !configuration.isMultifactor()) {
                    log.debug(String.format("Disable MFA %s for %s", factor.getUsername(), container));
                    // User has choosen to disable MFA
                    final Credentials factor2 = this.getToken(prompt);
                    session.getClient().disableMFAForVersionedBucket(container.getName(),
                            factor2.getUsername(), factor2.getPassword());
                }
            }
            else {
                if(configuration.isEnabled()) {
                    if(configuration.isMultifactor()) {
                        final Credentials factor = this.getToken(prompt);
                        log.debug(String.format("Enable bucket versioning with MFA %s for %s", factor.getUsername(), container));
                        session.getClient().enableBucketVersioningWithMFA(container.getName(),
                                factor.getUsername(), factor.getPassword());
                    }
                    else {
                        if(current.isEnabled()) {
                            log.debug(String.format("Versioning already enabled for bucket %s", container));
                        }
                        else {
                            log.debug(String.format("Enable bucket versioning for %s", container));
                            session.getClient().enableBucketVersioning(container.getName());
                        }
                    }
                }
                else {
                    log.debug(String.format("Susped bucket versioning for %s", container));
                    session.getClient().suspendBucketVersioning(container.getName());
                }
            }
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map("Cannot write file attributes", e);
        }
    }

    @Override
    public VersioningConfiguration getConfiguration(final Path container) throws BackgroundException {
        try {
            final S3BucketVersioningStatus status
                    = session.getClient().getBucketVersioningStatus(container.getName());

            return new VersioningConfiguration(status.isVersioningEnabled(),
                    status.isMultiFactorAuthDeleteRequired());
        }
        catch(ServiceException e) {
            try {
                throw new ServiceExceptionMappingService().map("Cannot read container configuration", e);
            }
            catch(AccessDeniedException l) {
                log.warn(String.format("Missing permission to read versioning configuration for %s %s", container, e.getMessage()));
                return VersioningConfiguration.empty();
            }
        }
    }

    /**
     * Versioning support. Copy a previous version of the object into the same bucket.
     * The copied object becomes the latest version of that object and all object versions are preserved.
     */
    @Override
    public void revert(final Path file) throws BackgroundException {
        if(file.attributes().isFile()) {
            try {
                final PathContainerService containerService = new PathContainerService();
                final S3Object destination = new S3Object(containerService.getKey(file));
                // Keep same storage class
                destination.setStorageClass(file.attributes().getStorageClass());
                // Keep encryption setting
                destination.setServerSideEncryptionAlgorithm(file.attributes().getEncryption());
                // Apply non standard ACL
                final S3AccessControlListFeature acl = new S3AccessControlListFeature(session);
                destination.setAcl(acl.convert(acl.getPermission(file)));
                session.getClient().copyVersionedObject(file.attributes().getVersionId(),
                        containerService.getContainer(file).getName(), containerService.getKey(file), containerService.getContainer(file).getName(), destination, false);
            }
            catch(ServiceException e) {
                throw new ServiceExceptionMappingService().map("Cannot revert file", e, file);
            }
        }
    }

    /**
     * Prompt for MFA credentials
     *
     * @param controller Prompt controller
     * @return MFA one time authentication password.
     * @throws ch.cyberduck.core.exception.ConnectionCanceledException
     *          Prompt dismissed
     */
    protected Credentials getToken(final LoginController controller) throws ConnectionCanceledException {
        final Credentials credentials = new Credentials(
                Preferences.instance().getProperty("s3.mfa.serialnumber"), null, false) {
            @Override
            public String getUsernamePlaceholder() {
                return LocaleFactory.localizedString("MFA Serial Number", "S3");
            }

            @Override
            public String getPasswordPlaceholder() {
                return LocaleFactory.localizedString("MFA Authentication Code", "S3");
            }
        };
        // Prompt for MFA credentials.
        controller.prompt(session.getHost().getProtocol(), credentials,
                LocaleFactory.localizedString("Provide additional login credentials", "Credentials"),
                LocaleFactory.localizedString("Multi-Factor Authentication", "S3"), new LoginOptions());

        Preferences.instance().setProperty("s3.mfa.serialnumber", credentials.getUsername());
        return credentials;
    }

}

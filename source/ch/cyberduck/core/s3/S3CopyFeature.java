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

import ch.cyberduck.core.Acl;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Copy;

import org.jets3t.service.ServiceException;
import org.jets3t.service.model.StorageObject;

/**
 * @version $Id$
 */
public class S3CopyFeature implements Copy {

    private S3Session session;

    private PathContainerService containerService
            = new PathContainerService();

    public S3CopyFeature(final S3Session session) {
        this.session = session;
    }

    @Override
    public void copy(final Path source, final Path copy) throws BackgroundException {
        if(source.isFile()) {
            // Keep same storage class
            final String storageClass = source.attributes().getStorageClass();
            // Keep encryption setting
            final String encryptionAlgorithm = source.attributes().getEncryption();
            // Apply non standard ACL
            final S3AccessControlListFeature accessControlListFeature = new S3AccessControlListFeature(session);
            final Acl acl = accessControlListFeature.getPermission(source);
            this.copy(source, copy, storageClass, encryptionAlgorithm, acl);
        }
    }

    protected void copy(final Path source, final Path copy, final String storageClass, final String encryptionAlgorithm,
                        final Acl acl) throws BackgroundException {
        if(source.isFile()) {
            final StorageObject destination = new StorageObject(containerService.getKey(copy));
            destination.setStorageClass(storageClass);
            destination.setServerSideEncryptionAlgorithm(encryptionAlgorithm);
            destination.setAcl(new S3AccessControlListFeature(session).convert(acl));
            try {
                // Copying object applying the metadata of the original
                session.getClient().copyObject(containerService.getContainer(source).getName(),
                        containerService.getKey(source),
                        containerService.getContainer(copy).getName(), destination, false);
            }
            catch(ServiceException e) {
                throw new ServiceExceptionMappingService().map("Cannot copy {0}", e, source);
            }
        }
    }
}

package ch.cyberduck.core.s3;

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
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.AbstractIOExceptionMappingService;
import ch.cyberduck.core.DefaultIOExceptionMappingService;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.exception.NotfoundException;

import org.apache.http.HttpStatus;
import org.jets3t.service.ServiceException;

import java.io.IOException;

/**
 * @version $Id$
 */
public class ServiceExceptionMappingService extends AbstractIOExceptionMappingService<ServiceException> {

    @Override
    public BackgroundException map(final ServiceException e) {
        final StringBuilder buffer = new StringBuilder();
        if(e.isParsedFromXmlMessage()) {
            // S3 protocol message
            this.append(buffer, e.getErrorMessage());
            if(HttpStatus.SC_BAD_REQUEST == e.getResponseCode()) {
                return new InteroperabilityException(buffer.toString(), e);
            }
            if(HttpStatus.SC_FORBIDDEN == e.getResponseCode()) {
                if(e.getErrorCode().equals("SignatureDoesNotMatch")) {
                    return new LoginFailureException(buffer.toString(), e);
                }
                if(e.getErrorCode().equals("InvalidAccessKeyId")) {
                    return new LoginFailureException(buffer.toString(), e);
                }
                if(e.getErrorCode().equals("InvalidClientTokenId")) {
                    return new LoginFailureException(buffer.toString(), e);
                }
                if(e.getErrorCode().equals("InvalidSecurity")) {
                    return new LoginFailureException(buffer.toString(), e);
                }
                if(e.getErrorCode().equals("MissingClientTokenId")) {
                    return new LoginFailureException(buffer.toString(), e);
                }
                if(e.getErrorCode().equals("MissingAuthenticationToken")) {
                    return new LoginFailureException(buffer.toString(), e);
                }
                return new AccessDeniedException(buffer.toString(), e);
            }
            if(HttpStatus.SC_NOT_FOUND == e.getResponseCode()) {
                return new NotfoundException(buffer.toString(), e);
            }
            if(HttpStatus.SC_UNAUTHORIZED == e.getResponseCode()) {
                // Actually never returned by S3 but always 403
                return new LoginFailureException(buffer.toString(), e);
            }
            return this.wrap(e, buffer);
        }
        else {
            if(e.getCause() instanceof IOException) {
                return new DefaultIOExceptionMappingService().map((IOException) e.getCause());
            }
            if(null == e.getCause()) {
                this.append(buffer, e.getMessage());
            }
            else {
                this.append(buffer, e.getCause().getMessage());
            }
            return this.wrap(e, buffer);
        }
    }
}
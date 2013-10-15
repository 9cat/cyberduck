package ch.cyberduck.core.iam;

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

import ch.cyberduck.core.AbstractIOExceptionMappingService;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.exception.NotfoundException;

import org.apache.http.HttpStatus;

import com.amazonaws.AmazonServiceException;

/**
 * @version $Id$
 */
public class AmazonServiceExceptionMappingService extends AbstractIOExceptionMappingService<AmazonServiceException> {

    @Override
    public BackgroundException map(final AmazonServiceException e) {
        final StringBuilder buffer = new StringBuilder();
        this.append(buffer, e.getMessage());
        this.append(buffer, e.getErrorCode());
        if(e.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
            return new InteroperabilityException(buffer.toString(), e);
        }
        if(e.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED) {
            return new InteroperabilityException(buffer.toString(), e);
        }
        if(e.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
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
        if(e.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            return new LoginFailureException(buffer.toString(), e);
        }
        if(e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return new NotfoundException(buffer.toString(), e);
        }
        return this.wrap(e, buffer);
    }
}

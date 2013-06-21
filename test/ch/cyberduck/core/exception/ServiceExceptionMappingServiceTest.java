package ch.cyberduck.core.exception;

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

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.ConnectionCanceledException;

import org.jets3t.service.ServiceException;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id$
 */
public class ServiceExceptionMappingServiceTest extends AbstractTestCase {

    @Test
    public void testLoginFailure() throws Exception {
        final ServiceException f = new ServiceException("m", "<null/>");
        f.setResponseCode(403);
        assertTrue(new ServiceExceptionMappingService().map(f) instanceof LoginFailureException);
    }

    @Test
    public void testMapping() {
        assertEquals("message.", new ServiceExceptionMappingService().map(new ServiceException("message")).getDetail());
        assertEquals("Exceeded 403 retry limit (1).", new ServiceExceptionMappingService().map(
                new ServiceException("Exceeded 403 retry limit (1).")).getDetail());
        assertEquals("Connection failed", new ServiceExceptionMappingService().map(
                new ServiceException("Exceeded 403 retry limit (1).")).getMessage());
    }

    @Test
    public void testDNSFailure() {
        assertEquals("custom",
                new ServiceExceptionMappingService().map("custom", new ServiceException("message", new UnknownHostException("h"))).getMessage());
        assertEquals("h.",
                new ServiceExceptionMappingService().map("custom", new ServiceException("message", new UnknownHostException("h"))).getDetail());
    }

    @Test
    public void testCustomMessage() {
        assertEquals("custom",
                new ServiceExceptionMappingService().map("custom", new ServiceException("message")).getMessage());
        assertEquals("message.",
                new ServiceExceptionMappingService().map("custom", new ServiceException("message")).getDetail());
    }

    @Test
    public void testIAMFailure() {
        assertEquals("The IAM policy must allow the action s3:GetBucketLocation on the resource arn:aws:s3:::endpoint-9a527d70-d432-4601-b24b-735e721b82c9",
                new ServiceExceptionMappingService().map("The IAM policy must allow the action s3:GetBucketLocation on the resource arn:aws:s3:::endpoint-9a527d70-d432-4601-b24b-735e721b82c9", new ServiceException("message")).getMessage());
        assertEquals("message.",
                new ServiceExceptionMappingService().map("The IAM policy must allow the action s3:GetBucketLocation on the resource arn:aws:s3:::endpoint-9a527d70-d432-4601-b24b-735e721b82c9", new ServiceException("message")).getDetail());
    }

    @Test
    public void testHandshakeFailure() {
        assertEquals(ConnectionCanceledException.class, new ServiceExceptionMappingService().map(
                new ServiceException(new SSLHandshakeException("f"))).getClass());
    }
}
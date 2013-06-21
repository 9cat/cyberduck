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

import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;

import com.rackspacecloud.client.cloudfiles.FilesException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id$
 */
public class FilesExceptionMappingServiceTest extends AbstractTestCase {

    @Test
    public void testLoginFailure() throws Exception {
        final FilesException f = new FilesException(
                "message", new Header[]{}, new BasicStatusLine(new ProtocolVersion("http", 1, 1), 403, "Forbidden"));
        assertTrue(new FilesExceptionMappingService().map(f) instanceof LoginFailureException);
        assertEquals("Login failed", new FilesExceptionMappingService().map(f).getMessage());
        assertEquals("message. 403 Forbidden.", new FilesExceptionMappingService().map(f).getDetail());
    }

    @Test
    public void testMap() throws Exception {
        assertEquals("message. 500 reason.", new FilesExceptionMappingService().map(
                new FilesException("message", null, new StatusLine() {
                    @Override
                    public ProtocolVersion getProtocolVersion() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public int getStatusCode() {
                        return 500;
                    }

                    @Override
                    public String getReasonPhrase() {
                        return "reason";
                    }
                })).getDetail());
    }
}

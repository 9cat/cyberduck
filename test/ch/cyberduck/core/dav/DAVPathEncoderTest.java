package ch.cyberduck.core.dav;

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

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Path;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class DAVPathEncoderTest extends AbstractTestCase {

    @Test
    public void testEncode() throws Exception {
        assertEquals("/", new DAVPathEncoder().encode(new Path("/", Path.DIRECTORY_TYPE)));
        assertEquals("/dav/", new DAVPathEncoder().encode(new Path("/dav", Path.DIRECTORY_TYPE)));
        assertEquals("/dav", new DAVPathEncoder().encode(new Path("/dav", Path.FILE_TYPE)));
        assertEquals("/dav/file%20path", new DAVPathEncoder().encode(new Path("/dav/file path", Path.FILE_TYPE)));
    }
}

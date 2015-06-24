package ch.cyberduck.core.local;

/*
 * Copyright (c) 2002-2015 David Kocher. All rights reserved.
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
import ch.cyberduck.core.exception.LocalAccessDeniedException;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class PanelSandboxBookmarkResolverTest extends AbstractTestCase {

    @Test(expected = LocalAccessDeniedException.class)
    public void testCreateNotFound() throws Exception {
        final String name = UUID.randomUUID().toString();
        FinderLocal l = new FinderLocal(System.getProperty("java.io.tmpdir"), name);
        try {
            assertNull(new PanelSandboxBookmarkResolver().create(l));
        }
        catch(LocalAccessDeniedException e) {
            assertEquals("The file “" + name + "” couldn’t be opened because there is no such file. Please verify disk permissions.", e.getDetail());
            throw e;
        }
    }

    @Test
    public void testCreate() throws Exception {
        FinderLocal l = new FinderLocal(System.getProperty("java.io.tmpdir"));
        assertNotNull(new PanelSandboxBookmarkResolver().create(l));
    }
}
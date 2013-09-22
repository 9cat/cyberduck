package ch.cyberduck.core.sftp;

/*
 * Copyright (c) 2012 David Kocher. All rights reserved.
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
import ch.cyberduck.core.LocalFactory;

import org.junit.Test;
import org.spearce.jgit.transport.OpenSshConfig;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class OpenSSHHostnameConfiguratorTest extends AbstractTestCase {

    @Test
    public void testLookup() throws Exception {
        OpenSSHHostnameConfigurator c = new OpenSSHHostnameConfigurator(
                new OpenSshConfig(
                        new File(LocalFactory.createLocal("test/ch/cyberduck/core/sftp", "openssh/config").getAbsolute())));
        assertEquals("cyberduck.ch", c.getHostname("alias"));
    }

    @Test
    public void testPort() throws Exception {
        OpenSSHHostnameConfigurator c = new OpenSSHHostnameConfigurator(
                new OpenSshConfig(
                        new File(LocalFactory.createLocal("test/ch/cyberduck/core/sftp", "openssh/config").getAbsolute())));
        assertEquals(555, c.getPort("portalias"));
        assertEquals(-1, c.getPort(null));
    }
}

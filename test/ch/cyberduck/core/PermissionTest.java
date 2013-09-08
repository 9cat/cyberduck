package ch.cyberduck.core;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class PermissionTest extends AbstractTestCase {

    @Test
    public void testGetAsDictionary() throws Exception {
        assertEquals(new Permission(777), new Permission(new Permission(777).serialize(SerializerFactory.get())));
        assertEquals(new Permission(700), new Permission(new Permission(700).serialize(SerializerFactory.get())));
        assertEquals(new Permission(400), new Permission(new Permission(400).serialize(SerializerFactory.get())));
    }

    @Test
    public void testSymbol() {
        Permission p1 = new Permission(777);
        assertEquals("rwxrwxrwx", p1.getSymbol());
        Permission p2 = new Permission(666);
        assertEquals("rw-rw-rw-", p2.getSymbol());
    }

    /**
     * 4000    (the set-user-ID-on-execution bit) Executable files with this bit set will run with effective uid set to the uid of the file owner.
     * Directories with the set-user-id bit set will force all files and sub-directories created in them to be owned by the directory owner
     * and not by the uid of the creating process, if the underlying file system supports this feature: see chmod(2) and the suiddir option to
     * mount(8).
     */
    @Test
    public void testSetUid() {
        assertTrue(new Permission(Permission.Action.read, Permission.Action.none, Permission.Action.none,
                false, true, false).isSetuid());
        assertTrue(new Permission(4755).isSetuid());
        assertTrue(new Permission(6755).isSetuid());
        assertTrue(new Permission(5755).isSetuid());
        assertFalse(new Permission(1755).isSetuid());
    }

    /**
     * 2000    (the set-group-ID-on-execution bit) Executable files with this bit set will run with effective gid set to the gid of the file owner.
     */
    @Test
    public void testSetGid() {
        assertTrue(new Permission(Permission.Action.read, Permission.Action.none, Permission.Action.none,
                false, false, true).isSetgid());
        assertTrue(new Permission(2755).isSetgid());
        assertTrue(new Permission(3755).isSetgid());
        assertTrue(new Permission(6755).isSetgid());
        assertFalse(new Permission(1755).isSetgid());
    }

    /**
     * 1000    (the sticky bit) See chmod(2) and sticky(8).
     */
    @Test
    public void testSetSticky() {
        assertTrue(new Permission(1755).isSticky());
        assertTrue(new Permission(3755).isSticky());
        assertTrue(new Permission(5755).isSticky());
        assertFalse(new Permission(2755).isSticky());
        assertFalse(new Permission(6755).isSticky());
    }

    @Test
    public void testActions() {
        assertEquals(Permission.Action.read_write, Permission.Action.all.and(Permission.Action.execute.not()));
        assertEquals(Permission.Action.read, Permission.Action.none.or(Permission.Action.read));
    }

    @Test
    public void testToMode() {
        final Permission permission = new Permission(Permission.Action.read,
                Permission.Action.none, Permission.Action.none);
        assertEquals("400", permission.getMode());
    }

    @Test
    public void testFromMode() {
        assertEquals(Permission.Action.all, (new Permission("rwxrwxrwx").getUser()));
        assertEquals(Permission.Action.all, (new Permission("rwxrwxrwx").getGroup()));
        assertEquals(Permission.Action.all, (new Permission("rwxrwxrwx").getOther()));
        assertEquals(Permission.Action.all, (new Permission("rwxrwxrwt").getOther()));
        assertEquals(Permission.Action.read_write, (new Permission("rwxrwxrwT").getOther()));
        assertEquals(Permission.Action.read, (new Permission("r--r--r--").getUser()));
        assertEquals(Permission.Action.read, (new Permission("s--r--r--").getUser()));
        assertEquals(Permission.Action.none, (new Permission("S--r--r--").getUser()));
        assertEquals(Permission.Action.read, (new Permission("r--r--r--").getGroup()));
        assertEquals(Permission.Action.read, (new Permission("r--r--r--").getOther()));
        assertEquals(Permission.Action.read_write, (new Permission("rw-rw-rw-").getUser()));
        assertEquals(Permission.Action.read_write, (new Permission("rw-rw-rw-").getGroup()));
        assertEquals(Permission.Action.read_write, (new Permission("rw-rw-rw-").getOther()));
        assertEquals(Permission.Action.read_execute, (new Permission("r-xr-xr-x").getUser()));
        assertEquals(Permission.Action.read_execute, (new Permission("r-xr-xr-x").getGroup()));
        assertEquals(Permission.Action.read_execute, (new Permission("r-xr-xr-x").getOther()));
    }

    @Test
    public void testModeStickyBit() {
        final Permission permission = new Permission(Permission.Action.read,
                Permission.Action.none, Permission.Action.none, true, false, false);
        assertEquals("1400", permission.getMode());
    }

    @Test
    public void testFailureParsing() {
        assertEquals(Permission.EMPTY, new Permission("rwx"));
        assertEquals(Permission.EMPTY, new Permission(888));
    }

    @Test
    public void testEmpty() {
        assertEquals(Permission.EMPTY, new Permission());
        assertEquals(Permission.EMPTY, new Permission(0));
    }
}

package ch.cyberduck.core.ftp;

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
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Permission;
import ch.cyberduck.core.exception.ListCanceledException;
import ch.cyberduck.core.ftp.parser.CompositeFileEntryParser;

import org.apache.commons.net.ftp.FTPFileEntryParser;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class FTPListResponseReaderTest extends AbstractTestCase {

    @Test(expected = FTPInvalidListException.class)
    public void test3243() throws Exception {
        final FTPFileEntryParser parser = new FTPParserSelector().getParser("UNIX");
        final FTPSession s = new FTPSession(new Host(new FTPProtocol(), "localhost"));
        Path path = new Path("/SunnyD", EnumSet.of(Path.Type.directory));
        assertEquals("SunnyD", path.getName());
        assertEquals("/SunnyD", path.getAbsolute());
        final AttributedList<Path> list = new AttributedList<Path>();
        new FTPListResponseReader(parser).read(path, Collections.singletonList(" drwxrwx--x 1 owner group          512 Jun 12 15:40 SunnyD"), new DisabledListProgressListener()
        );
    }

    @Test
    public void testParseSymbolicLink() throws Exception {
        final FTPFileEntryParser parser = new FTPParserSelector().getParser("UNIX");

        final FTPSession s = new FTPSession(new Host(new FTPProtocol(), "localhost"));
        Path path = new Path("/", EnumSet.of(Path.Type.directory));
        assertEquals("/", path.getName());
        assertEquals("/", path.getAbsolute());

        final AttributedList<Path> list = new FTPListResponseReader(parser).read(path, Collections.singletonList("lrwxrwxrwx    1 mk basicgrp       27 Sep 23  2004 www -> /www/basic/mk"), new DisabledListProgressListener()
        );

        assertFalse(list.isEmpty());
        final Path parsed = list.get(0);
        assertTrue(parsed.isSymbolicLink());
        assertEquals("/www/basic/mk", parsed.getSymlinkTarget().getAbsolute());
        assertEquals(new Permission("rwxrwxrwx"), parsed.attributes().getPermission());
    }

    @Test(expected = FTPInvalidListException.class)
    public void test3763() throws Exception {
        final FTPFileEntryParser parser = new FTPParserSelector().getParser("UNIX");
        final FTPSession s = new FTPSession(new Host(new FTPProtocol(), "localhost"));
        Path path = new Path("/www", EnumSet.of(Path.Type.directory));
        assertEquals("www", path.getName());
        assertEquals("/www", path.getAbsolute());
        final AttributedList<Path> list = new FTPListResponseReader(parser).read(path, Collections.singletonList("lrwxrwxrwx    1 mk basicgrp       27 Sep 23  2004 /home/mk/www -> /www/basic/mk"), new DisabledListProgressListener()
        );
    }

    @Test
    public void testStickyBit() throws Exception {
        final FTPFileEntryParser parser = new FTPParserSelector().getParser("UNIX");
        final FTPSession s = new FTPSession(new Host(new FTPProtocol(), "localhost"));
        final AttributedList<Path> list = new FTPListResponseReader(parser).read(new Path("/", EnumSet.of(Path.Type.directory)), Collections.singletonList("-rwsrwSr-T 1 dkocher dkocher         0 Sep  6 22:27 t"), new DisabledListProgressListener()
        );
        final Path parsed = list.get(new Path("/t", EnumSet.of(Path.Type.file)).getReference());
        assertNotNull(parsed);
        assertTrue(parsed.attributes().getPermission().isSticky());
        assertTrue(parsed.attributes().getPermission().isSetuid());
        assertTrue(parsed.attributes().getPermission().isSetgid());
        assertEquals(new Permission("rwsrwSr-T"), parsed.attributes().getPermission());
    }

    @Test
    @Ignore
    public void testParseHardlinkCountBadFormat() throws Exception {
        final FTPSession s = new FTPSession(new Host(new FTPProtocol(), "localhost"));
        Path path = new Path(
                "/store/public/brain", EnumSet.of(Path.Type.directory));

        String[] replies = new String[]{
                "drwx------+111 mi       public       198 Dec 17 12:29 unsorted"
        };

        final CompositeFileEntryParser parser = new FTPParserSelector().getParser("UNIX");
        final AttributedList<Path> list = new FTPListResponseReader(parser).read(path, Arrays.asList(replies), new DisabledListProgressListener());
        assertEquals(1, list.size());
        final Path parsed = list.get(0);
        assertEquals("unsorted", parsed.getName());
        assertEquals("/store/public/brain", parsed.getParent().getAbsolute());
    }

    @Test
    public void testParseAbsolutePaths() throws Exception {
        final FTPSession s = new FTPSession(new Host(new FTPProtocol(), "localhost"));
        Path path = new Path(
                "/data/FTP_pub", EnumSet.of(Path.Type.directory));

        String[] replies = new String[]{
                "- [RWCEAFMS] Petersm                             0 May 05  2004 /data/FTP_pub/WelcomeTo_PeakFTP"
        };
        final CompositeFileEntryParser parser = new FTPParserSelector().getParser("NETWARE  Type : L8");
        final AttributedList<Path> list = new FTPListResponseReader(parser).read(path, Arrays.asList(replies), new DisabledListProgressListener()
        );
        assertEquals(1, list.size());
        final Path parsed = list.get(0);
        assertEquals("WelcomeTo_PeakFTP", parsed.getName());
        assertEquals("/data/FTP_pub", parsed.getParent().getAbsolute());
        assertFalse(parsed.attributes().getPermission().isSticky());
        assertFalse(parsed.attributes().getPermission().isSetuid());
        assertFalse(parsed.attributes().getPermission().isSetgid());
    }

    @Test(expected = ListCanceledException.class)
    public void testLimit() throws Exception {
        final CompositeFileEntryParser parser = new FTPParserSelector().getParser("NETWARE  Type : L8");
        final AttributedList<Path> list = new FTPListResponseReader(parser).read(
                new Path("/", EnumSet.of(Path.Type.directory)), Arrays.asList("lrwxrwxrwx    1 ftp      ftp            23 Feb 05 06:51 debian -> ../pool/4/mirror/debian"), new DisabledListProgressListener() {
                    @Override
                    public void chunk(final Path parent, AttributedList<Path> list) throws ListCanceledException {
                        throw new ListCanceledException(AttributedList.<Path>emptyList());
                    }
                }
        );
    }

    @Test(expected = FTPInvalidListException.class)
    public void testListNoRead() throws Exception {
        final Path directory = new Path("/sandbox/noread", EnumSet.of(Path.Type.directory));
        final String[] lines = new String[]{
                "213-Status follows:",
                "d-w--w----    2 1003     1003         4096 Nov 06  2013 noread",
                "213 End of status"};
        final CompositeFileEntryParser parser = new FTPParserSelector().getParser("UNIX");
        final AttributedList<Path> list = new FTPListResponseReader(parser).read(directory, Arrays.asList(lines), new DisabledListProgressListener());
        assertEquals(0, list.size());
    }

    @Test(expected = FTPInvalidListException.class)
    public void testListSymbolicLink() throws Exception {
        final Path directory = new Path("/home/barchouston/www", EnumSet.of(Path.Type.directory));
        final String[] lines = new String[]{
                "213-status of /home/barchouston/www:",
                "lrwxrwxrwx   1 barchous barchous       16 Apr  2  2002 /home/barchouston/www -> /www/barchouston",
                "213 End of Status"};
        final CompositeFileEntryParser parser = new FTPParserSelector().getParser("UNIX");
        final AttributedList<Path> list = new FTPListResponseReader(parser).read(directory, Arrays.asList(lines), new DisabledListProgressListener());
        assertEquals(0, list.size());
    }
}

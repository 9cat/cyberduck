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

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Permission;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ListCanceledException;

import org.junit.Test;

import java.net.SocketTimeoutException;
import java.util.EnumSet;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class FTPListServiceTest extends AbstractTestCase {

    @Test
    public void testList() throws Exception {
        final Host host = new Host(new FTPTLSProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("ftp.user"), properties.getProperty("ftp.password")
        ));
        final FTPSession session = new FTPSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final ListService service = new FTPListService(session, null, TimeZone.getDefault());
        final Path directory = session.workdir();
        final AttributedList<Path> list = service.list(directory, new DisabledListProgressListener() {
            int size = 0;

            @Override
            public void chunk(AttributedList<Path> list) throws ListCanceledException {
                assertEquals(++size, list.size());
                assertNotNull(list.get(list.size() - 1));
            }
        });
        assertTrue(list.contains(
                new Path(directory, "test", EnumSet.of(Path.Type.file)).getReference()));
        assertEquals(new Permission(Permission.Action.read_write, Permission.Action.read_write, Permission.Action.read_write),
                list.get(new Path(directory, "test", EnumSet.of(Path.Type.file)).getReference()).attributes().getPermission());
        session.close();
    }

    @Test
    public void testListExtended() throws Exception {
        final Host host = new Host(new FTPTLSProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("ftp.user"), properties.getProperty("ftp.password")
        ));
        final FTPSession session = new FTPSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final FTPListService service = new FTPListService(session, null, TimeZone.getDefault());
        service.remove(FTPListService.Command.list);
        service.remove(FTPListService.Command.stat);
        service.remove(FTPListService.Command.mlsd);
        final Path directory = session.workdir();
        final AttributedList<Path> list = service.list(directory, new DisabledListProgressListener() {
            int size = 0;

            @Override
            public void chunk(AttributedList<Path> list) throws ListCanceledException {
                assertEquals(++size, list.size());
            }
        });
        assertTrue(list.contains(
                new Path(directory, "test", EnumSet.of(Path.Type.file)).getReference()));
        assertEquals(new Permission(Permission.Action.read_write, Permission.Action.read_write, Permission.Action.read_write),
                list.get(new Path(directory, "test", EnumSet.of(Path.Type.file)).getReference()).attributes().getPermission());
        session.close();
    }

    @Test
    public void testListEmptyDirectoryStat() throws Exception {
        final Host host = new Host(new FTPTLSProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("ftp.user"), properties.getProperty("ftp.password")
        ));
        final FTPSession session = new FTPSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final FTPListService list = new FTPListService(session, null, TimeZone.getDefault());
        list.remove(FTPListService.Command.list);
        list.remove(FTPListService.Command.lista);
        list.remove(FTPListService.Command.mlsd);
        assertTrue(list.list(new Path(session.workdir(), "test.d", EnumSet.of(Path.Type.directory)), new DisabledListProgressListener()).isEmpty());
        session.close();
    }

    @Test
    public void testListEmptyDirectoryList() throws Exception {
        final Host host = new Host(new FTPTLSProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("ftp.user"), properties.getProperty("ftp.password")
        ));
        final FTPSession session = new FTPSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final FTPListService list = new FTPListService(session, null, TimeZone.getDefault());
        list.remove(FTPListService.Command.stat);
        list.remove(FTPListService.Command.lista);
        list.remove(FTPListService.Command.mlsd);
        assertTrue(list.list(new Path(session.workdir(), "test.d", EnumSet.of(Path.Type.directory)), new DisabledListProgressListener()).isEmpty());
        session.close();
    }

    @Test
    public void testPostProcessing() throws Exception {
        final Host host = new Host(new FTPTLSProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("ftp.user"), properties.getProperty("ftp.password")
        ));
        final FTPSession session = new FTPSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final FTPListService service = new FTPListService(session, null, TimeZone.getDefault());
        final AttributedList<Path> list = new AttributedList<Path>();
        final Path l = new Path("/test.d", EnumSet.of(Path.Type.file, AbstractPath.Type.symboliclink));
        l.setSymlinkTarget(new Path("/test.s", EnumSet.of(Path.Type.file)));
        list.add(l);
        assertTrue(list.contains(new Path("/test.d", EnumSet.of(Path.Type.file, AbstractPath.Type.symboliclink)).getReference()));
        service.post(new Path("/", EnumSet.of(Path.Type.directory)), list);
        assertFalse(list.contains(new Path("/test.d", EnumSet.of(Path.Type.file, AbstractPath.Type.symboliclink)).getReference()));
        assertTrue(list.contains(new Path("/test.d", EnumSet.of(Path.Type.directory, AbstractPath.Type.symboliclink)).getReference()));
        session.close();
    }

    @Test
    public void testListIOFailureStat() throws Exception {
        final Host host = new Host(new FTPTLSProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("ftp.user"), properties.getProperty("ftp.password")
        ));
        final FTPSession session = new FTPSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final FTPListService service = new FTPListService(session, null, TimeZone.getDefault());
        service.remove(FTPListService.Command.lista);
        service.remove(FTPListService.Command.mlsd);
        final AtomicBoolean set = new AtomicBoolean();
        service.implementations.put(FTPListService.Command.stat, new ListService() {
            @Override
            public AttributedList<Path> list(final Path directory, final ListProgressListener listener) throws BackgroundException {
                if(set.get()) {
                    fail();
                }
                set.set(true);
                throw new BackgroundException("t", new SocketTimeoutException());
            }
        });
        final Path directory = session.workdir();
        final AttributedList<Path> list = service.list(directory, new DisabledListProgressListener());
        assertTrue(set.get());
        assertTrue(session.isConnected());
        assertNotNull(session.getClient());
        assertTrue(list.contains(
                new Path(directory, "test", EnumSet.of(Path.Type.file)).getReference()));
        service.list(directory, new DisabledListProgressListener());
    }
}

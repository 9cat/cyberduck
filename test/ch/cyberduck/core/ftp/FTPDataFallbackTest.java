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
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DefaultHostKeyController;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.transfer.TransferStatus;

import org.junit.Test;

import java.net.SocketTimeoutException;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class FTPDataFallbackTest extends AbstractTestCase {

    @Test
    public void testFallbackDataConnectionSocketTimeout() throws Exception {
        final Host host = new Host(new FTPProtocol(), "mirror.switch.ch", new Credentials(
                Preferences.instance().getProperty("connection.login.anon.name"), null
        ));
        host.setFTPConnectMode(FTPConnectMode.PORT);

        final AtomicInteger count = new AtomicInteger();

        final FTPSession session = new FTPSession(host) {
            protected int timeout() {
                return 2000;
            }
        };
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final Path path = new Path("/pub/debian/README.html", EnumSet.of(Path.Type.file));
        final TransferStatus status = new TransferStatus();
        final DataConnectionAction<Void> action = new DataConnectionAction<Void>() {
            @Override
            public Void execute() throws BackgroundException {
                if(count.get() == 0) {
                    throw new BackgroundException(new SocketTimeoutException());
                }
                return null;
            }
        };
        final FTPDataFallback f = new FTPDataFallback(session) {
            @Override
            protected <T> T fallback(final DataConnectionAction<T> action) throws BackgroundException {
                count.incrementAndGet();
                return super.fallback(action);
            }
        };
        f.data(path, action);
        assertEquals(1, count.get());
        session.close();
    }

    @Test
    public void testFallbackDataConnection500Error() throws Exception {
        final Host host = new Host(new FTPTLSProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("ftp.user"), properties.getProperty("ftp.password")
        ));
        host.setFTPConnectMode(FTPConnectMode.PORT);
        final AtomicInteger count = new AtomicInteger();
        final FTPSession session = new FTPSession(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final TransferStatus status = new TransferStatus();
        final DataConnectionAction<Void> action = new DataConnectionAction<Void>() {
            @Override
            public Void execute() throws BackgroundException {
                if(count.get() == 0) {
                    throw new BackgroundException(new FTPException(500, "m"));
                }
                return null;
            }
        };
        final FTPDataFallback f = new FTPDataFallback(session) {
            @Override
            protected <T> T fallback(final DataConnectionAction<T> action) throws BackgroundException {
                count.incrementAndGet();
                return super.fallback(action);
            }
        };
        f.data(new Path("test", EnumSet.of(Path.Type.file)), action);
        assertEquals(1, count.get());
        session.close();
    }
}

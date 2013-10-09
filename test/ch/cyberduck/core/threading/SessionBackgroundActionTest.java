package ch.cyberduck.core.threading;

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
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.DefaultHostKeyController;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.NullSession;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.ProgressListener;
import ch.cyberduck.core.TranscriptListener;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;

import org.junit.Test;

import java.net.SocketTimeoutException;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class SessionBackgroundActionTest extends AbstractTestCase {

    @Test
    public void testGetExceptionCanceled() throws Exception {
        final BackgroundException failure = new BackgroundException(new RuntimeException());
        SessionBackgroundAction a = new SessionBackgroundAction(null, Cache.empty(), new AlertCallback() {
            @Override
            public void alert(final SessionBackgroundAction repeatableBackgroundAction, final BackgroundException f, final StringBuilder transcript) {
                assertEquals(failure, f);
            }
        }, new ProgressListener() {
            @Override
            public void message(final String message) {
                //
            }
        }, new TranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                //
            }
        }, new DisabledLoginController(), new DefaultHostKeyController()
        ) {

            @Override
            public Object run() throws BackgroundException {
                throw new ConnectionCanceledException();
            }
        };
        a.call();
        assertFalse(a.hasFailed());
        assertNull(a.getException());
    }

    @Test
    public void testGetExceptionFailure() throws Exception {
        final BackgroundException failure = new BackgroundException(new RuntimeException());
        SessionBackgroundAction a = new SessionBackgroundAction(null, Cache.empty(), new AlertCallback() {
            @Override
            public void alert(final SessionBackgroundAction repeatableBackgroundAction, final BackgroundException f, final StringBuilder transcript) {
                assertEquals(failure, f);
            }
        }, new ProgressListener() {
            @Override
            public void message(final String message) {
                //
            }
        }, new TranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                //
            }
        }, new DisabledLoginController(), new DefaultHostKeyController()
        ) {

            @Override
            public Object run() throws BackgroundException {
                throw failure;
            }
        };
        a.call();
        assertTrue(a.hasFailed());
        assertNotNull(a.getException());
    }

    @Test
    public void testRetrySocket() throws Exception {
        final BackgroundException failure = new BackgroundException(new SocketTimeoutException(""));
        SessionBackgroundAction a = new SessionBackgroundAction(new NullSession(new Host(("t"))), Cache.empty(), new AlertCallback() {
            @Override
            public void alert(final SessionBackgroundAction repeatableBackgroundAction, final BackgroundException f, final StringBuilder transcript) {
                assertEquals(failure, f);
            }
        }, new ProgressListener() {
            @Override
            public void message(final String message) {
                //
            }
        }, new TranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                //
            }
        }, new DisabledLoginController(), new DefaultHostKeyController()
        ) {
            @Override
            public Object run() throws BackgroundException {
                throw failure;
            }
        };
        assertEquals(Preferences.instance().getInteger("connection.retry"), a.retry());
    }
}

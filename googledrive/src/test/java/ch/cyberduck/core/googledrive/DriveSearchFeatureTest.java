package ch.cyberduck.core.googledrive;

/*
 * Copyright (c) 2002-2017 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
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
 */

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginConnectionService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DefaultX509TrustManager;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.test.IntegrationTest;
import ch.cyberduck.ui.browser.SearchFilter;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.*;

@Category(IntegrationTest.class)
public class DriveSearchFeatureTest {

    @Test
    public void testSearchRoot() throws Exception {
        final Host host = new Host(new DriveProtocol(), "www.googleapis.com", new Credentials());
        final DriveSession session = new DriveSession(host, new DefaultX509TrustManager(), new DefaultX509KeyManager());
        new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(),
                new DisabledPasswordStore() {
                    @Override
                    public String getPassword(Scheme scheme, int port, String hostname, String user) {
                        if(user.equals("Google Drive OAuth2 Access Token")) {
                            return System.getProperties().getProperty("googledrive.accesstoken");
                        }
                        if(user.equals("Google Drive OAuth2 Refresh Token")) {
                            return System.getProperties().getProperty("googledrive.refreshtoken");
                        }
                        return null;
                    }

                    @Override
                    public String getPassword(String hostname, String user) {
                        return super.getPassword(hostname, user);
                    }
                }, new DisabledProgressListener()
        ).connect(session, PathCache.empty(), new DisabledCancelCallback());
        final String name = new AlphanumericRandomStringService().random();
        final Path workdir = new DriveHomeFinderService(session).find();
        final Path file = new DriveTouchFeature(session).touch(new Path(workdir, name, EnumSet.of(Path.Type.file)), new TransferStatus());
        final DriveSearchFeature feature = new DriveSearchFeature(session);
        assertTrue(feature.search(workdir, new SearchFilter(name), new DisabledListProgressListener()).contains(file));
        // Supports prefix matching only
        assertFalse(feature.search(workdir, new SearchFilter(StringUtils.substring(name, 2)), new DisabledListProgressListener()).contains(file));
        assertTrue(feature.search(workdir, new SearchFilter(StringUtils.substring(name, 0, name.length() - 2)), new DisabledListProgressListener()).contains(file));
        final Path subdir = new Path(workdir, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
        assertFalse(feature.search(subdir, new SearchFilter(name), new DisabledListProgressListener()).contains(file));
        new DriveDeleteFeature(session).delete(Collections.singletonList(file), new DisabledLoginCallback(), new Delete.DisabledCallback());
        session.close();
    }

    @Test
    public void testSearchFolder() throws Exception {
        final Host host = new Host(new DriveProtocol(), "www.googleapis.com", new Credentials());
        final DriveSession session = new DriveSession(host, new DefaultX509TrustManager(), new DefaultX509KeyManager());
        new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(),
                new DisabledPasswordStore() {
                    @Override
                    public String getPassword(Scheme scheme, int port, String hostname, String user) {
                        if(user.equals("Google Drive OAuth2 Access Token")) {
                            return System.getProperties().getProperty("googledrive.accesstoken");
                        }
                        if(user.equals("Google Drive OAuth2 Refresh Token")) {
                            return System.getProperties().getProperty("googledrive.refreshtoken");
                        }
                        return null;
                    }

                    @Override
                    public String getPassword(String hostname, String user) {
                        return super.getPassword(hostname, user);
                    }
                }, new DisabledProgressListener()
        ).connect(session, PathCache.empty(), new DisabledCancelCallback());
        final String name = new AlphanumericRandomStringService().random();
        final Path workdir = new DriveDirectoryFeature(session).mkdir(new Path(new DriveHomeFinderService(session).find(), new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory)), null, new TransferStatus());
        final Path file = new DriveTouchFeature(session).touch(new Path(workdir, name, EnumSet.of(Path.Type.file)), new TransferStatus());
        final DriveSearchFeature feature = new DriveSearchFeature(session);
        assertTrue(feature.search(workdir, new SearchFilter(name), new DisabledListProgressListener()).contains(file));
        // Supports prefix matching only
        assertFalse(feature.search(workdir, new SearchFilter(StringUtils.substring(name, 2)), new DisabledListProgressListener()).contains(file));
        final AttributedList<Path> result = feature.search(workdir, new SearchFilter(StringUtils.substring(name, 0, name.length() - 2)), new DisabledListProgressListener());
        assertTrue(result.contains(file));
        assertEquals(workdir, result.get(result.indexOf(file)).getParent());
        final Path subdir = new DriveDirectoryFeature(session).mkdir(new Path(workdir, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory)), null, new TransferStatus());
        assertFalse(feature.search(subdir, new SearchFilter(name), new DisabledListProgressListener()).contains(file));
        new DriveDeleteFeature(session).delete(Arrays.asList(file, subdir), new DisabledLoginCallback(), new Delete.DisabledCallback());
        session.close();
    }
}
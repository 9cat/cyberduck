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

import ch.cyberduck.core.*;
import ch.cyberduck.core.cryptomator.CryptoVault;
import ch.cyberduck.core.cryptomator.features.CryptoDeleteFeature;
import ch.cyberduck.core.cryptomator.features.CryptoListService;
import ch.cyberduck.core.cryptomator.features.CryptoTouchFeature;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DefaultX509TrustManager;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.DefaultVaultRegistry;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.test.IntegrationTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.Assert.*;

@Category(IntegrationTest.class)
public class DriveListServiceTest {

    @Test
    public void testListCryptomator() throws Exception {
        final Host host = new Host(new DriveProtocol(), "www.googleapis.com", new Credentials());
        final DriveSession session = new DriveSession(host, new DefaultX509TrustManager(), new DefaultX509KeyManager());
        new LoginConnectionService(new DisabledLoginCallback() {
            @Override
            public void prompt(final Host bookmark, final Credentials credentials, final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
                fail(reason);
            }
        }, new DisabledHostKeyCallback(),
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
                }, new DisabledProgressListener()
        ).connect(session, PathCache.empty(), new DisabledCancelCallback());
        final Path home = new DriveHomeFinderService(session).find();
        final CryptoVault cryptomator = new CryptoVault(
                new Path(home, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory)), new DisabledPasswordStore());
        final Path vault = cryptomator.create(session, null, new VaultCredentials("test"));
        session.withRegistry(new DefaultVaultRegistry(new DisabledPasswordStore(), new DisabledPasswordCallback(), cryptomator));
        assertTrue(new CryptoListService(session, new DriveDefaultListService(session), cryptomator).list(vault, new DisabledListProgressListener()).isEmpty());
        final Path test = new CryptoTouchFeature<Void>(session, new DriveTouchFeature(session), new DriveWriteFeature(session), cryptomator).touch(
                new Path(vault, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file)), new TransferStatus());
        assertEquals(test, new CryptoListService(session, new DriveDefaultListService(session), cryptomator).list(vault, new DisabledListProgressListener()).get(0));
        new CryptoDeleteFeature(session, new DriveDeleteFeature(session), cryptomator).delete(Arrays.asList(test, vault), new DisabledLoginCallback(), new Delete.DisabledCallback());
        session.close();
    }
}

package ch.cyberduck.core.googlestorage;

/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
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

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.features.AclPermission;
import ch.cyberduck.core.features.Headers;
import ch.cyberduck.core.features.Lifecycle;
import ch.cyberduck.core.features.Logging;
import ch.cyberduck.core.features.Versioning;
import ch.cyberduck.core.identity.IdentityConfiguration;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.s3.S3Protocol;
import ch.cyberduck.test.IntegrationTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(IntegrationTest.class)
public class GoogleStorageSessionTest {

    @Test
    public void testConnect() throws Exception {
        final Host host = new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                System.getProperties().getProperty("google.projectid"), null
        ));
        final GoogleStorageSession session = new GoogleStorageSession(host);
        assertNotNull(session.open(new DisabledHostKeyCallback()));
        assertTrue(session.isConnected());
        assertNotNull(session.getClient());
        session.login(new DisabledPasswordStore() {
            @Override
            public String getPassword(final Scheme scheme, final int port, final String hostname, final String user) {
                if(user.equals("Google Cloud Storage (api-project-408246103372) OAuth2 Access Token")) {
                    return System.getProperties().getProperty("google.accesstoken");
                }
                if(user.equals("Google Cloud Storage (api-project-408246103372) OAuth2 Refresh Token")) {
                    return System.getProperties().getProperty("google.refreshtoken");
                }
                return null;
            }
        }, new DisabledLoginCallback(), new DisabledCancelCallback(), PathCache.empty());
        session.close();
    }

    @Test(expected = LoginCanceledException.class)
    public void testConnectInvalidRefreshToken() throws Exception {
        final Host host = new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                System.getProperties().getProperty("google.projectid"), null
        ));
        final GoogleStorageSession session = new GoogleStorageSession(host);
        assertNotNull(session.open(new DisabledHostKeyCallback()));
        assertTrue(session.isConnected());
        assertNotNull(session.getClient());
        session.login(new DisabledPasswordStore() {
            @Override
            public String getPassword(final Scheme scheme, final int port, final String hostname, final String user) {
                if(user.equals("Google Cloud Storage (api-project-408246103372) OAuth2 Access Token")) {
                    return System.getProperties().getProperty("google.accesstoken");
                }
                if(user.equals("Google Cloud Storage (api-project-408246103372) OAuth2 Refresh Token")) {
                    return "a";
                }
                return null;
            }
        }, new DisabledLoginCallback(), new DisabledCancelCallback(), PathCache.empty());
    }

    @Test
    public void testConnectInvalidAccessTokenRefreshToken() throws Exception {
        final Host host = new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                System.getProperties().getProperty("google.projectid"), null
        ));
        final GoogleStorageSession session = new GoogleStorageSession(host);
        assertNotNull(session.open(new DisabledHostKeyCallback()));
        assertTrue(session.isConnected());
        assertNotNull(session.getClient());
        session.login(new DisabledPasswordStore() {
            @Override
            public String getPassword(final Scheme scheme, final int port, final String hostname, final String user) {
                if(user.equals("Google Cloud Storage (api-project-408246103372) OAuth2 Access Token")) {
                    // Mark as not expired
                    PreferencesFactory.get().setProperty("googlestorage.oauth.expiry", System.currentTimeMillis() + 60 * 1000);
                    return "a";
                }
                if(user.equals("Google Cloud Storage (api-project-408246103372) OAuth2 Refresh Token")) {
                    return System.getProperties().getProperty("google.refreshtoken");
                }
                return null;
            }
        }, new DisabledLoginCallback(), new DisabledCancelCallback(), PathCache.empty());
    }

    @Test(expected = LoginCanceledException.class)
    public void testConnectInvalidProjectId() throws Exception {
        final Host host = new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                System.getProperties().getProperty("google.projectid") + "1", null
        ));
        final GoogleStorageSession session = new GoogleStorageSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore() {
            @Override
            public String getPassword(final Scheme scheme, final int port, final String hostname, final String user) {
                if(user.equals("Google Cloud Storage (api-project-408246103372) OAuth2 Access Token")) {
                    return System.getProperties().getProperty("google.accesstoken");
                }
                if(user.equals("Google Cloud Storage (api-project-408246103372) OAuth2 Refresh Token")) {
                    return System.getProperties().getProperty("google.refreshtoken");
                }
                return null;
            }
        }, new DisabledLoginCallback(), new DisabledCancelCallback(), PathCache.empty());
    }

    @Test(expected = LoginCanceledException.class)
    public void testConnectMissingKey() throws Exception {
        final Host host = new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                System.getProperties().getProperty("google.projectid"), null
        ));
        final GoogleStorageSession session = new GoogleStorageSession(host);
        assertNotNull(session.open(new DisabledHostKeyCallback()));
        assertTrue(session.isConnected());
        assertNotNull(session.getClient());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback() {
            @Override
            public Credentials prompt(final String username,
                                      final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
                Assert.assertEquals("OAuth2 Authentication", title);
                throw new LoginCanceledException();
            }
        }, null, PathCache.empty());
    }

    @Test(expected = LoginCanceledException.class)
    public void testCallbackOauth() throws Exception {
        final Host host = new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                "a", "s"
        ));
        final GoogleStorageSession session = new GoogleStorageSession(host);
        assertNotNull(session.open(new DisabledHostKeyCallback()));
        assertTrue(session.isConnected());
        assertNotNull(session.getClient());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback(), PathCache.empty());
    }

    @Test
    public void testFeatures() {
        assertNotNull(new GoogleStorageSession(new Host(new S3Protocol())).getFeature(AclPermission.class));
        assertNotNull(new GoogleStorageSession(new Host(new S3Protocol())).getFeature(DistributionConfiguration.class));
        assertNotNull(new GoogleStorageSession(new Host(new S3Protocol())).getFeature(IdentityConfiguration.class));
        assertNotNull(new GoogleStorageSession(new Host(new S3Protocol())).getFeature(Logging.class));
        assertNotNull(new GoogleStorageSession(new Host(new S3Protocol())).getFeature(Headers.class));
        assertNull(new GoogleStorageSession(new Host(new S3Protocol())).getFeature(Lifecycle.class));
        assertNull(new GoogleStorageSession(new Host(new S3Protocol())).getFeature(Versioning.class));
    }

    @Test(expected = LoginCanceledException.class)
    public void testInvalidProjectId() throws Exception {
        final Host host = new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                "duck-1432", ""
        ));
        final GoogleStorageSession session = new GoogleStorageSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback(), PathCache.empty());
        session.close();
    }

    @Test(expected = LoginCanceledException.class)
    public void testProjectIdNoAuthorization() throws Exception {
        final Host host = new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                "stellar-perigee-775", ""
        ));
        final GoogleStorageSession session = new GoogleStorageSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback() {
            @Override
            public Credentials prompt(final String username, final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
                // OAuth2
                return new Credentials("", "");
            }
        }, new DisabledCancelCallback(), PathCache.empty());
        session.close();
    }
}

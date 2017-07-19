package ch.cyberduck.core.googledrive;

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

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostKeyCallback;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PreferencesUseragentProvider;
import ch.cyberduck.core.UrlProvider;
import ch.cyberduck.core.UseragentProvider;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.*;
import ch.cyberduck.core.http.HttpSession;
import ch.cyberduck.core.oauth.OAuth2ErrorResponseInterceptor;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.Preferences;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.ssl.ThreadLocalHostnameDelegatingTrustManager;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.threading.CancelCallback;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

public class DriveSession extends HttpSession<Drive> {

    private ApacheHttpTransport transport;

    private final JsonFactory json = new GsonFactory();

    private final Preferences preferences
            = PreferencesFactory.get();

    private final UseragentProvider useragent
            = new PreferencesUseragentProvider();

    private final OAuth2RequestInterceptor authorizationService = new OAuth2RequestInterceptor(builder.build(this).build(),
            GoogleOAuthConstants.TOKEN_SERVER_URL, GoogleOAuthConstants.AUTHORIZATION_SERVER_URL,
            host.getProtocol().getClientId(),
            host.getProtocol().getClientSecret(),
            Collections.singletonList(DriveScopes.DRIVE))
            .withRedirectUri(preferences.getProperty("googledrive.oauth.redirecturi"));

    private final OAuth2ErrorResponseInterceptor retryHandler = new OAuth2ErrorResponseInterceptor(
            authorizationService);

    public DriveSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, new ThreadLocalHostnameDelegatingTrustManager(trust, host.getHostname()), key);
    }

    @Override
    protected Drive connect(final HostKeyCallback callback) throws BackgroundException {
        final HttpClientBuilder configuration = builder.build(this);
        configuration.addInterceptorLast(authorizationService);
        configuration.setServiceUnavailableRetryStrategy(retryHandler);
        this.transport = new ApacheHttpTransport(configuration.build());
        return new Drive.Builder(transport, json, new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                request.setSuppressUserAgentSuffix(true);
                // OAuth Bearer added in interceptor
            }
        })
                .setApplicationName(useragent.get())
                .build();
    }

    @Override
    public void login(final HostPasswordStore keychain, final LoginCallback prompt, final CancelCallback cancel, final Cache<Path> cache) throws BackgroundException {
        authorizationService.setTokens(authorizationService.authorize(host, keychain, prompt, cancel));
    }

    @Override
    protected void logout() throws BackgroundException {
        transport.shutdown();
    }

    @Override
    public AttributedList<Path> list(final Path directory, final ListProgressListener listener) throws BackgroundException {
        return new DriveListService(this, new DriveFileidProvider(this)).list(directory, listener);
    }

    public HttpClient getHttpClient() {
        return transport.getHttpClient();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T _getFeature(Class<T> type) {
        if(type == Read.class) {
            return (T) new DriveReadFeature(this);
        }
        if(type == Write.class) {
            return (T) new DriveWriteFeature(this);
        }
        if(type == Upload.class) {
            return (T) new DriveUploadFeature(new DriveWriteFeature(this));
        }
        if(type == Directory.class) {
            return (T) new DriveDirectoryFeature(this);
        }
        if(type == Delete.class) {
            return (T) new DriveBatchDeleteFeature(this);
        }
        if(type == Move.class) {
            return (T) new DriveMoveFeature(this);
        }
        if(type == Copy.class) {
            return (T) new DriveCopyFeature(this);
        }
        if(type == Touch.class) {
            return (T) new DriveTouchFeature(this);
        }
        if(type == UrlProvider.class) {
            return (T) new DriveUrlProvider();
        }
        if(type == Home.class) {
            return (T) new DriveHomeFinderService(this);
        }
        if(type == IdProvider.class) {
            return (T) new DriveFileidProvider(this);
        }
        if(type == Quota.class) {
            return (T) new DriveQuotaFeature(this);
        }
        if(type == Timestamp.class) {
            return (T) new DriveTimestampFeature(this);
        }
        if(type == Headers.class) {
            return (T) new DriveMetadataFeature(this);
        }
        if(type == Search.class) {
            return (T) new DriveSearchFeature(this);
        }
        if(type == Find.class) {
            return (T) new DriveFindFeature(this);
        }
        if(type == AttributesFinder.class) {
            return (T) new DriveFindFeature(this);
        }
        return super._getFeature(type);
    }
}

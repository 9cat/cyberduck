package ch.cyberduck.core.s3;

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

import ch.cyberduck.core.DescriptiveUrl;
import ch.cyberduck.core.DescriptiveUrlBag;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.URIEncoder;
import ch.cyberduck.core.UrlProvider;
import ch.cyberduck.core.UserDateFormatterFactory;
import ch.cyberduck.core.shared.DefaultUrlProvider;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.utils.ServiceUtils;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @version $Id$
 */
public class S3UrlProvider implements UrlProvider {
    private static final Logger log = Logger.getLogger(S3UrlProvider.class);

    private PathContainerService containerService
            = new PathContainerService();

    private HostPasswordStore store;

    private S3Session session;

    public S3UrlProvider(final S3Session session) {
        this(session, PasswordStoreFactory.get());
    }

    public S3UrlProvider(final S3Session session, final HostPasswordStore store) {
        this.session = session;
        this.store = store;
    }

    @Override
    public DescriptiveUrlBag toUrl(final Path file) {
        final DescriptiveUrlBag list = new DescriptiveUrlBag();
        if(file.attributes().isFile()) {
            // Publicly accessible URL of given object
            list.add(this.createBucketUrl(file, session.getHost().getProtocol().getScheme()));
            list.add(this.createBucketUrl(file, Scheme.http));
            if(!session.getHost().getCredentials().isAnonymousLogin()) {
                list.add(this.createSignedUrl(file, 60 * 60));
                // Default signed URL expiring in 24 hours.
                list.add(this.createSignedUrl(file, Preferences.instance().getInteger("s3.url.expire.seconds")));
                // Week
                list.add(this.createSignedUrl(file, 7 * 24 * 60 * 60));
            }
            // Torrent
            final S3Service service = new RestS3Service(
                    new AWSCredentials(session.getHost().getCredentials().getUsername(), session.getHost().getCredentials().getPassword()));
            list.add(new DescriptiveUrl(URI.create(service.createTorrentUrl(containerService.getContainer(file).getName(), containerService.getKey(file))),
                    DescriptiveUrl.Type.torrent,
                    MessageFormat.format(LocaleFactory.localizedString("{0} URL"), LocaleFactory.localizedString("Torrent"))));
        }
        list.addAll(new DefaultUrlProvider(session.getHost()).toUrl(file));
        return list;
    }

    /**
     * Properly URI encode and prepend the bucket name.
     *
     * @param scheme Protocol
     * @return URL to be displayed in browser
     */
    protected DescriptiveUrl createBucketUrl(final Path file, final Scheme scheme) {
        final StringBuilder url = new StringBuilder(scheme.name());
        url.append("://");
        if(file.isRoot()) {
            url.append(session.getHost().getHostname());
        }
        else {
            final String hostname = this.getHostnameForContainer(containerService.getContainer(file));
            if(hostname.startsWith(containerService.getContainer(file).getName())) {
                url.append(hostname);
                if(!containerService.isContainer(file)) {
                    url.append(URIEncoder.encode(containerService.getKey(file)));
                }
            }
            else {
                url.append(session.getHost().getHostname());
                url.append(URIEncoder.encode(file.getAbsolute()));
            }
        }
        return new DescriptiveUrl(URI.create(url.toString()), DescriptiveUrl.Type.http,
                MessageFormat.format(LocaleFactory.localizedString("{0} URL"), scheme.name().toUpperCase(Locale.ROOT)));
    }

    /**
     * Query string authentication. Query string authentication is useful for giving HTTP or
     * browser access to resources that would normally require authentication. The signature in the query
     * string secures the request.
     *
     * @return A signed URL with a limited validity over time.
     */
    protected DescriptiveUrl createSignedUrl(final Path file, final int seconds) {
        // Determine expiry time for URL
        final Calendar expiry = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expiry.add(Calendar.SECOND, seconds);
        // Generate URL
        final S3Service client = new RestS3Service(
                new AWSCredentials(session.getHost().getCredentials().getUsername(), session.getHost().getCredentials().getPassword()));
        final String secret = store.find(session.getHost());
        if(StringUtils.isBlank(secret)) {
            log.warn("No secret found in keychain required to sign temporary URL");
            return DescriptiveUrl.EMPTY;
        }
        client.setProviderCredentials(
                new AWSCredentials(session.getHost().getCredentials().getUsername(), secret));
        return new DescriptiveUrl(URI.create(client.createSignedUrl("GET",
                containerService.getContainer(file).getName(), containerService.getKey(file), null,
                null, expiry.getTimeInMillis() / 1000, false, session.getHost().getProtocol().isSecure(), false)), DescriptiveUrl.Type.signed,
                MessageFormat.format(LocaleFactory.localizedString("{0} URL"), LocaleFactory.localizedString("Signed", "S3"))
                        + " (" + MessageFormat.format(LocaleFactory.localizedString("Expires {0}", "S3") + ")",
                        UserDateFormatterFactory.get().getShortFormat(expiry.getTimeInMillis()))
        );
    }

    private String getHostnameForContainer(final Path bucket) {
        if(!ServiceUtils.isBucketNameValidDNSName(containerService.getContainer(bucket).getName())) {
            return session.getHost().getHostname();
        }
        if(session.getHost().getHostname().equals(session.getHost().getProtocol().getDefaultHostname())) {
            return String.format("%s.%s", bucket.getName(), session.getHost().getHostname());
        }
        return session.getHost().getHostname();
    }
}


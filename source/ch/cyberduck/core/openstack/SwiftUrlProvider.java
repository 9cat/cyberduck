package ch.cyberduck.core.openstack;

/*
 * Copyright (c) 2013 David Kocher. All rights reserved.
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
 * feedback@cyberduck.ch
 */

import ch.cyberduck.core.DescriptiveUrl;
import ch.cyberduck.core.DescriptiveUrlBag;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.UrlProvider;
import ch.cyberduck.core.UserDateFormatterFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.shared.DefaultUrlProvider;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jets3t.service.Constants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import ch.iterate.openstack.swift.model.AccountInfo;
import ch.iterate.openstack.swift.model.Region;

/**
 * @version $Id$
 */
public class SwiftUrlProvider implements UrlProvider {
    private static final Logger log = Logger.getLogger(SwiftUrlProvider.class);

    private PathContainerService containerService
            = new SwiftPathContainerService();

    private SwiftSession session;

    private Map<Region, AccountInfo> accounts;

    public SwiftUrlProvider(final SwiftSession session) {
        this(session, Collections.<Region, AccountInfo>emptyMap());
    }

    public SwiftUrlProvider(final SwiftSession session, final Map<Region, AccountInfo> accounts) {
        this.session = session;
        this.accounts = accounts;
    }

    @Override
    public DescriptiveUrlBag toUrl(final Path file) {
        final DescriptiveUrlBag list = new DescriptiveUrlBag();
        if(file.isFile()) {
            Region region = null;
            try {
                region = new SwiftRegionService(session).lookup(containerService.getContainer(file));
            }
            catch(BackgroundException e) {
                log.warn(String.format("Failure looking up region for %s %s", file, e.getMessage()));
            }
            if(null == region) {
                list.addAll(new DefaultUrlProvider(session.getHost()).toUrl(file));
            }
            else {
                list.add(new DescriptiveUrl(
                        URI.create(region.getStorageUrl(containerService.getContainer(file).getName(), containerService.getKey(file)).toString()),
                        DescriptiveUrl.Type.provider,
                        MessageFormat.format(LocaleFactory.localizedString("{0} URL"),
                                session.getHost().getProtocol().getScheme().name().toUpperCase(Locale.ROOT))
                ));
                // In one hour
                list.addAll(this.sign(region, file, this.getExpiry((int) TimeUnit.HOURS.toSeconds(1))));
                // Default signed URL expiring in 24 hours.
                list.addAll(this.sign(region, file, this.getExpiry((int) TimeUnit.SECONDS.toSeconds(
                        Preferences.instance().getInteger("s3.url.expire.seconds")))));
                // 1 Week
                list.addAll(this.sign(region, file, this.getExpiry((int) TimeUnit.DAYS.toSeconds(7))));
                // 1 Month
                list.addAll(this.sign(region, file, this.getExpiry((int) TimeUnit.DAYS.toSeconds(30))));
                // 1 Year
                list.addAll(this.sign(region, file, this.getExpiry((int) TimeUnit.DAYS.toSeconds(365))));
            }
        }
        return list;
    }

    /**
     * @param expiry Seconds
     */
    protected DescriptiveUrlBag sign(final Region region, final Path file, final long expiry) {
        final String path = region.getStorageUrl(
                containerService.getContainer(file).getName(), containerService.getKey(file)).getRawPath();
        if(!accounts.containsKey(region)) {
            log.warn(String.format("No account info for region %s available required to sign temporary URL", region));
            return DescriptiveUrlBag.empty();
        }
        // OpenStack Swift Temporary URLs (TempURL) required the X-Account-Meta-Temp-URL-Key header
        // be set on the Swift account. Used to sign.
        final AccountInfo info = accounts.get(region);
        if(StringUtils.isBlank(info.getTempUrlKey())) {
            log.warn("Missing X-Account-Meta-Temp-URL-Key header value to sign temporary URL");
            return DescriptiveUrlBag.empty();
        }
        if(log.isInfoEnabled()) {
            log.info(String.format("Using X-Account-Meta-Temp-URL-Key header value %s to sign", info.getTempUrlKey()));
        }
        final String signature = this.sign(info.getTempUrlKey(),
                String.format("GET\n%d\n%s", expiry, path));
        //Compile the temporary URL
        final DescriptiveUrlBag list = new DescriptiveUrlBag();
        for(Scheme scheme : Arrays.asList(Scheme.valueOf(region.getStorageUrl().getScheme()))) {
            final int port = region.getStorageUrl().getPort();
            list.add(new DescriptiveUrl(URI.create(String.format("%s://%s%s%s?temp_url_sig=%s&temp_url_expires=%d",
                    scheme.name(), region.getStorageUrl().getHost(),
                    port == -1 ? StringUtils.EMPTY : port == scheme.getPort() ? StringUtils.EMPTY : String.format(":%d", port),
                    path, signature, expiry)),
                    DescriptiveUrl.Type.signed,
                    MessageFormat.format(LocaleFactory.localizedString("{0} URL"), LocaleFactory.localizedString("Signed", "S3"))
                            + " (" + MessageFormat.format(LocaleFactory.localizedString("Expires {0}", "S3") + ")",
                            UserDateFormatterFactory.get().getShortFormat(expiry))
            ));
        }
        return list;
    }

    protected String sign(final String secret, final String body) {
        try {
            // Acquire an HMAC/SHA1 from the raw key bytes.
            final SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(Charset.forName("UTF-8")),
                    Constants.HMAC_SHA1_ALGORITHM);
            // Acquire the MAC instance and initialize with the signing key.
            final Mac mac = Mac.getInstance(Constants.HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            return Hex.encodeHexString(mac.doFinal(body.getBytes(Charset.forName("UTF-8"))));
        }
        catch(NoSuchAlgorithmException e) {
            log.error(String.format("Error signing %s %s", body, e.getMessage()));
            return null;
        }
        catch(InvalidKeyException e) {
            log.error(String.format("Error signing %s %s", body, e.getMessage()));
            return null;
        }
    }

    protected long getExpiry(final int seconds) {
        final Calendar expiry = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expiry.add(Calendar.SECOND, seconds);
        return expiry.getTimeInMillis();
    }
}
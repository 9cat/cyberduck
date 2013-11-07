package ch.cyberduck.core.dav;

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

import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ChecksumException;
import ch.cyberduck.core.http.HttpUploadFeature;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jets3t.service.utils.ServiceUtils;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @version $Id$
 */
public class DAVUploadFeature extends HttpUploadFeature<String, MessageDigest> {
    private static final Logger log = Logger.getLogger(DAVUploadFeature.class);

    public DAVUploadFeature(final DAVSession session) {
        super(new DAVWriteFeature(session));
    }

    @Override
    protected InputStream decorate(final InputStream in, final MessageDigest digest) {
        return new DigestInputStream(in, digest);
    }

    @Override
    protected MessageDigest digest() {
        MessageDigest digest = null;
        if(Preferences.instance().getBoolean("webdav.upload.checksum")) {
            try {
                digest = MessageDigest.getInstance("MD5");
            }
            catch(NoSuchAlgorithmException e) {
                log.error(e.getMessage());
            }
        }
        return digest;
    }

    @Override
    protected void post(final MessageDigest digest, final String etag) throws BackgroundException {
        if(StringUtils.isBlank(etag)) {
            log.warn("No ETag returned by server to verify checksum");
            return;
        }
        if(etag.matches("[a-fA-F0-9]{32}")) {
            log.warn(String.format("ETag %s returned by server does not match MD5 pattern", etag));
            return;
        }
        if(null != digest) {
            // Obtain locally-calculated MD5 hash.
            final String expected = ServiceUtils.toHex(digest.digest());
            // Compare our locally-calculated hash with the ETag returned by S3.
            if(!expected.equals(etag)) {
                throw new ChecksumException("Upload failed",
                        String.format("Mismatch between MD5 hash of uploaded data (%s) and ETag returned by the server (%s)", expected, etag));
            }
        }
    }
}

package ch.cyberduck.core.sftp;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
 *
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

import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.exception.ChecksumException;
import ch.cyberduck.core.exception.ConnectionCanceledException;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;

import java.security.PublicKey;

import net.schmizz.sshj.common.KeyType;

/**
 * Saving accepted host keys in preferences as Base64 encoded strings.
 *
 * @version $Id$
 */
public abstract class PreferencesHostKeyVerifier extends AbstractHostKeyCallback {

    @Override
    public boolean verify(String hostname, int port, PublicKey key) throws ConnectionCanceledException, ChecksumException {
        final String lookup = Preferences.instance().getProperty(
                String.format("ssh.hostkey.%s.%s", KeyType.fromKey(key), hostname));
        if(StringUtils.equals(Base64.toBase64String(key.getEncoded()), lookup)) {
            return true;
        }
        final boolean accept;
        if(null == lookup) {
            accept = this.isUnknownKeyAccepted(hostname, key);
        }
        else {
            accept = this.isChangedKeyAccepted(hostname, key);
        }
        if(accept) {
            this.allow(hostname, key, true);
        }
        return accept;
    }

    @Override
    protected void allow(final String hostname, final PublicKey key, final boolean persist) {
        if(persist) {
            Preferences.instance().setProperty(String.format("ssh.hostkey.%s.%s",
                    KeyType.fromKey(key), hostname), Base64.toBase64String(key.getEncoded()));
        }
    }
}

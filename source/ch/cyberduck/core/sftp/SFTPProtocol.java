package ch.cyberduck.core.sftp;

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

import ch.cyberduck.core.AbstractProtocol;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.i18n.Locale;

/**
 * @version $Id:$
 */
public final class SFTPProtocol extends AbstractProtocol {

    @Override
    public Type getType() {
        return Type.ssh;
    }

    @Override
    public String getIdentifier() {
        return this.getScheme().name();
    }

    @Override
    public String getDescription() {
        return Locale.localizedString("SFTP (SSH File Transfer Protocol)");
    }

    @Override
    public Scheme getScheme() {
        return Scheme.sftp;
    }

    @Override
    public boolean isEncodingConfigurable() {
        return true;
    }

    @Override
    public boolean isAnonymousConfigurable() {
        return false;
    }

    @Override
    public String disk() {
        return String.format("%s.tiff", "ftp");
    }

    @Override
    public SFTPSession createSession(final Host host) {
        return new SFTPSession(host);
    }
}

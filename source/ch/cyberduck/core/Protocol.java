package ch.cyberduck.core;

/*
 *  Copyright (c) 2007 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.dav.DAVProtocol;
import ch.cyberduck.core.dav.DAVSSLProtocol;
import ch.cyberduck.core.ftp.FTPProtocol;
import ch.cyberduck.core.ftp.FTPTLSProtocol;
import ch.cyberduck.core.gstorage.GoogleStorageProtocol;
import ch.cyberduck.core.openstack.CloudfilesProtocol;
import ch.cyberduck.core.openstack.SwiftProtocol;
import ch.cyberduck.core.s3.S3Protocol;
import ch.cyberduck.core.sftp.SFTPProtocol;

import org.apache.commons.lang.StringUtils;

import java.util.Set;

/**
 * @version $Id$
 */
public interface Protocol {

    public static final Protocol FTP = new FTPProtocol();
    public static final Protocol FTP_TLS = new FTPTLSProtocol();
    public static final Protocol SFTP = new SFTPProtocol();
    public static final Protocol S3_SSL = new S3Protocol();
    public static final Protocol WEBDAV = new DAVProtocol();
    public static final Protocol WEBDAV_SSL = new DAVSSLProtocol();
    public static final Protocol CLOUDFILES = new CloudfilesProtocol();
    public static final Protocol SWIFT = new SwiftProtocol();
    public static final Protocol GOOGLESTORAGE_SSL = new GoogleStorageProtocol();

    Session createSession(Host host);

    public enum Type {
        ftp {
            /**
             * Allows empty string for password.
             *
             * @return True if username is not blank and password is not null
             */
            @Override
            public boolean validate(final Credentials credentials, final LoginOptions options) {
                // Allow empty passwords
                return StringUtils.isNotBlank(credentials.getUsername()) && null != credentials.getPassword();
            }
        },
        ssh {
            @Override
            public boolean validate(Credentials credentials, final LoginOptions options) {
                if(credentials.isPublicKeyAuthentication()) {
                    return StringUtils.isNotBlank(credentials.getUsername());
                }
                return super.validate(credentials, options);
            }
        },
        s3,
        googlestorage {
            @Override
            public boolean validate(final Credentials credentials, final LoginOptions options) {
                // OAuth only requires the project token
                return StringUtils.isNotBlank(credentials.getUsername());
            }
        },
        swift,
        dav;

        /**
         * Check login credentials for validity for this protocol.
         *
         * @param credentials Login credentials
         * @param options     Options
         * @return True if username is not a blank string and password is not empty ("") and not null.
         */
        public boolean validate(Credentials credentials, final LoginOptions options) {
            if(options.user) {
                if(StringUtils.isBlank(credentials.getUsername())) {
                    return false;
                }
            }
            if(options.password) {
                if(StringUtils.isEmpty(credentials.getPassword())) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * @return True if anonymous logins are possible.
     */
    boolean isAnonymousConfigurable();

    boolean isHostnameConfigurable();

    /**
     * @return False if the port to connect is static.
     */
    boolean isPortConfigurable();

    boolean isWebUrlConfigurable();

    /**
     * @return True if the character set is not defined in the protocol.
     */
    boolean isEncodingConfigurable();

    /**
     * @return True if protocol uses UTC timezone for timestamps
     */
    boolean isUTCTimezone();

    /**
     * @return Locations for containers
     */
    Set<String> getRegions();

    /**
     * @return Human readable short name
     */
    String getName();

    /**
     * @return Available in connection selection
     */
    boolean isEnabled();

    /**
     * @return True if the protocol is inherently secure.
     */
    boolean isSecure();

    /**
     * Provider identification
     *
     * @return Identifier if no vendor specific profile
     * @see #getIdentifier()
     */
    String getProvider();

    /**
     * @return Protocol family
     */
    Type getType();

    /**
     * Must be unique across all available protocols.
     *
     * @return The identifier for this protocol which is the scheme by default
     */
    String getIdentifier();

    /**
     * @return Human readable description
     */
    String getDescription();


    /**
     * @return Protocol scheme
     */
    Scheme getScheme();

    /**
     * @return Protocol schemes
     */
    String[] getSchemes();

    /**
     * @return Default hostname for server
     */
    String getDefaultHostname();

    /**
     * @return Default port for server
     */
    int getDefaultPort();

    /**
     * @return Authentication context path
     */
    String getContext();

    /**
     * @return A mounted disk icon to display
     */
    String disk();

    /**
     * @return Replacement for small disk icon
     */
    String icon();

    String favicon();

    /**
     * @return Username label
     */
    String getUsernamePlaceholder();

    /**
     * @return Password label
     */
    String getPasswordPlaceholder();
}
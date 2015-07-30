package ch.cyberduck.core.importer;

/*
 * Copyright (c) 2002-2014 David Kocher. All rights reserved.
 * http://cyberduck.io/
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
 * feedback@cyberduck.io
 */

import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.preferences.PreferencesFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

/**
 * @version $Id$
 */
public class CrossFtpBookmarkCollection extends XmlBookmarkCollection {
    private static final Logger log = Logger.getLogger(CrossFtpBookmarkCollection.class);

    private static final long serialVersionUID = 7442378118872253601L;

    @Override
    public String getBundleIdentifier() {
        return "com.crossftp";
    }

    @Override
    public String getName() {
        return "CrossFTP";
    }

    @Override
    public Local getFile() {
        return LocalFactory.get(PreferencesFactory.get().getProperty("bookmark.import.crossftp.location"));
    }

    @Override
    protected void parse(Local file) throws AccessDeniedException {
        this.read(file);
    }

    @Override
    protected AbstractHandler getHandler() {
        return new ServerHandler();
    }

    /**
     * Parser for Filezilla Site Manager.
     */
    private class ServerHandler extends AbstractHandler {
        private Host current = null;

        @Override
        public void startElement(String uri, String name, String qName, Attributes attrs) {
            if(name.equals("site")) {
                current = new Host(attrs.getValue("hName"));
                current.setNickname(attrs.getValue("name"));
                current.getCredentials().setUsername(attrs.getValue("un"));
                current.setWebURL(attrs.getValue("wURL"));
                current.setComment(attrs.getValue("comm"));
                current.setDefaultPath(attrs.getValue("path"));
                String protocol = attrs.getValue("ftpPType");
                try {
                    switch(Integer.valueOf(protocol)) {
                        case 1:
                            current.setProtocol(ProtocolFactory.FTP);
                            break;
                        case 2:
                        case 3:
                        case 4:
                            current.setProtocol(ProtocolFactory.FTP_TLS);
                            break;
                        case 6:
                            current.setProtocol(ProtocolFactory.WEBDAV);
                            break;
                        case 7:
                            current.setProtocol(ProtocolFactory.WEBDAV_SSL);
                            break;
                        case 8:
                        case 9:
                            current.setProtocol(ProtocolFactory.S3_SSL);
                            break;
                    }
                }
                catch(NumberFormatException e) {
                    log.warn("Unknown protocol:" + e.getMessage());
                }
                try {
                    current.setPort(Integer.parseInt(attrs.getValue("port")));
                }
                catch(NumberFormatException e) {
                    log.warn("Invalid Port:" + e.getMessage());
                }
            }
            super.startElement(uri, name, qName, attrs);
        }

        @Override
        public void startElement(String name, Attributes attrs) {
            //
        }

        @Override
        public void endElement(String name, String elementText) {
            if(name.equals("site")) {
                add(current);
            }
        }
    }
}
package ch.cyberduck.core.ftp;

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

import ch.cyberduck.core.Path;
import ch.cyberduck.core.date.MDTMSecondsDateFormatter;
import ch.cyberduck.core.date.UserDateFormatterFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.FTPExceptionMappingService;
import ch.cyberduck.core.features.Timestamp;
import ch.cyberduck.core.i18n.Locale;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.TimeZone;

/**
 * @version $Id$
 */
public class FTPMFMTTimestampFeature implements Timestamp {
    private static final Logger log = Logger.getLogger(FTPMFMTTimestampFeature.class);

    private FTPSession session;

    private FTPException failure;

    public FTPMFMTTimestampFeature(final FTPSession session) {
        this.session = session;
    }

    @Override
    public void setTimestamp(final Path file, final Long created, final Long modified, final Long accessed) throws BackgroundException {
        if(failure != null) {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Skip setting timestamp for %s due to previous failure %s", file, failure.getMessage()));
            }
            throw new FTPExceptionMappingService().map("Cannot change timestamp", failure, file);
        }
        session.message(MessageFormat.format(Locale.localizedString("Changing timestamp of {0} to {1}", "Status"),
                file.getName(), UserDateFormatterFactory.get().getShortFormat(modified)));
        try {
            final MDTMSecondsDateFormatter formatter = new MDTMSecondsDateFormatter();
            if(!session.getClient().setModificationTime(file.getAbsolute(),
                    formatter.format(modified, TimeZone.getTimeZone("UTC")))) {
                throw failure = new FTPException(session.getClient().getReplyCode(),
                        session.getClient().getReplyString());
            }
        }
        catch(IOException e) {
            throw new FTPExceptionMappingService().map("Cannot change timestamp", e, file);
        }
    }
}
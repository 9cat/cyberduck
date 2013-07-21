package ch.cyberduck.core.filter;

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
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.Filter;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Local;

import org.apache.log4j.Logger;

import java.util.regex.Pattern;

/**
 * @version $Id$
 */
public class UploadRegexFilter implements Filter<Local> {
    private static final Logger log = Logger.getLogger(UploadRegexFilter.class);

    private final Pattern pattern
            = Pattern.compile(Preferences.instance().getProperty("queue.upload.skip.regex"));

    @Override
    public boolean accept(final Local file) {
        if(file.attributes().isDuplicate()) {
            return false;
        }
        if(Preferences.instance().getBoolean("queue.upload.skip.enable")) {
            if(pattern.matcher(file.getName()).matches()) {
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Skip %s excluded with regex", file.getAbsolute()));
                }
                return false;
            }
        }
        return true;
    }
}

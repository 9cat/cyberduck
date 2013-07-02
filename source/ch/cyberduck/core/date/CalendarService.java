package ch.cyberduck.core.date;

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

import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * @version $Id$
 */
public class CalendarService implements DateDomainService<Calendar> {
    private static Logger log = Logger.getLogger(CalendarService.class);

    /**
     * @param timestamp Milliseconds
     *                  #see Calendar#MILLISECOND
     *                  #see Calendar#SECOND
     *                  #see Calendar#MINUTE
     *                  #see Calendar#HOUR
     * @return Calendar from milliseconds
     */
    @Override
    public Calendar asDate(final long timestamp, final Instant precision) {
        if(log.isDebugEnabled()) {
            log.debug("asCalendar:" + timestamp);
        }
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(timestamp);
        if(precision == Instant.MILLISECOND) {
            return c;
        }
        c.clear(Calendar.MILLISECOND);
        if(precision == Instant.SECOND) {
            return c;
        }
        c.clear(Calendar.SECOND);
        if(precision == Instant.MINUTE) {
            return c;
        }
        c.clear(Calendar.MINUTE);
        if(precision == Instant.HOUR) {
            return c;
        }
        c.clear(Calendar.HOUR);
        return c;
    }
}

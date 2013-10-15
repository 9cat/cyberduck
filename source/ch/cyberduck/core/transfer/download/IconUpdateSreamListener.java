package ch.cyberduck.core.transfer.download;

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
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.ch
 */

import ch.cyberduck.core.Local;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.io.DisabledStreamListener;
import ch.cyberduck.core.local.IconService;
import ch.cyberduck.core.local.IconServiceFactory;
import ch.cyberduck.core.transfer.TransferStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @version $Id$
 */
public class IconUpdateSreamListener extends DisabledStreamListener {

    private TransferStatus status;

    private Local file;

    private final IconService icon
            = IconServiceFactory.get();

    // An integer between 0 and 9
    private int step = 0;

    // Only update the file custom icon if the size is > 5MB. Otherwise creating too much
    // overhead when transferring a large amount of files
    private boolean threshold;

    private boolean enabled = Preferences.instance().getBoolean("queue.download.icon.update");

    public IconUpdateSreamListener(final TransferStatus status, final Local file) {
        this.status = status;
        this.threshold = status.getLength() > Preferences.instance().getLong("queue.download.icon.threshold");
        this.file = file;
    }

    @Override
    public void bytesReceived(long bytes) {
        if(enabled && threshold) {
            final BigDecimal fraction = new BigDecimal(status.getCurrent()).divide(new BigDecimal(status.getLength()), 1, RoundingMode.DOWN);
            if(fraction.multiply(BigDecimal.TEN).intValue() > step) {
                // Another 10 percent of the file has been transferred
                icon.set(file, status);
                step++;
            }
        }
    }
}

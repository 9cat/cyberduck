package ch.cyberduck.core.googledrive;

/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
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
 */

import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.IdProvider;
import ch.cyberduck.core.shared.DefaultTimestampFeature;

import java.io.IOException;

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.File;

public class DriveTimestampFeature extends DefaultTimestampFeature {

    private final DriveSession session;

    public DriveTimestampFeature(final DriveSession session) {
        this.session = session;
    }

    @Override
    public void setTimestamp(final Path file, final Long modified) throws BackgroundException {
        try {
            final String fileid = session.getFeature(IdProvider.class).getFileid(file);
            final File properties = new File();
            properties.setModifiedTime(new DateTime(modified));
            session.getClient().files().update(fileid, properties).setFields("modifiedTime").execute();
        }
        catch(IOException e) {
            throw new DriveExceptionMappingService().map("Failure to write attributes of {0}", e, file);
        }
    }
}

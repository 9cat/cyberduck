package ch.cyberduck.core.local;

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
 * feedback@cyberduck.ch
 */

import ch.cyberduck.core.DefaultPathReference;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Preferences;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.UUID;

/**
 * @version $Id$
 */
public class TemporaryFileService {

    protected TemporaryFileService() {
        //
    }

    public static void register() {
        TemporaryFileServiceFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private static class Factory extends TemporaryFileServiceFactory {
        @Override
        protected TemporaryFileService create() {
            return new TemporaryFileService();
        }
    }

    public Local create(final Path file) {
        return this.create(UUID.randomUUID().toString(), file);
    }

    public Local create(final String uid, final Path file) {
        final Local folder = LocalFactory.createLocal(
                new File(Preferences.instance().getProperty("tmp.dir"),
                        uid + String.valueOf(Path.DELIMITER) + file.getParent().getAbsolute()));
        return LocalFactory.createLocal(folder, FilenameUtils.getName(new DefaultPathReference(file).unique()));
    }
}

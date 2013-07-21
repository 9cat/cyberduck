package ch.cyberduck.core.local;

/*
 * Copyright (c) 2012 David Kocher. All rights reserved.
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

import ch.cyberduck.core.Local;
import ch.cyberduck.ui.cocoa.application.NSImage;
import ch.cyberduck.ui.cocoa.application.NSWorkspace;
import ch.cyberduck.ui.resources.IconCacheFactory;

import org.rococoa.cocoa.foundation.NSUInteger;

/**
 * @version $Id$
 */
public final class WorkspaceIconService implements IconService {

    public static void register() {
        IconServiceFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private static class Factory extends IconServiceFactory {
        @Override
        protected IconService create() {
            return new WorkspaceIconService();
        }
    }

    private WorkspaceIconService() {
        //
    }

    @Override
    public boolean set(final Local file, final String image) {
        return this.update(file, IconCacheFactory.<NSImage>get().iconNamed(image));
    }

    protected boolean update(final Local file, final NSImage icon) {
        synchronized(NSWorkspace.class) {
            // Specify 0 if you want to generate icons in all available icon representation formats
            return NSWorkspace.sharedWorkspace().setIcon_forFile_options(icon, file.getAbsolute(), new NSUInteger(0));
        }
    }

    @Override
    public boolean set(final Local file, final int progress) {
        return this.set(file, String.format("download%d.icns", progress));
    }

    @Override
    public boolean remove(final Local file) {
        return this.update(file, null);
    }
}
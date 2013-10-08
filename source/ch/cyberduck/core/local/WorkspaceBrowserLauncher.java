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

import ch.cyberduck.ui.cocoa.application.NSWorkspace;
import ch.cyberduck.ui.cocoa.foundation.NSURL;

import org.apache.commons.lang3.StringUtils;

/**
 * @version $Id$
 */
public class WorkspaceBrowserLauncher implements BrowserLauncher {

    public static void register() {
        BrowserLauncherFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private static class Factory extends BrowserLauncherFactory {
        @Override
        protected BrowserLauncher create() {
            return new WorkspaceBrowserLauncher();
        }
    }

    @Override
    public boolean open(final String url) {
        synchronized(NSWorkspace.class) {
            if(StringUtils.isNotBlank(url)) {
                if(NSWorkspace.sharedWorkspace().openURL(NSURL.URLWithString(url))) {
                    return true;
                }
            }
            return false;
        }
    }
}
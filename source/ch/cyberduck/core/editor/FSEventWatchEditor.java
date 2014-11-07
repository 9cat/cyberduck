package ch.cyberduck.core.editor;

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
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.io.watchservice.FSEventWatchService;
import ch.cyberduck.core.local.Application;
import ch.cyberduck.core.local.FileWatcher;
import ch.cyberduck.ui.Controller;

import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * An editor listing for file system notifications on a particular folder
 *
 * @version $Id$
 */
public class FSEventWatchEditor extends BrowserBackgroundEditor {
    private static final Logger log = Logger.getLogger(FSEventWatchEditor.class);

    private FileWatcher monitor
            = new FileWatcher(new FSEventWatchService());

    /**
     * With custom editor for file type.
     *
     * @param controller  Browser
     * @param application Editor application
     * @param file        Remote file
     */
    public FSEventWatchEditor(final Controller controller, final Session session,
                              final Application application, final Path file) {
        super(controller, session, application, file);
    }

    public void watch(final Local local) throws IOException {
        try {
            monitor.register(local, new DefaultEditorListener(this)).await();
        }
        catch(InterruptedException e) {
            throw new IOException(String.format("Failure monitoring file %s", local), e);
        }
    }

    @Override
    public void delete() {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Close monitor %s", monitor));
        }
        monitor.close();
        super.delete();
    }
}
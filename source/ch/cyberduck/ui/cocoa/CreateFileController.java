package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.editor.Editor;
import ch.cyberduck.core.editor.EditorFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Touch;
import ch.cyberduck.ui.cocoa.application.NSAlert;
import ch.cyberduck.ui.cocoa.application.NSImage;
import ch.cyberduck.ui.cocoa.threading.BrowserControllerBackgroundAction;
import ch.cyberduck.ui.resources.IconCacheFactory;

import java.text.MessageFormat;
import java.util.Collections;

/**
 * @version $Id$
 */
public class CreateFileController extends FileController {

    public CreateFileController(final WindowController parent, final Cache cache) {
        super(parent, cache, NSAlert.alert(
                LocaleFactory.localizedString("Create new file", "File"),
                LocaleFactory.localizedString("Enter the name for the new file:", "File"),
                LocaleFactory.localizedString("Create", "File"),
                EditorFactory.instance().getDefaultEditor() != null ? LocaleFactory.localizedString("Edit", "File") : null,
                LocaleFactory.localizedString("Cancel", "File")
        ));
        alert.setIcon(IconCacheFactory.<NSImage>get().documentIcon(null, 64));
    }

    @Override
    public void callback(final int returncode) {
        if(returncode == DEFAULT_OPTION) {
            this.createFile(this.getWorkdir(), inputField.stringValue(), false);
        }
        else if(returncode == ALTERNATE_OPTION) {
            this.createFile(this.getWorkdir(), inputField.stringValue(), true);
        }
    }

    protected void createFile(final Path workdir, final String filename, final boolean edit) {
        final BrowserController c = (BrowserController) parent;
        final Path file = new Path(workdir, filename, Path.FILE_TYPE);
        c.background(new BrowserControllerBackgroundAction<Path>(c) {
            @Override
            public Path run() throws BackgroundException {
                final Session<?> session = c.getSession();
                final Touch feature = session.getFeature(Touch.class);
                feature.touch(file);
                if(edit) {
                    file.attributes().setSize(0L);
                    Editor editor = EditorFactory.instance().create(c, session, file);
                    editor.open();
                }
                return file;
            }

            @Override
            public String getActivity() {
                return MessageFormat.format(LocaleFactory.localizedString("Uploading {0}", "Status"),
                        file.getName());
            }

            @Override
            public void cleanup() {
                super.cleanup();
                if(filename.charAt(0) == '.') {
                    c.setShowHiddenFiles(true);
                }
                c.reloadData(Collections.singletonList(file), Collections.singletonList(file));
            }
        });
    }
}
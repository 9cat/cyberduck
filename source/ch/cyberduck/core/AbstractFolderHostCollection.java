package ch.cyberduck.core;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
 *
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

import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.serializer.Reader;
import ch.cyberduck.core.serializer.Writer;

import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public abstract class AbstractFolderHostCollection extends AbstractHostCollection {
    private static final Logger log = Logger.getLogger(AbstractFolderHostCollection.class);

    private static final long serialVersionUID = 6598370606581477494L;

    private Writer<Host> writer = HostWriterFactory.get();

    private Reader<Host> reader = HostReaderFactory.get();

    protected Local folder;

    /**
     * Reading bookmarks from this folder
     *
     * @param f Parent directory to look for bookmarks
     */
    public AbstractFolderHostCollection(final Local f) {
        this.folder = f;
    }

    @Override
    public String getName() {
        return LocaleFactory.localizedString(folder.getName());
    }

    /**
     * @param bookmark Bookmark
     * @return File for bookmark
     */
    public Local getFile(final Host bookmark) {
        return LocalFactory.createLocal(folder, String.format("%s.duck", bookmark.getUuid()));
    }

    public Local getFolder() {
        return folder;
    }

    @Override
    public void collectionItemAdded(final Host bookmark) {
        writer.write(bookmark, this.getFile(bookmark));
        super.collectionItemAdded(bookmark);
    }

    @Override
    public void collectionItemRemoved(final Host bookmark) {
        try {
            this.getFile(bookmark).delete();
        }
        catch(AccessDeniedException e) {
            log.error(e.getMessage());
        }
        finally {
            super.collectionItemRemoved(bookmark);
        }
    }

    @Override
    public void collectionItemChanged(final Host bookmark) {
        writer.write(bookmark, this.getFile(bookmark));
        super.collectionItemChanged(bookmark);
    }

    @Override
    public void load() throws AccessDeniedException {
        if(log.isInfoEnabled()) {
            log.info(String.format("Reloading %s", folder.getAbsolute()));
        }
        this.lock();
        try {
            folder.mkdir();
            final AttributedList<Local> bookmarks = folder.list().filter(
                    new Filter<Local>() {
                        @Override
                        public boolean accept(final Local file) {
                            return file.getName().endsWith(".duck");
                        }
                    }
            );
            for(Local next : bookmarks) {
                final Host bookmark = reader.read(next);
                if(null == bookmark) {
                    continue;
                }
                // Legacy support.
                if(!this.getFile(bookmark).equals(next)) {
                    this.rename(next, bookmark);

                }
                this.add(bookmark);
            }
            // Sort using previously built index
            this.sort();
        }
        finally {
            this.unlock();
        }
        super.load();
    }

    protected void rename(final Local next, final Host bookmark) throws AccessDeniedException {
        // Rename all files previously saved with nickname to UUID.
        next.rename(this.getFile(bookmark));
    }

    @Override
    public void save() {
        // Save individual bookmarks upon add but not collection itself.
    }
}

package ch.cyberduck.core.transfer;

/*
 *  Copyright (c) 2007 David Kocher. All rights reserved.
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

import ch.cyberduck.core.LocaleFactory;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @version $Id$
 */
public abstract class TransferAction {

    private static final Map<String, TransferAction> registry
            = new HashMap<String, TransferAction>();

    public static TransferAction forName(final String name) {
        return registry.get(name);
    }

    private String name;

    public TransferAction(final String name) {
        registry.put(name, this);
        this.name = name;
    }

    public abstract String getTitle();

    public abstract String getDescription();

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return this.name();
    }

    /**
     * Overwrite any prior existing file
     */
    public static final TransferAction overwrite = new TransferAction("overwrite") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Overwrite");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Overwrite all files", "Transfer");
        }
    };

    /**
     * Append to any existing file when writing
     */
    public static final TransferAction resume = new TransferAction("resume") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Resume");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Append existing files", "Transfer");
        }
    };

    /**
     * Create a new file with a similar name
     */
    public static final TransferAction rename = new TransferAction("similar") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Rename");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Rename transferred files with a timestamp appended to the filename", "Transfer");
        }
    };

    /**
     * Create a new file with a similar name
     */
    public static final TransferAction renameexisting = new TransferAction("rename") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Rename existing");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Rename existing files with timestamp appended to filename", "Transfer");
        }
    };

    /**
     * Do not transfer file
     */
    public static final TransferAction skip = new TransferAction("skip") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Skip");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Skip transfer of files that already exist", "Transfer");
        }
    };

    /**
     * Prompt the user about existing files
     */
    public static final TransferAction callback = new TransferAction("ask") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Prompt");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Prompt for action on every file", "Transfer");
        }
    };

    /**
     * Automatically decide the transfer action using the comparision service for paths.
     */
    public static final TransferAction comparison = new TransferAction("compare") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Compare");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Skip files that match size, modification date or checksum", "Transfer");
        }
    };

    public static final TransferAction cancel = new TransferAction("cancel") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Cancel");
        }

        @Override
        public String getDescription() {
            return StringUtils.EMPTY;
        }
    };

    /**
     * Synchronize action
     */
    public static final TransferAction download = new TransferAction("download") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Download");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Download changed and missing files", "Transfer");
        }
    };

    /**
     * Synchronize action
     */
    public static final TransferAction upload = new TransferAction("upload") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Upload");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Upload changed and missing files", "Transfer");
        }
    };

    /**
     * Synchronize action
     */
    public static final TransferAction mirror = new TransferAction("mirror") {
        @Override
        public String getTitle() {
            return LocaleFactory.localizedString("Mirror");
        }

        @Override
        public String getDescription() {
            return LocaleFactory.localizedString("Download and Upload", "Transfer");
        }
    };

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final TransferAction that = (TransferAction) o;
        if(name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
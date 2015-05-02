package ch.cyberduck.core.worker;

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

import ch.cyberduck.core.Acl;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.ProgressListener;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.features.AclPermission;

import java.text.MessageFormat;
import java.util.List;

/**
 * @version $Id$
 */
public class WriteAclWorker extends Worker<Boolean> {

    private Session<?> session;

    private AclPermission feature;

    /**
     * Selected files.
     */
    private List<Path> files;

    /**
     * Permissions to apply to files.
     */
    private Acl acl;

    /**
     * Descend into directories
     */
    private boolean recursive;

    private ProgressListener listener;

    public WriteAclWorker(final Session session, final AclPermission feature, final List<Path> files,
                          final Acl acl, final boolean recursive,
                          final ProgressListener listener) {
        this.session = session;
        this.feature = feature;
        this.files = files;
        this.acl = acl;
        this.recursive = recursive;
        this.listener = listener;
    }

    @Override
    public Boolean run() throws BackgroundException {
        for(Path file : files) {
            this.write(file);
        }
        return true;
    }

    protected void write(final Path file) throws BackgroundException {
        if(this.isCanceled()) {
            throw new ConnectionCanceledException();
        }
        if(!acl.equals(file.attributes().getAcl())) {
            listener.message(MessageFormat.format(LocaleFactory.localizedString("Changing permission of {0} to {1}", "Status"),
                    file.getName(), acl));
            feature.setPermission(file, acl);
            file.attributes().setAcl(acl);
        }
        if(recursive) {
            if(file.isVolume()) {
                // No recursion when changing container ACL
            }
            else if(file.isDirectory()) {
                for(Path child : session.list(file, new ActionListProgressListener(this, listener))) {
                    this.write(child);
                }
            }
        }
    }

    @Override
    public Boolean initialize() {
        return false;
    }

    @Override
    public String getActivity() {
        return MessageFormat.format(LocaleFactory.localizedString("Changing permission of {0} to {1}", "Status"),
                this.toString(files), acl);
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final WriteAclWorker that = (WriteAclWorker) o;
        if(files != null ? !files.equals(that.files) : that.files != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return files != null ? files.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WriteAclWorker{");
        sb.append("files=").append(files);
        sb.append('}');
        return sb.toString();
    }
}

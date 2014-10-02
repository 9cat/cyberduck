package ch.cyberduck.ui.action;

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

import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.ProgressListener;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.features.Headers;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * @version $Id$
 */
public abstract class WriteMetadataWorker extends Worker<Boolean> {

    private Session<?> session;

    private Headers feature;

    /**
     * Selected files.
     */
    private List<Path> files;

    /**
     * The updated metadata to apply
     */
    private Map<String, String> metadata;

    private ProgressListener listener;

    protected WriteMetadataWorker(final Session session, final Headers feature,
                                  final List<Path> files, final Map<String, String> metadata,
                                  final ProgressListener listener) {
        this.session = session;
        this.feature = feature;
        this.files = files;
        this.metadata = metadata;
        this.listener = listener;
    }

    @Override
    public Boolean run() throws BackgroundException {
        for(Path file : files) {
            if(this.isCanceled()) {
                throw new ConnectionCanceledException();
            }
            if(!metadata.equals(file.attributes().getMetadata())) {
                for(Map.Entry<String, String> entry : metadata.entrySet()) {
                    // Prune metadata from entries which are unique to a single file. For example md5-hash.
                    if(StringUtils.isBlank(entry.getValue())) {
                        // Reset with previous value
                        metadata.put(entry.getKey(), file.attributes().getMetadata().get(entry.getKey()));
                    }
                }
                listener.message(MessageFormat.format(LocaleFactory.localizedString("Writing metadata of {0}", "Status"),
                        file.getName()));
                feature.setMetadata(file, metadata);
                file.attributes().setMetadata(metadata);
            }
        }
        return true;
    }

    @Override
    public String getActivity() {
        return MessageFormat.format(LocaleFactory.localizedString("Writing metadata of {0}", "Status"),
                this.toString(files));
    }

    @Override
    public Boolean initialize() {
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final WriteMetadataWorker that = (WriteMetadataWorker) o;
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
        final StringBuilder sb = new StringBuilder("WriteMetadataWorker{");
        sb.append("files=").append(files);
        sb.append('}');
        return sb.toString();
    }
}
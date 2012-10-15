package ch.cyberduck.core.transfer;

/*
 * Copyright (c) 2002-2011 David Kocher. All rights reserved.
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

import ch.cyberduck.core.AbstractStreamListener;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathFactory;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.serializer.Serializer;
import ch.cyberduck.core.transfer.copy.CopyTransferFilter;

import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @version $Id$
 */
public class CopyTransfer extends Transfer {
    private static final Logger log = Logger.getLogger(CopyTransfer.class);

    /**
     * Mapping source to destination files
     */
    private Map<Path, Path> files = Collections.emptyMap();

    private Session destination;

    /**
     * @param files Source to destination mapping
     */
    public CopyTransfer(Map<Path, Path> files) {
        super(new ArrayList<Path>(files.keySet()),
                new BandwidthThrottle(Preferences.instance().getFloat("queue.download.bandwidth.bytes")));
        this.files = files;
        this.destination = files.values().iterator().next().getSession();
    }

    public <T> CopyTransfer(T dict, Session s) {
        super(dict, s, new BandwidthThrottle(Preferences.instance().getFloat("queue.download.bandwidth.bytes")));
    }

    @Override
    public boolean isResumable() {
        return false;
    }

    @Override
    public boolean isReloadable() {
        return false;
    }

    @Override
    public <T> T getAsDictionary() {
        final Serializer dict = super.getSerializer();
        dict.setStringForKey(String.valueOf(KIND_COPY), "Kind");
        return dict.getSerialized();
    }

    @Override
    public List<Session> getSessions() {
        final ArrayList<Session> sessions = new ArrayList<Session>(super.getSessions());
        if(destination != null) {
            sessions.add(destination);
        }
        return sessions;
    }

    /**
     * Prunes the map of selected files. Files which are a child of an already included directory
     * are removed from the returned map.
     */
    @Override
    protected void normalize() {
        log.debug("normalize");
        final Map<Path, Path> normalized = new HashMap<Path, Path>();
        Iterator<Path> sourcesIter = files.keySet().iterator();
        Iterator<Path> destinationsIter = files.values().iterator();
        while(sourcesIter.hasNext()) {
            Path f = sourcesIter.next();
            Path r = destinationsIter.next();
            boolean duplicate = false;
            for(Iterator<Path> normalizedIter = normalized.keySet().iterator(); normalizedIter.hasNext(); ) {
                Path n = normalizedIter.next();
                if(f.isChild(n)) {
                    // The selected file is a child of a directory
                    // already included for deletion
                    duplicate = true;
                    break;
                }
                if(n.isChild(f)) {
                    // Remove the previously added file as it is a child
                    // of the currently evaluated file
                    normalizedIter.remove();
                }
            }
            if(!duplicate) {
                normalized.put(f, r);
            }
        }
        this.files = normalized;
        this.setRoots(new ArrayList<Path>(files.keySet()));
    }

    @Override
    public TransferAction action(boolean resumeRequested, boolean reloadRequested) {
        return TransferAction.ACTION_OVERWRITE;
    }

    @Override
    public TransferPathFilter filter(final TransferAction action) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Filter transfer with action %s", action.toString()));
        }
        if(action.equals(TransferAction.ACTION_OVERWRITE)) {
            return new CopyTransferFilter(files);
        }
        return super.filter(action);
    }

    @Override
    public AttributedList<Path> children(Path source) {
        final AttributedList<Path> list = source.children();
        final Path target = files.get(source);
        for(Path p : list) {
            files.put(p, PathFactory.createPath(destination,
                    target.getAbsolute(), p.getName(), p.attributes().getType()));
        }
        return list;
    }

    @Override
    protected void transfer(final Path source, final TransferOptions options, final TransferStatus status) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Transfer file %s with options %s", source, options));
        }
        final Path destination = files.get(source);
        if(source.attributes().isFile()) {
            source.copy(destination, this.getBandwidth(), new AbstractStreamListener() {
                @Override
                public void bytesReceived(long bytes) {
                    transferred += bytes;
                }

                @Override
                public void bytesSent(long bytes) {
                    transferred += bytes;
                }
            }, status);
        }
        else {
            if(destination.getSession().isCreateFolderSupported(destination)) {
                destination.mkdir();
            }
        }
    }

    @Override
    public String getName() {
        return MessageFormat.format(Locale.localizedString("Copying {0} to {1}", "Status"),
                files.keySet().iterator().next().getName(), files.values().iterator().next().getName());
    }

    @Override
    public String getStatus() {
        return this.isComplete() ? "Copy complete" : "Transfer incomplete";
    }

    @Override
    public String getImage() {
        return "transfer-upload.tiff";
    }
}
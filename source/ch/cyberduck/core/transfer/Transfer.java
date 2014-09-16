package ch.cyberduck.core.transfer;

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

import ch.cyberduck.core.DescriptiveUrl;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Serializable;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.serializer.Serializer;
import ch.cyberduck.core.shared.DefaultUrlProvider;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @version $Id$
 */
public abstract class Transfer implements Serializable {
    private static final Logger log = Logger.getLogger(Transfer.class);

    /**
     * Files and folders initially selected to be part of this transfer
     */
    protected List<TransferItem> roots;

    /**
     * The sum of the file length of all files in the <code>queue</code> or null if unknown
     */
    private Long size;

    /**
     * The number bytes already transferred of the files in the <code>queue</code> or null if unknown
     */
    private Long transferred;

    public abstract Type getType();

    public enum Type {
        download {
            @Override
            public boolean isReloadable() {
                return true;
            }
        },
        upload {
            @Override
            public boolean isReloadable() {
                return true;
            }
        },
        sync {
            @Override
            public boolean isReloadable() {
                return true;
            }
        },
        copy {
            @Override
            public boolean isReloadable() {
                return true;
            }
        },
        move {
            @Override
            public boolean isReloadable() {
                return false;
            }
        };

        public abstract boolean isReloadable();

    }

    protected Host host;

    /**
     * In Bytes per second
     */
    protected BandwidthThrottle bandwidth
            = new BandwidthThrottle(BandwidthThrottle.UNLIMITED);


    /**
     * The transfer has been reset
     */
    private boolean reset;

    /**
     * Last transferred timestamp
     */
    private Date timestamp;

    /**
     * Unique identifier
     */
    private String uuid;

    /**
     * Transfer state
     */
    private State state = State.stopped;

    private enum State {
        running,
        stopped
    }

    /**
     * @return True if in <code>running</code> state
     */
    public boolean isRunning() {
        return state == State.running;
    }

    /**
     * Create a transfer with a single root which can
     * be a plain file or a directory
     *
     * @param host Connection details
     * @param root File or directory
     */
    public Transfer(final Host host, final Path root, final Local folder, final BandwidthThrottle bandwidth) {
        this(host, Collections.singletonList(new TransferItem(root, folder)), bandwidth);
    }

    /**
     * @param host  Connection details
     * @param roots List of files to add to transfer
     */
    public Transfer(final Host host, final List<TransferItem> roots, final BandwidthThrottle bandwidth) {
        this.host = host;
        this.roots = roots;
        this.bandwidth = bandwidth;
    }

    public <T> T serialize(final Serializer dict) {
        dict.setStringForKey(String.valueOf(this.getType().name()), "Type");
        dict.setObjectForKey(host, "Host");
        dict.setListForKey(roots, "Items");
        dict.setStringForKey(this.getUuid(), "UUID");
        dict.setStringForKey(String.valueOf(this.getSize()), "Size");
        dict.setStringForKey(String.valueOf(this.getTransferred()), "Current");
        if(timestamp != null) {
            dict.setStringForKey(String.valueOf(timestamp.getTime()), "Timestamp");
        }
        if(bandwidth != null) {
            dict.setStringForKey(String.valueOf(bandwidth.getRate()), "Bandwidth");
        }
        return dict.getSerialized();
    }

    /**
     * @param bytesPerSecond Maximum number of bytes to transfer by second
     */
    public void setBandwidth(final float bytesPerSecond) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Throttle bandwidth to %s bytes per second", bytesPerSecond));
        }
        bandwidth.setRate(bytesPerSecond);
    }

    public void setBandwidth(final BandwidthThrottle bandwidth) {
        this.bandwidth = bandwidth;
    }

    /**
     * @return Rate in bytes per second allowed for this transfer
     */
    public BandwidthThrottle getBandwidth() {
        return bandwidth;
    }

    /**
     * @return Time when transfer did end
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @return The first <code>root</code> added to this transfer
     */
    public TransferItem getRoot() {
        return roots.iterator().next();
    }

    public String getRemote() {
        if(this.roots.size() == 1) {
            return new DefaultUrlProvider(host).toUrl(this.getRoot().remote).find(DescriptiveUrl.Type.provider).getUrl();
        }
        else {
            return new DefaultUrlProvider(host).toUrl(this.getRoot().remote.getParent()).find(DescriptiveUrl.Type.provider).getUrl();
        }
    }

    public String getLocal() {
        final Local local = roots.iterator().next().local;
        if(roots.size() == 1) {
            return local.getAbbreviatedPath();
        }
        else {
            return local.getParent().getAbbreviatedPath();
        }
    }

    public List<TransferItem> getRoots() {
        return roots;
    }

    public Host getHost() {
        return host;
    }

    public String getName() {
        if(roots.isEmpty()) {
            return LocaleFactory.localizedString("None");
        }
        final StringBuilder name = new StringBuilder();
        name.append(roots.iterator().next().remote.getName());
        if(roots.size() > 1) {
            name.append("… (").append(roots.size()).append(")");
        }
        return name.toString();
    }

    /**
     * @param session Session
     * @param action  Transfer action for duplicate files
     * @return Null if the filter could not be determined and the transfer should be canceled instead
     */
    public abstract TransferPathFilter filter(Session<?> session, TransferAction action);

    /**
     * @param session         Session
     * @param resumeRequested Requested resume
     * @param reloadRequested Requested overwrite
     * @param prompt          Callback
     * @return Duplicate file strategy from preferences or user selection
     */
    public abstract TransferAction action(Session<?> session, boolean resumeRequested, boolean reloadRequested,
                                          TransferPrompt prompt) throws BackgroundException;

    /**
     * Returns the children of this path filtering it with the default regex filter
     *
     * @param session   Session
     * @param directory The directory to list the children
     * @param local     Local directory
     * @param listener  Listener  @return A list of child items
     */
    public abstract List<TransferItem> list(Session<?> session, Path directory, Local local,
                                            ListProgressListener listener) throws BackgroundException;

    /**
     * The actual transfer implementation
     *
     * @param session Session
     * @param file    Remote
     * @param local   Local
     * @param options Quarantine option
     * @param status  Transfer status
     * @param login   Login prompt
     */
    public abstract void transfer(Session<?> session, Path file, Local local,
                                  TransferOptions options, TransferStatus status,
                                  LoginCallback login) throws BackgroundException;

    public void start() {
        state = State.running;
        // Will be set to true in #reset when transfer action is determined
        reset = false;
    }

    public void stop() {
        state = State.stopped;
        timestamp = new Date();
    }

    /**
     * Recalculate the size of the <code>queue</code>
     */
    public synchronized void reset() {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Reset status for %s", this));
        }
        transferred = null;
        size = null;
        reset = true;
    }

    /**
     * @return True if transfer has been reset before starting.
     */
    public boolean isReset() {
        return reset;
    }

    /**
     * @return True if the bytes transferred equal the size of the queue
     */
    public synchronized boolean isComplete() {
        if(this.isRunning()) {
            return false;
        }
        if(null == size || null == transferred) {
            return false;
        }
        return this.getSize() == this.getTransferred();
    }

    /**
     * @return The sum of all file lengths in this transfer.
     */
    public long getSize() {
        if(null == size) {
            return 0L;
        }
        return size;
    }

    public void addSize(final long bytes) {
        if(null == size) {
            // Initialize
            size = 0L;
        }
        size += bytes;
    }

    /**
     * @return The number of bytes transferred of all files.
     */
    public synchronized long getTransferred() {
        if(null == transferred) {
            return 0L;
        }
        return transferred;
    }

    public synchronized void addTransferred(final long bytes) {
        if(null == transferred) {
            // Initialize
            transferred = 0L;
        }
        transferred += bytes;
    }

    public String getUuid() {
        if(null == uuid) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public synchronized void setTransferred(long transferred) {
        this.transferred = transferred;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Transfer{");
        sb.append("transferred=").append(transferred);
        sb.append(", size=").append(size);
        sb.append(", roots=").append(roots);
        sb.append(", state=").append(state);
        sb.append(", host=").append(host);
        sb.append('}');
        return sb.toString();
    }
}
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

import ch.cyberduck.core.Acl;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.Permission;
import ch.cyberduck.core.io.StreamCancelation;
import ch.cyberduck.core.io.StreamProgress;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Status class is the model of a download's status.
 * The wrapper for any status informations of a transfer as the size and transferred
 * bytes.
 *
 * @version $Id$
 */
public class TransferStatus implements StreamCancelation, StreamProgress {
    public static final long KILO = 1024; //2^10
    public static final long MEGA = 1048576; // 2^20
    public static final long GIGA = 1073741824; // 2^30
    private static final Logger log = Logger.getLogger(TransferStatus.class);
    private Rename rename
            = new Rename();
    /**
     * Target file or directory already exists
     */
    private boolean exists = false;
    /**
     * Append to file
     */
    private boolean append = false;

    /**
     * Not accepted
     */
    private boolean skipped = false;

    /**
     * The number of transfered bytes. Must be less or equals size.
     */
    private AtomicLong current
            = new AtomicLong(0);
    /**
     * Transfer size. May be less than the file size in attributes or 0 if creating symbolic links.
     */
    private long length = 0L;
    /**
     * The transfer has been canceled by the user.
     */
    private AtomicBoolean canceled
            = new AtomicBoolean();
    private AtomicBoolean complete
            = new AtomicBoolean();
    /**
     * MIME type
     */
    private String mime;
    /**
     * Current remote attributes of existing file including UNIX permissions, timestamp and ACL
     */
    private PathAttributes remote = new PathAttributes();
    /**
     * Target UNIX permissions to set when transfer is complete
     */
    private Permission permission = Permission.EMPTY;
    /**
     * Target ACL to set when transfer is complete
     */
    private Acl acl = Acl.EMPTY;
    /**
     * Target timestamp to set when transfer is complete
     */
    private Long timestamp;
    private Map<String, String> parameters
            = Collections.emptyMap();

    public boolean isComplete() {
        return complete.get();
    }

    @Override
    public void setComplete() {
        complete.set(true);
    }

    /**
     * If this path is currently transferred, interrupt it as soon as possible
     */
    public void setCanceled() {
        canceled.set(true);
    }

    /**
     * @return True if marked for interrupt
     */
    public boolean isCanceled() {
        return canceled.get();
    }

    /**
     * @return Number of bytes transferred
     */
    public long getCurrent() {
        return current.get();
    }

    /**
     * @param current The already transferred bytes
     */
    public void setCurrent(final long current) {
        this.current.set(current);
        if(log.isInfoEnabled()) {
            log.info(String.format("Transferred bytes set to %d bytes", current));
        }
    }

    @Override
    public void progress(final long transferred) {
        this.setCurrent(current.get() + transferred);
    }

    public TransferStatus current(final long transferred) {
        this.current.set(transferred);
        return this;
    }

    /**
     * @return Transfer content length
     */
    public long getLength() {
        return length;
    }

    /**
     * @param length Transfer content length
     */
    public void setLength(final long length) {
        this.length = length;
    }

    /**
     * @param length Transfer content length
     */
    public TransferStatus length(final long length) {
        this.length = length;
        return this;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(final boolean exists) {
        this.exists = exists;
    }

    public TransferStatus exists(boolean exists) {
        this.exists = exists;
        return this;
    }

    public boolean isAppend() {
        return append;
    }

    /**
     * Mark this path with an append flag when transferred
     *
     * @param append If false, the current status is cleared
     * @see #setCurrent(long)
     */
    public void setAppend(final boolean append) {
        if(!append) {
            current.set(0);
        }
        this.append = append;
    }

    public TransferStatus append(final boolean append) {
        this.append = append;
        return this;
    }

    public void setSkipped(boolean skip) {
        this.skipped = skip;
    }

    public TransferStatus skip(final boolean skip) {
        this.skipped = skip;
        return this;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public Rename getRename() {
        return rename;
    }

    public TransferStatus rename(final Path renamed) {
        this.rename.remote = renamed;
        return this;
    }

    public TransferStatus rename(final Local renamed) {
        this.rename.local = renamed;
        return this;
    }

    public boolean isRename() {
        if(this.isAppend()) {
            return false;
        }
        return rename.remote != null;
    }

    public void setRename(final Rename rename) {
        this.rename = rename;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(final String mime) {
        this.mime = mime;
    }

    public PathAttributes getRemote() {
        return remote;
    }

    public void setRemote(PathAttributes remote) {
        this.remote = remote;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public Acl getAcl() {
        return acl;
    }

    public void setAcl(Acl acl) {
        this.acl = acl;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public TransferStatus parameters(final Map<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final TransferStatus that = (TransferStatus) o;
        if(append != that.append) {
            return false;
        }
        if(exists != that.exists) {
            return false;
        }
        if(length != that.length) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = (exists ? 1 : 0);
        result = 31 * result + (append ? 1 : 0);
        result = 31 * result + (int) (length ^ (length >>> 32));
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransferStatus{");
        sb.append("exists=").append(exists);
        sb.append(", append=").append(append);
        sb.append(", current=").append(current);
        sb.append(", length=").append(length);
        sb.append(", canceled=").append(canceled);
        sb.append(", renamed=").append(rename);
        sb.append('}');
        return sb.toString();
    }

    public static final class Rename {
        /**
         * Upload target
         */
        public Path remote;
        public Local local;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Rename{");
            sb.append("local=").append(local);
            sb.append(", remote=").append(remote);
            sb.append('}');
            return sb.toString();
        }
    }
}

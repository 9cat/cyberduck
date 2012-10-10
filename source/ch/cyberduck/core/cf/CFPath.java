package ch.cyberduck.core.cf;

/*
 *  Copyright (c) 2008 David Kocher. All rights reserved.
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

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.ConnectionCanceledException;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathFactory;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Status;
import ch.cyberduck.core.StreamListener;
import ch.cyberduck.core.cdn.Distribution;
import ch.cyberduck.core.cloud.CloudPath;
import ch.cyberduck.core.http.DelayedHttpEntityCallable;
import ch.cyberduck.core.http.ResponseOutputStream;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.io.BandwidthThrottle;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.log4j.Logger;
import org.jets3t.service.utils.ServiceUtils;
import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rackspacecloud.client.cloudfiles.FilesContainerInfo;
import com.rackspacecloud.client.cloudfiles.FilesNotFoundException;
import com.rackspacecloud.client.cloudfiles.FilesObject;
import com.rackspacecloud.client.cloudfiles.FilesObjectMetaData;

/**
 * Rackspace Cloud Files Implementation
 *
 * @version $Id$
 */
public class CFPath extends CloudPath {
    private static Logger log = Logger.getLogger(CFPath.class);

    private static class Factory extends PathFactory<CFSession> {
        @Override
        protected Path create(CFSession session, String path, int type) {
            return new CFPath(session, path, type);
        }

        @Override
        protected Path create(CFSession session, String parent, String name, int type) {
            return new CFPath(session, parent, name, type);
        }

        @Override
        protected Path create(CFSession session, String parent, Local file) {
            return new CFPath(session, parent, file);
        }

        @Override
        protected <T> Path create(CFSession session, T dict) {
            return new CFPath(session, dict);
        }
    }

    public static PathFactory factory() {
        return new Factory();
    }

    private final CFSession session;

    protected CFPath(CFSession s, String parent, String name, int type) {
        super(parent, name, type);
        this.session = s;
    }

    protected CFPath(CFSession s, String path, int type) {
        super(path, type);
        this.session = s;
    }

    protected CFPath(CFSession s, String parent, Local file) {
        super(parent, file);
        this.session = s;
    }

    protected <T> CFPath(CFSession s, T dict) {
        super(dict);
        this.session = s;
    }

    @Override
    public CFSession getSession() {
        return session;
    }

    @Override
    public boolean exists() {
        if(super.exists()) {
            return true;
        }
        if(this.isContainer()) {
            try {
                return this.getSession().getClient().containerExists(this.getName());
            }
            catch(HttpException e) {
                log.warn(String.format("Container %s does not exist", this.getName()));
                return false;
            }
            catch(ConnectionCanceledException e) {
                log.warn(e.getMessage());
            }
            catch(IOException e) {
                log.warn(e.getMessage());
            }
        }
        return super.exists();
    }

    @Override
    public void readSize() {
        try {
            this.getSession().check();
            this.getSession().message(MessageFormat.format(Locale.localizedString("Getting size of {0}", "Status"),
                    this.getName()));

            if(this.isContainer()) {
                attributes().setSize(
                        this.getSession().getClient().getContainerInfo(this.getContainerName()).getTotalSize()
                );
            }
            else if(this.attributes().isFile()) {
                attributes().setSize(
                        Long.valueOf(this.getSession().getClient().getObjectMetaData(this.getContainerName(), this.getKey()).getContentLength())
                );
            }
        }
        catch(HttpException e) {
            this.error("Cannot read file attributes", e);
        }
        catch(IOException e) {
            this.error("Cannot read file attributes", e);
        }
    }

    @Override
    public void readChecksum() {
        if(this.attributes().isFile()) {
            try {
                this.getSession().check();
                this.getSession().message(MessageFormat.format(Locale.localizedString("Compute MD5 hash of {0}", "Status"),
                        this.getName()));

                final String checksum = this.getSession().getClient().getObjectMetaData(
                        this.getContainerName(), this.getKey()).getETag();
                attributes().setChecksum(checksum);
                attributes().setETag(checksum);
            }
            catch(HttpException e) {
                this.error("Cannot read file attributes", e);
            }
            catch(IOException e) {
                this.error("Cannot read file attributes", e);
            }
        }
    }

    @Override
    public void readTimestamp() {
        if(this.attributes().isFile()) {
            try {
                this.getSession().check();
                this.getSession().message(MessageFormat.format(Locale.localizedString("Getting timestamp of {0}", "Status"),
                        this.getName()));

                try {
                    attributes().setModificationDate(
                            ServiceUtils.parseRfc822Date(this.getSession().getClient().getObjectMetaData(this.getContainerName(),
                                    this.getKey()).getLastModified()).getTime()
                    );
                }
                catch(ParseException e) {
                    log.error(e);
                }
            }
            catch(HttpException e) {
                this.error("Cannot read file attributes", e);
            }
            catch(IOException e) {
                this.error("Cannot read file attributes", e);
            }
        }
    }

    @Override
    public AttributedList<Path> list(final AttributedList<Path> children) {
        if(this.attributes().isDirectory()) {
            try {
                this.getSession().check();
                this.getSession().message(MessageFormat.format(Locale.localizedString("Listing directory {0}", "Status"),
                        this.getName()));

                if(this.isRoot()) {
                    // Clear CDN cache when reloading
                    this.getSession().cdn().clear();

                    final int limit = Preferences.instance().getInteger("cf.list.limit");
                    String marker = null;
                    List<FilesContainerInfo> list;
                    // List all containers
                    do {
                        list = this.getSession().getClient().listContainersInfo(limit, marker);
                        for(FilesContainerInfo container : list) {
                            Path p = PathFactory.createPath(this.getSession(), this.getAbsolute(), container.getName(),
                                    Path.VOLUME_TYPE | Path.DIRECTORY_TYPE);
                            p.attributes().setSize(container.getTotalSize());
                            p.attributes().setOwner(this.getSession().getClient().getUserName());

                            children.add(p);

                            marker = container.getName();
                        }
                    }
                    while(list.size() == limit);
                }
                else {
                    final int limit = Preferences.instance().getInteger("cf.list.limit");
                    String marker = null;
                    List<FilesObject> list;
                    do {
                        list = this.getSession().getClient().listObjectsStartingWith(this.getContainerName(),
                                this.isContainer() ? StringUtils.EMPTY : this.getKey() + Path.DELIMITER, null, limit, marker, Path.DELIMITER);
                        for(FilesObject object : list) {
                            final Path file = PathFactory.createPath(this.getSession(), this.getContainerName(), object.getName(),
                                    "application/directory".equals(object.getMimeType()) ? Path.DIRECTORY_TYPE : Path.FILE_TYPE);
                            file.setParent(this);
                            if(file.attributes().isFile()) {
                                file.attributes().setSize(object.getSize());
                                file.attributes().setChecksum(object.getMd5sum());
                                try {
                                    final Date modified = DateParser.parse(object.getLastModified());
                                    if(null != modified) {
                                        file.attributes().setModificationDate(modified.getTime());
                                    }
                                }
                                catch(InvalidDateException e) {
                                    log.warn("Not ISO 8601 format:" + e.getMessage());
                                }
                            }
                            if(file.attributes().isDirectory()) {
                                file.attributes().setPlaceholder(true);
                                if(children.contains(file.getReference())) {
                                    continue;
                                }
                            }
                            file.attributes().setOwner(this.attributes().getOwner());

                            children.add(file);

                            marker = object.getName();
                        }
                        if(Preferences.instance().getBoolean("cf.list.cdn.preload")) {
                            for(Distribution.Method method : this.getSession().cdn().getMethods(this.getContainerName())) {
                                // Cache CDN configuration
                                this.getSession().cdn().read(this.getSession().cdn().getOrigin(method, this.getContainerName()), method);
                            }
                        }
                    }
                    while(list.size() == limit);
                }
            }
            catch(HttpException e) {
                log.warn("Listing directory failed:" + e.getMessage());
                children.attributes().setReadable(false);
                if(session.cache().containsKey(this.getReference())) {
                    this.error(e.getMessage(), e);
                }
            }
            catch(IOException e) {
                log.warn("Listing directory failed:" + e.getMessage());
                children.attributes().setReadable(false);
                if(session.cache().containsKey(this.getReference())) {
                    this.error(e.getMessage(), e);
                }
            }
        }
        return children;
    }

    @Override
    public InputStream read(boolean check) throws IOException {
        if(check) {
            this.getSession().check();
        }
        try {
            if(this.status().isResume()) {
                return this.getSession().getClient().getObjectAsRangedStream(this.getContainerName(), this.getKey(),
                        this.status().getCurrent(), this.status().getLength());
            }
            return this.getSession().getClient().getObjectAsStream(this.getContainerName(), this.getKey());
        }
        catch(HttpException e) {
            IOException failure = new IOException(e.getMessage());
            failure.initCause(e);
            throw failure;
        }
    }

    @Override
    protected void download(final BandwidthThrottle throttle, final StreamListener listener,
                            final boolean check, final boolean quarantine) {
        if(attributes().isFile()) {
            OutputStream out = null;
            InputStream in = null;
            try {
                in = this.read(check);
                out = this.getLocal().getOutputStream(this.status().isResume());
                this.download(in, out, throttle, listener, quarantine);
            }
            catch(IOException e) {
                this.error("Download failed", e);
            }
            finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
        }
    }

    @Override
    protected void upload(final BandwidthThrottle throttle, final StreamListener listener, boolean check) {
        if(attributes().isFile()) {
            try {
                String md5sum = null;
                if(Preferences.instance().getBoolean("cf.upload.metadata.md5")) {
                    this.getSession().message(MessageFormat.format(Locale.localizedString("Compute MD5 hash of {0}", "Status"),
                            this.getName()));
                    md5sum = this.getLocal().attributes().getChecksum();
                }
                MessageDigest digest = null;
                if(!Preferences.instance().getBoolean("cf.upload.metadata.md5")) {
                    try {
                        digest = MessageDigest.getInstance("MD5");
                    }
                    catch(NoSuchAlgorithmException e) {
                        log.error("MD5 calculation disabled:" + e.getMessage());
                    }
                }
                InputStream in = null;
                ResponseOutputStream<String> out = null;
                try {
                    if(null == digest) {
                        log.warn("MD5 calculation disabled");
                        in = this.getLocal().getInputStream();
                    }
                    else {
                        in = new DigestInputStream(this.getLocal().getInputStream(), digest);
                    }
                    out = this.write(check, md5sum);
                    this.upload(out, in, throttle, listener);
                }
                finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                }
                if(null != digest && null != out) {
                    this.getSession().message(MessageFormat.format(
                            Locale.localizedString("Compute MD5 hash of {0}", "Status"), this.getName()));
                    // Obtain locally-calculated MD5 hash.
                    String expectedETag = ServiceUtils.toHex(digest.digest());
                    // Compare our locally-calculated hash with the ETag returned.
                    final String result = out.getResponse();
                    if(!expectedETag.equals(result)) {
                        throw new IOException("Mismatch between MD5 hash of uploaded data ("
                                + expectedETag + ") and ETag returned ("
                                + result + ") for object key: "
                                + this.getKey());
                    }
                    else {
                        if(log.isDebugEnabled()) {
                            log.debug("Object upload was automatically verified, the calculated MD5 hash " +
                                    "value matched the ETag returned: " + this.getKey());
                        }
                    }
                }
            }
            catch(IOException e) {
                this.error("Upload failed", e);
            }
        }
    }

    @Override
    public OutputStream write(boolean check) throws IOException {
        return this.write(check, null);
    }

    private ResponseOutputStream<String> write(boolean check, final String md5sum) throws IOException {
        if(check) {
            this.getSession().check();
        }
        // No Content-Range support
        final Status status = this.status();
        status.setResume(false);
        final HashMap<String, String> metadata = new HashMap<String, String>();
        // Default metadata for new files
        for(String m : Preferences.instance().getList("cf.metadata.default")) {
            if(StringUtils.isBlank(m)) {
                log.warn(String.format("Invalid header %s", m));
                continue;
            }
            if(!m.contains("=")) {
                log.warn(String.format("Invalid header %s", m));
                continue;
            }
            int split = m.indexOf('=');
            String name = m.substring(0, split);
            if(StringUtils.isBlank(name)) {
                log.warn(String.format("Missing key in %s", m));
                continue;
            }
            String value = m.substring(split + 1);
            if(StringUtils.isEmpty(value)) {
                log.warn(String.format("Missing value in %s", m));
                continue;
            }
            metadata.put(name, value);
        }

        // Submit store call to background thread
        final DelayedHttpEntityCallable<String> command = new DelayedHttpEntityCallable<String>() {
            /**
             *
             * @return The ETag returned by the server for the uploaded object
             */
            @Override
            public String call(AbstractHttpEntity entity) throws IOException {
                try {
                    return CFPath.this.getSession().getClient().storeObjectAs(CFPath.this.getContainerName(),
                            CFPath.this.getKey(), entity,
                            metadata, md5sum);
                }
                catch(HttpException e) {
                    IOException failure = new IOException(e.getMessage());
                    failure.initCause(e);
                    throw failure;
                }
            }

            @Override
            public long getContentLength() {
                return status().getLength() - status().getCurrent();
            }
        };
        return this.write(command);
    }

    @Override
    public void mkdir() {
        if(this.attributes().isDirectory()) {
            try {
                this.getSession().check();
                this.getSession().message(MessageFormat.format(Locale.localizedString("Making directory {0}", "Status"),
                        this.getName()));

                if(this.isContainer()) {
                    // Create container at top level
                    this.getSession().getClient().createContainer(this.getName());
                }
                else {
                    // Create virtual directory
                    this.getSession().getClient().createFullPath(this.getContainerName(), this.getKey());
                }
            }
            catch(HttpException e) {
                this.error("Cannot create folder {0}", e);
            }
            catch(IOException e) {
                this.error("Cannot create folder {0}", e);
            }
        }
    }

    @Override
    public void delete() {
        try {
            this.getSession().check();
            final String container = this.getContainerName();
            if(attributes().isFile()) {
                this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                        this.getName()));

                this.getSession().getClient().deleteObject(container, this.getKey());
            }
            else if(attributes().isDirectory()) {
                for(AbstractPath i : this.children()) {
                    if(!this.getSession().isConnected()) {
                        break;
                    }
                    i.delete();
                }
                this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                        this.getName()));
                if(this.isContainer()) {
                    this.getSession().getClient().deleteContainer(container);
                }
                else {
                    try {
                        this.getSession().getClient().deleteObject(container, this.getKey());
                    }
                    catch(FilesNotFoundException e) {
                        // No real placeholder but just a delmiter returned in the object listing.
                        log.warn(e.getMessage());
                    }
                }
            }
        }
        catch(HttpException e) {
            this.error("Cannot delete {0}", e);
        }
        catch(IOException e) {
            this.error("Cannot delete {0}", e);
        }
    }

    @Override
    public void readMetadata() {
        if(this.attributes().isFile()) {
            try {
                this.getSession().check();
                this.getSession().message(MessageFormat.format(Locale.localizedString("Reading metadata of {0}", "Status"),
                        this.getName()));

                final FilesObjectMetaData meta
                        = this.getSession().getClient().getObjectMetaData(this.getContainerName(), this.getKey());
                this.attributes().setMetadata(meta.getMetaData());
            }
            catch(HttpException e) {
                this.error("Cannot read file attributes", e);
            }
            catch(IOException e) {
                this.error("Cannot read file attributes", e);
            }
        }
    }

    @Override
    public void writeMetadata(Map<String, String> meta) {
        if(this.attributes().isFile()) {
            try {
                this.getSession().check();
                this.getSession().message(MessageFormat.format(Locale.localizedString("Writing metadata of {0}", "Status"),
                        this.getName()));

                this.getSession().getClient().updateObjectMetadata(this.getContainerName(), this.getKey(), meta);
            }
            catch(HttpException e) {
                this.error("Cannot write file attributes", e);
            }
            catch(IOException e) {
                this.error("Cannot write file attributes", e);
            }
            finally {
                this.attributes().clear(false, false, false, true);
            }
        }
    }

    @Override
    public void rename(AbstractPath renamed) {
        try {
            this._copy(renamed);
            this.delete();
        }
        catch(HttpException e) {
            this.error("Cannot rename {0}", e);
        }
        catch(IOException e) {
            this.error("Cannot rename {0}", e);
        }
    }

    @Override
    public void copy(AbstractPath copy, BandwidthThrottle throttle, StreamListener listener) {
        if(((Path) copy).getSession().equals(this.getSession())) {
            // Copy on same server
            try {
                this._copy(copy);
            }
            catch(HttpException e) {
                this.error("Cannot copy {0}", e);
            }
            catch(IOException e) {
                this.error("Cannot copy {0}", e);
            }
        }
        else {
            // Copy to different host
            super.copy(copy, throttle, listener);
        }
    }

    private void _copy(AbstractPath copy) throws IOException, HttpException {
        this.getSession().check();
        this.getSession().message(MessageFormat.format(Locale.localizedString("Copying {0} to {1}", "Status"),
                this.getName(), copy));

        if(attributes().isVolume()) {
            throw new IOException("Renaming containers is not supported");
        }
        else if(this.attributes().isFile()) {
            String destination = ((CFPath) copy).getKey();
            this.getSession().getClient().copyObject(this.getContainerName(), this.getKey(),
                    ((CFPath) copy).getContainerName(), destination);
        }
        else if(attributes().isDirectory()) {
            for(Path i : this.children()) {
                if(!this.getSession().isConnected()) {
                    break;
                }
                i.copy(PathFactory.createPath(this.getSession(), copy.getAbsolute(),
                        i.getName(), i.attributes().getType()));
            }
        }
        this.status().setComplete(true);
    }

    /**
     * @return Publicy accessible URL of given object
     */
    @Override
    public String toHttpURL() {
        CFSession session = this.getSession();
        for(Distribution.Method method : session.cdn().getMethods(this.getContainerName())) {
            if(session.cdn().isCached(method)) {
                final Distribution distribution = session.cdn().read(session.cdn().getOrigin(method, this.getContainerName()), method);
                return distribution.getURL(this);
            }
        }
        // Storage URL is not accessible
        return null;
    }
}

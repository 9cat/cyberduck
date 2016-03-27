package ch.cyberduck.core.dropbox;

/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
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
 */

import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Attributes;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.shared.DefaultAttributesFeature;
import ch.cyberduck.core.shared.DefaultFindFeature;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.UploadUploader;

public class DropboxWriteFeature implements Write {

    private static final Logger log = Logger.getLogger(DropboxWriteFeature.class);

    private DropboxSession session;

    private Find finder;

    private Attributes attributes;


    public DropboxWriteFeature(final DropboxSession session) {
        this(session, new DefaultFindFeature(session), new DefaultAttributesFeature(session));
    }

    public DropboxWriteFeature(final DropboxSession session, final Find finder, final Attributes attributes) {
        this.session = session;
        this.finder = finder;
        this.attributes = attributes;
    }

    @Override
    public Append append(final Path file, final Long length, final PathCache cache) throws BackgroundException {
        if(finder.withCache(cache).find(file)) {
            final PathAttributes attributes = this.attributes.withCache(cache).find(file);
            return new Append(false, true).withSize(attributes.getSize()).withChecksum(attributes.getChecksum());
        }
        return Write.notfound;
    }

    @Override
    public OutputStream write(Path file, TransferStatus status) throws BackgroundException {
        try {
            UploadUploader uploader = session.getClient().files().upload(file.getAbsolute());
            return new UploadProxyOutputStream(uploader);
        }
        catch(DbxException ex) {
            throw new BackgroundException("Upload failed.", ex);
        }
    }

    @Override
    public boolean temporary() {
        return false;
    }

    @Override
    public boolean random() {
        return false;
    }

    private static final class UploadProxyOutputStream extends ProxyOutputStream {

        private final UploadUploader uploader;

        public UploadProxyOutputStream(final UploadUploader uploader) {
            super(uploader.getOutputStream());
            this.uploader = uploader;
        }

        @Override
        public void close() throws IOException {
            try {
                uploader.finish();
                uploader.close();
            }
            catch(DbxException ex) {
                throw new IOException("Upload failed.", ex);
            }
            finally {
                super.close();
            }
        }
    }
}

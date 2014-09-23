package ch.cyberduck.core.io;

/*
 * Copyright (c) 2002-2013 David Kocher. All rights reserved.
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
 * Bug fixes, suggestions and comments should be sent to feedback@cyberduck.ch
 */

import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.exception.ConnectionCanceledException;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @version $Id$
 */
public final class StreamCopier {
    private static final Logger log = Logger.getLogger(StreamCopier.class);

    private StreamCancelation cancel;

    private StreamProgress progress;

    private StreamListener listener
            = new DisabledStreamListener();

    private Integer buffer
            = Preferences.instance().getInteger("connection.buffer");

    /**
     * Buffer size
     */
    private Integer chunksize
            = Preferences.instance().getInteger("connection.chunksize");

    private Long offset = 0L;

    private Long limit = -1L;

    private boolean keepFlushing;

    public StreamCopier(final StreamCancelation cancel, final StreamProgress progress) {
        this.cancel = cancel;
        this.progress = progress;
    }

    public StreamCopier withBuffer(final Integer buffer) {
        this.buffer = buffer;
        return this;
    }

    public StreamCopier withFlushing(boolean keepFlushing) {
        this.keepFlushing = keepFlushing;
        return this;
    }

    public StreamCopier withChunksize(final Integer chunksize) {
        this.chunksize = chunksize;
        return this;
    }

    public StreamCopier withListener(final StreamListener listener) {
        this.listener = listener;
        return this;
    }

    public StreamCopier withLimit(final Long limit) {
        if(limit > 0) {
            this.limit = limit;
        }
        return this;
    }

    public StreamCopier withOffset(final Long offset) {
        if(offset > 0) {
            this.offset = offset;
        }
        return this;
    }

    /**
     * Updates the current number of bytes transferred in the status reference.
     *
     * @param in  The stream to read from
     * @param out The stream to write to
     */
    public void transfer(final InputStream in, final OutputStream out) throws IOException, ConnectionCanceledException {
        this.transfer(new BufferedInputStream(in, buffer), new BufferedOutputStream(out, buffer));
    }

    private void transfer(final BufferedInputStream in, final BufferedOutputStream out) throws IOException, ConnectionCanceledException {
        if(offset > 0) {
            skip(in, offset);
        }
        try {
            final byte[] buffer = new byte[chunksize];
            long total = 0;
            int len = chunksize;
            if(limit > 0 && limit < chunksize) {
                // Cast will work because chunk size is int
                len = limit.intValue();
            }
            while(len > 0 && !cancel.isCanceled()) {
                final int read = in.read(buffer, 0, len);
                if(-1 == read) {
                    if(log.isDebugEnabled()) {
                        log.debug("End of file reached");
                    }
                    progress.setComplete();
                    break;
                }
                else {
                    listener.recv(read);
                    out.write(buffer, 0, read);
                    if(keepFlushing) {
                        out.flush();
                    }
                    progress.progress(read);
                    listener.sent(read);
                    total += read;
                }
                if(limit > 0) {
                    // Only adjust if not reading to the end of the stream. Cast will work because chunk size is int
                    len = (int) Math.min(limit - total, chunksize);
                }
                if(limit == total) {
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Limit %d reached reading from stream", limit));
                    }
                    progress.setComplete();
                }
            }
        }
        finally {
            if(!keepFlushing) {
                out.flush();
            }
        }
        if(cancel.isCanceled()) {
            throw new ConnectionCanceledException();
        }
    }

    public static void skip(final InputStream bi, final long offset) throws IOException {
        long skipped = bi.skip(offset);
        if(log.isInfoEnabled()) {
            log.info(String.format("Skipping %d bytes", skipped));
        }
        if(skipped < offset) {
            throw new IOResumeException(String.format("Skipped %d bytes instead of %d",
                    skipped, offset));
        }
    }
}

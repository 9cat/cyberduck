package ch.cyberduck.core.threading;

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
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.ch
 */

import ch.cyberduck.core.*;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.ui.growl.Growl;
import ch.cyberduck.ui.growl.GrowlFactory;

import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public abstract class SessionBackgroundAction<T> extends AbstractBackgroundAction<T>
        implements ProgressListener, TranscriptListener {
    private static final Logger log = Logger.getLogger(SessionBackgroundAction.class);

    /**
     * Contains all exceptions thrown while this action was running
     */
    private BackgroundException exception;

    /**
     * This action encountered one or more exceptions
     */
    private boolean failed;

    /**
     * Contains the transcript of the session while this action was running
     */
    private StringBuilder transcript
            = new StringBuilder();

    /**
     * The number of times this action has been run
     */
    protected int repeatCount;

    private static final String LINE_SEPARATOR
            = System.getProperty("line.separator");

    private AlertCallback alert;

    private ProgressListener progressListener;

    private TranscriptListener transcriptListener;

    private ConnectionService connection;

    private final NetworkFailureDiagnostics diagnostics
            = new NetworkFailureDiagnostics();

    private Growl growl = GrowlFactory.get();

    private Session<?> session;

    private Cache cache;

    public SessionBackgroundAction(final Session<?> session,
                                   final Cache cache,
                                   final AlertCallback alert,
                                   final ProgressListener progressListener,
                                   final TranscriptListener transcriptListener,
                                   final LoginCallback prompt,
                                   final HostKeyCallback key) {
        this.session = session;
        this.cache = cache;
        this.alert = alert;
        this.progressListener = progressListener;
        this.transcriptListener = transcriptListener;
        this.connection = new LoginConnectionService(prompt, key,
                PasswordStoreFactory.get(), progressListener);
    }

    public BackgroundException getException() {
        return exception;
    }

    @Override
    public void message(final String message) {
        progressListener.message(message);
    }

    /**
     * Append to the transcript and notify listeners.
     */
    @Override
    public void log(final boolean request, final String message) {
        transcript.append(message).append(LINE_SEPARATOR);
        transcriptListener.log(request, message);
    }

    @Override
    public void prepare() throws ConnectionCanceledException {
        super.prepare();
        this.message(this.getActivity());
        session.addProgressListener(this);
        session.addTranscriptListener(this);
    }

    @Override
    public void cancel() {
        connection.cancel();
        super.cancel();
    }

    /**
     * The number of times a new connection attempt should be made. Takes into
     * account the number of times already tried.
     *
     * @return Greater than zero if a failed action should be repeated again
     */
    protected int retry() {
        if(this.hasFailed() && !this.isCanceled()) {
            // Check for an exception we consider possibly temporary
            if(diagnostics.isNetworkFailure(exception)) {
                // The initial connection attempt does not count
                return Preferences.instance().getInteger("connection.retry") - repeatCount;
            }
        }
        return 0;
    }

    protected void reset() {
        // Clear the transcript and exceptions
        transcript = new StringBuilder();
        failed = false;
        exception = null;
    }

    /**
     * @return True if the the action had a permanent failures. Returns false if
     *         there were only temporary exceptions and the action succeeded upon retry
     */
    protected boolean hasFailed() {
        return failed;
    }

    @Override
    public T call() {
        // Reset status
        this.reset();
        try {
            // Open connection
            this.connect(session);
            // Run action
            return super.call();
        }
        catch(ConnectionCanceledException failure) {
            // Do not report as failed if instanceof ConnectionCanceledException
            log.warn(String.format("Connection canceled %s", failure.getMessage()));
        }
        catch(BackgroundException failure) {
            log.warn(String.format("Failure executing background action: %s", failure));
            growl.notify(failure.getMessage(), session.getHost().getHostname());
            exception = failure;
            failed = true;
        }
        return null;
    }

    protected boolean connect(final Session session) throws BackgroundException {
        if(connection.check(session, cache)) {
            // New connection opened
            growl.notify("Connection opened", session.getHost().getHostname());
            // Reset cache
            cache.clear();
            return true;
        }
        // Use existing connection
        return false;
    }

    protected void close(final Session session) throws BackgroundException {
        session.close();
    }

    @Override
    public void finish() {
        while(this.retry() > 0) {
            if(log.isInfoEnabled()) {
                log.info(String.format("Retry failed background action %s", this));
            }
            // This is an automated retry. Wait some time first.
            this.pause();
            if(!this.isCanceled()) {
                repeatCount++;
                // Reset the failure status but remember the previous exception for automatic retry.
                failed = false;
                // Re-run the action with the previous lock used
                this.call();
            }
        }
        session.removeProgressListener(this);
        // It is important _not_ to do this in #cleanup as otherwise
        // the listeners are still registered when the next BackgroundAction
        // is already running
        session.removeTranscriptListener(this);
        super.finish();
    }

    @Override
    public boolean alert() {
        if(this.hasFailed() && !this.isCanceled()) {
            // Display alert if the action was not canceled intentionally
            return alert.alert(session.getHost(), exception, transcript);
        }
        return false;
    }

    @Override
    public void cleanup() {
        this.message(null);
    }

    /**
     * Idle this action for some time. Blocks the caller.
     */
    public void pause() {
        if(0 == Preferences.instance().getInteger("connection.retry.delay")) {
            log.info("No pause between retry");
            return;
        }
        final BackgroundActionPauser pauser = new BackgroundActionPauser(this);
        pauser.await(this);
    }

    public Session<?> getSession() {
        return session;
    }

    @Override
    public String getName() {
        return BookmarkNameProvider.toString(session.getHost());
    }

    /**
     * @return The session instance
     */
    @Override
    public Object lock() {
        return session;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SessionBackgroundAction{");
        sb.append("session=").append(session);
        sb.append(", failed=").append(failed);
        sb.append(", exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}

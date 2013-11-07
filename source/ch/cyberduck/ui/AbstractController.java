package ch.cyberduck.ui;

/*
 *  Copyright (c) 2009 David Kocher. All rights reserved.
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

import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.threading.BackgroundAction;
import ch.cyberduck.core.threading.BackgroundActionRegistry;
import ch.cyberduck.core.threading.MainAction;
import ch.cyberduck.core.threading.SessionBackgroundAction;
import ch.cyberduck.core.threading.ThreadPool;
import ch.cyberduck.ui.threading.ControllerMainAction;

import org.apache.log4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * @version $Id$
 */
public abstract class AbstractController implements Controller {
    private static Logger log = Logger.getLogger(AbstractController.class);

    private ThreadPool singleExecutor;

    private ThreadPool concurrentExecutor;

    protected AbstractController() {
        singleExecutor = new ThreadPool();
        concurrentExecutor = new ThreadPool(Integer.MAX_VALUE);
    }

    protected AbstractController(final Thread.UncaughtExceptionHandler handler) {
        singleExecutor = new ThreadPool(handler);
        concurrentExecutor = new ThreadPool(Integer.MAX_VALUE, handler);
    }

    /**
     * Does wait for main action to return before continuing the caller thread.
     *
     * @param runnable The action to execute
     */
    @Override
    public void invoke(final MainAction runnable) {
        this.invoke(runnable, false);
    }

    /**
     * List of pending background tasks or this browser
     */
    private BackgroundActionRegistry registry
            = new BackgroundActionRegistry();

    /**
     * Pending background actions
     *
     * @return List of tasks.
     */
    public BackgroundActionRegistry getActions() {
        return registry;
    }

    /**
     * @return true if there is any network activity running in the background
     */
    public boolean isActivityRunning() {
        final BackgroundAction current = this.getActions().getCurrent();
        return null != current;
    }

    /**
     * Will queue up the <code>BackgroundAction</code> to be run in a background thread. Will be executed
     * as soon as no other previous <code>BackgroundAction</code> is pending.
     * Will return immediatly but not run the runnable before the lock of the runnable is acquired.
     *
     * @param action The runnable to execute in a secondary Thread
     * @see java.lang.Thread
     * @see ch.cyberduck.core.threading.BackgroundAction#lock()
     */
    @Override
    public <T> Future<T> background(final BackgroundAction<T> action) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Run action %s in background", action));
        }
        if(registry.contains(action)) {
            log.warn(String.format("Skip duplicate background action %s found in registry", action));
            return null;
        }
        registry.add(action);
        action.init();
        // Start background task
        final Callable<T> command = new BackgroundCallable<T>(action);
        try {
            if(null == action.lock()) {
                return concurrentExecutor.execute(command);
            }
            else {
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Synchronize on lock %s for action %s", action.lock(), action));
                }
                return singleExecutor.execute(command);
            }
        }
        catch(RejectedExecutionException e) {
            log.error(String.format("Error scheduling background task %s for execution. %s", action, e.getMessage()));
            action.cleanup();
        }
        if(log.isInfoEnabled()) {
            log.info(String.format("Scheduled background runnable %s for execution", action));
        }
        return null;
    }

    protected void invalidate() {
        singleExecutor.shutdown();
        concurrentExecutor.shutdown();
    }

    private final class BackgroundCallable<T> implements Callable<T> {
        private final BackgroundAction<T> action;


        public BackgroundCallable(final BackgroundAction<T> action) {
            this.action = action;
        }

        @Override
        public T call() {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Acquired lock for background runnable %s", action));
            }
            if(action.isCanceled()) {
                // Canceled action yields no result
                return null;
            }
            try {
                action.prepare();
                // Execute the action of the runnable
                return action.call();
            }
            catch(ConnectionCanceledException e) {
                log.warn(String.format("Connection canceled for background task %s", action));
            }
            catch(BackgroundException e) {
                log.error(String.format("Unhandled exception running background task %s", e.getMessage()), e);
            }
            catch(Exception e) {
                log.fatal(String.format("Unhandled exception running background task %s", e.getMessage()), e);
            }
            finally {
                // Remove from registry before retry
                registry.remove(action);
                action.finish();
                // Invoke the cleanup on the main thread to let the action synchronize the user interface
                invoke(new ControllerMainAction(AbstractController.this) {
                    @Override
                    public void run() {
                        try {
                            action.cleanup();
                        }
                        catch(Exception e) {
                            log.error(String.format("Exception running cleanup task %s", e.getMessage()), e);
                        }
                    }
                });
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Releasing lock for background runnable %s", action));
                }
            }
            // Canceled action yields no result
            return null;
        }
    }

    @Override
    public void start(final BackgroundAction action) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Start action %s", action));
        }
    }

    @Override
    public void cancel(final BackgroundAction action) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Cancel action %s", action));
        }
    }

    @Override
    public void stop(final BackgroundAction action) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Stop action %s", action));
        }
    }

    @Override
    public void message(final String message) {
        log.info(message);
    }

    @Override
    public void log(final boolean request, final String message) {
        log.info(message);
    }

    @Override
    public void alert(final SessionBackgroundAction<?> action, final BackgroundException failure, final StringBuilder transcript) {
        log.warn(failure.getMessage(), failure);
    }
}

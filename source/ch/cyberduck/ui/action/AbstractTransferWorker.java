package ch.cyberduck.ui.action;

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

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.SleepPreventer;
import ch.cyberduck.core.SleepPreventerFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferAction;
import ch.cyberduck.core.transfer.TransferErrorCallback;
import ch.cyberduck.core.transfer.TransferOptions;
import ch.cyberduck.core.transfer.TransferPathFilter;
import ch.cyberduck.core.transfer.TransferPrompt;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.ui.growl.Growl;
import ch.cyberduck.ui.growl.GrowlFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @version $Id$
 */
public abstract class AbstractTransferWorker extends Worker<Boolean> {
    private static final Logger log = Logger.getLogger(AbstractTransferWorker.class);

    private SleepPreventer sleep = SleepPreventerFactory.get();

    private Growl growl = GrowlFactory.get();

    private Transfer transfer;

    /**
     * Overwrite prompt
     */
    private TransferPrompt prompt;

    /**
     * Error prompt
     */
    private TransferErrorCallback error;

    private TransferOptions options;

    /**
     * Transfer status determined by filters
     */
    private Map<Path, TransferStatus> table
            = new HashMap<Path, TransferStatus>();

    /**
     * Workload
     */
    private Cache<Path> cache = new Cache<Path>(Integer.MAX_VALUE);

    public AbstractTransferWorker(final Transfer transfer, final TransferOptions options,
                                  final TransferPrompt prompt, final TransferErrorCallback error) {
        this.transfer = transfer;
        this.prompt = prompt;
        this.error = error;
        this.options = options;
    }

    public AbstractTransferWorker(final Transfer transfer, final TransferOptions options,
                                  final TransferPrompt prompt, final TransferErrorCallback error,
                                  final Cache cache) {
        this.transfer = transfer;
        this.options = options;
        this.prompt = prompt;
        this.error = error;
        this.cache = cache;
    }

    public AbstractTransferWorker(final Transfer transfer, final TransferOptions options,
                                  final TransferPrompt prompt, final TransferErrorCallback error,
                                  final Map<Path, TransferStatus> table) {
        this.transfer = transfer;
        this.options = options;
        this.prompt = prompt;
        this.error = error;
        this.table = table;
    }

    protected abstract Session<?> borrow() throws BackgroundException;

    protected abstract void release(Session session) throws BackgroundException;

    @Override
    public void cancel() {
        for(TransferStatus status : table.values()) {
            status.setCanceled();
        }
        super.cancel();
    }

    protected void complete() throws BackgroundException {
        // No need to implement for single threaded transfer
    }

    @Override
    public Boolean initialize() {
        return false;
    }

    @Override
    public Boolean run() throws BackgroundException {
        final String lock = sleep.lock();
        try {
            transfer.start();
            if(log.isDebugEnabled()) {
                log.debug(String.format("Start transfer with prompt %s and options %s", prompt, options));
            }
            final Session<?> session = this.borrow();
            try {
                // Determine the filter to match files against
                final TransferAction action = transfer.action(session, options.resumeRequested, options.reloadRequested, prompt);
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Selected transfer action %s", action));
                }
                if(action.equals(TransferAction.cancel)) {
                    if(log.isInfoEnabled()) {
                        log.info(String.format("Transfer %s canceled by user", this));
                    }
                    throw new ConnectionCanceledException();
                }
                // Determine transfer filter implementation from selected overwrite action
                final TransferPathFilter filter = transfer.filter(session, action);
                // Reset the cached size of the transfer and progress value
                transfer.reset();
                // Calculate information about the files in advance to give progress information
                for(Path next : transfer.getRoots()) {
                    this.prepare(next, new TransferStatus().exists(true), filter);
                }
                this.complete();
                // Transfer all files sequentially
                for(Path next : transfer.getRoots()) {
                    this.transfer(next, filter);
                }
                this.complete();
            }
            finally {
                this.release(session);
            }
        }
        finally {
            transfer.stop();
            if(transfer.isReset()) {
                growl.notify(transfer.isComplete() ?
                        String.format("%s complete", StringUtils.capitalize(transfer.getType().name())) : "Transfer incomplete", transfer.getName());
            }
            sleep.release(lock);
        }
        return true;
    }

    /**
     * To be called before any file is actually transferred
     *
     * @param file   File
     * @param filter Filter to apply to exclude files from transfer
     */
    public void prepare(final Path file, final TransferStatus parent, final TransferPathFilter filter) throws BackgroundException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Find transfer status of %s for transfer %s", file, this));
        }
        if(this.isCanceled()) {
            throw new ConnectionCanceledException();
        }
        if(prompt.isSelected(file)) {
            // Only prepare the path it will be actually transferred
            if(filter.accept(file, parent)) {
                if(log.isInfoEnabled()) {
                    log.info(String.format("Accepted file %s in transfer %s", file, this));
                }
                this.submit(new TransferCallable() {
                    @Override
                    public TransferStatus call() throws BackgroundException {
                        // Transfer
                        final Session<?> session = borrow();
                        session.message(MessageFormat.format(LocaleFactory.localizedString("Prepare {0}", "Status"), file.getName()));
                        try {
                            // Determine transfer status
                            final TransferStatus status = filter.prepare(file, parent);
                            table.put(file, status);
                            // Apply filter
                            filter.apply(file, status);
                            // Add transfer length to total bytes
                            transfer.addSize(status.getLength());
                            // Add skipped bytes
                            transfer.addTransferred(status.getCurrent());
                            // Recursive
                            if(file.attributes().isDirectory()) {
                                // Call recursively for all children
                                final AttributedList<Path> children = transfer.list(session, file, new ActionListProgressListener(AbstractTransferWorker.this));
                                // Put into cache for later reference when transferring
                                cache.put(file.getReference(), children);
                                // Call recursively
                                for(Path f : children) {
                                    prepare(f, status, filter);
                                }
                            }
                            if(log.isInfoEnabled()) {
                                log.info(String.format("Determined transfer status %s of %s for transfer %s", status, file, this));
                            }
                            return status;
                        }
                        catch(ConnectionCanceledException e) {
                            throw e;
                        }
                        catch(BackgroundException e) {
                            // Prompt to continue or abort
                            if(error.prompt(e)) {
                                // Continue
                                log.warn(String.format("Ignore transfer failure %s", e));
                                return null;
                            }
                            else {
                                throw new ConnectionCanceledException(e);
                            }
                        }
                        finally {
                            release(session);
                        }
                    }
                });
            }
            else {
                if(log.isInfoEnabled()) {
                    log.info(String.format("Skip file %s by filter %s for transfer %s", file, filter, this));
                }
            }
        }
        else {
            log.info(String.format("Skip unchecked file %s for transfer %s", file, this));
        }
    }

    /**
     * @param file   File
     * @param filter Filter to apply to exclude files from transfer
     */
    public void transfer(final Path file, final TransferPathFilter filter) throws BackgroundException {
        if(this.isCanceled()) {
            throw new ConnectionCanceledException();
        }
        // Only transfer if accepted by filter and stored in table with transfer status
        if(table.containsKey(file)) {
            final TransferStatus status = table.get(file);
            this.submit(new TransferCallable() {
                @Override
                public TransferStatus call() throws BackgroundException {
                    // Transfer
                    final Session<?> session = borrow();
                    try {
                        boolean failure = false;
                        try {
                            if(status.isRename()) {
                                // Save with different name
                                transfer.transfer(session, status.getRenamed(), options, status);
                            }
                            else {
                                transfer.transfer(session, file, options, status);
                            }
                        }
                        catch(ConnectionCanceledException e) {
                            throw e;
                        }
                        catch(BackgroundException e) {
                            // Prompt to continue or abort
                            if(error.prompt(e)) {
                                failure = true;
                                // Continue
                                log.warn(String.format("Ignore transfer failure %s", e));
                            }
                            else {
                                throw new ConnectionCanceledException(e);
                            }
                        }
                        // Recursive
                        if(file.attributes().isDirectory()) {
                            for(Path f : cache.get(file.getReference())) {
                                // Recursive
                                transfer(f, filter);
                            }
                            cache.remove(file.getReference());
                        }
                        if(!failure) {
                            // Post process of file.
                            try {
                                filter.complete(file, options, status, session);
                            }
                            catch(BackgroundException e) {
                                log.warn(String.format("Ignore failure in completion filter for %s", file));
                            }
                        }
                        return table.remove(file);
                    }
                    finally {
                        release(session);
                    }
                }
            });
        }
        else {
            log.warn(String.format("Skip file %s with unknown transfer status", file));
        }
    }

    protected abstract void submit(TransferCallable callable) throws BackgroundException;

    protected interface TransferCallable extends Callable<TransferStatus> {
        TransferStatus call() throws BackgroundException;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AbstractTransferWorker{");
        sb.append("transfer=").append(transfer);
        sb.append('}');
        return sb.toString();
    }
}

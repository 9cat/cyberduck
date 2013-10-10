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

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferErrorCallback;
import ch.cyberduck.core.transfer.TransferOptions;
import ch.cyberduck.core.transfer.TransferPrompt;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.log4j.Logger;

import java.util.Map;

/**
 * @version $Id$
 */
public class SingleTransferWorker extends AbstractTransferWorker {
    private static final Logger log = Logger.getLogger(SingleTransferWorker.class);

    private Session session;

    public SingleTransferWorker(final Session session, final Transfer transfer, final TransferOptions options,
                                final TransferPrompt prompt, final TransferErrorCallback error) {
        super(transfer, options, prompt, error);
        this.session = session;
    }

    public SingleTransferWorker(final Session session, final Transfer transfer, final TransferOptions options,
                                final TransferPrompt prompt, final TransferErrorCallback error,
                                final Cache cache) {
        super(transfer, options, prompt, error, cache);
        this.session = session;
    }

    public SingleTransferWorker(final Session session, final Transfer transfer, final TransferOptions options,
                                final TransferPrompt prompt, final TransferErrorCallback error,
                                final Map<Path, TransferStatus> table) {
        super(transfer, options, prompt, error, table);
        this.session = session;
    }

    @Override
    public Session borrow() {
        return session;
    }

    @Override
    protected void release(final Session session) throws BackgroundException {
        //
    }

    protected void submit(TransferCallable runnable) throws BackgroundException {
        runnable.call();
    }
}

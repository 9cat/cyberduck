package ch.cyberduck.ui.threading;

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

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.ui.Controller;
import ch.cyberduck.ui.action.Worker;

import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public class WorkerBackgroundAction<T> extends BrowserBackgroundAction<Boolean> {
    private static final Logger log = Logger.getLogger(WorkerBackgroundAction.class);

    private Worker<T> worker;

    private T result;

    public WorkerBackgroundAction(final Controller controller, final Session session,
                                  final Worker<T> worker) {
        super(controller, session, Cache.empty());
        this.worker = worker;
    }

    @Override
    public Boolean run() throws BackgroundException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Run worker %s", worker));
        }
        result = worker.run();
        return true;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if(!worker.isCanceled()) {
            if(null == result) {
                log.warn(String.format("No result for worker %s. Skip cleanup.", worker));
            }
            else {
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Cleanup worker %s", worker));
                }
                worker.cleanup(result);
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        if(log.isDebugEnabled()) {
            log.debug(String.format("Cancel worker %s", worker));
        }
        worker.cancel();
    }

    @Override
    public boolean isCanceled() {
        return worker.isCanceled();
    }

    @Override
    public String getActivity() {
        return worker.getActivity();
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final WorkerBackgroundAction that = (WorkerBackgroundAction) o;
        if(worker != null ? !worker.equals(that.worker) : that.worker != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return worker != null ? worker.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WorkerBackgroundAction{");
        sb.append("worker=").append(worker);
        sb.append(", result=").append(result);
        sb.append('}');
        return sb.toString();
    }
}

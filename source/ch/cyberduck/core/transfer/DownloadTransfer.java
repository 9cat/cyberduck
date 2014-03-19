package ch.cyberduck.core.transfer;

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

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Filter;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Download;
import ch.cyberduck.core.filter.DownloadRegexFilter;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.transfer.download.AbstractDownloadFilter;
import ch.cyberduck.core.transfer.download.CompareFilter;
import ch.cyberduck.core.transfer.download.IconUpdateSreamListener;
import ch.cyberduck.core.transfer.download.OverwriteFilter;
import ch.cyberduck.core.transfer.download.RenameExistingFilter;
import ch.cyberduck.core.transfer.download.RenameFilter;
import ch.cyberduck.core.transfer.download.ResumeFilter;
import ch.cyberduck.core.transfer.download.SkipFilter;
import ch.cyberduck.core.transfer.download.TrashFilter;
import ch.cyberduck.core.transfer.normalizer.DownloadRootPathsNormalizer;
import ch.cyberduck.core.transfer.symlink.DownloadSymlinkResolver;

import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @version $Id$
 */
public class DownloadTransfer extends Transfer {
    private static final Logger log = Logger.getLogger(DownloadTransfer.class);

    private Filter<Path> filter;

    private Cache cache
            = new Cache(Preferences.instance().getInteger("transfer.cache.size"));

    public DownloadTransfer(final Host host, final Path root, final Local local) {
        this(host, Collections.singletonList(new TransferItem(root, local)));
    }

    public DownloadTransfer(final Host host, final List<TransferItem> roots) {
        this(host, new DownloadRootPathsNormalizer().normalize(roots), new DownloadRegexFilter());
    }

    public DownloadTransfer(final Host host, final List<TransferItem> roots, final Filter<Path> f) {
        super(host, new DownloadRootPathsNormalizer().normalize(roots), new BandwidthThrottle(
                Preferences.instance().getFloat("queue.download.bandwidth.bytes")));
        this.filter = f;
    }

    public <T> DownloadTransfer(final T serialized) {
        super(serialized, new BandwidthThrottle(
                Preferences.instance().getFloat("queue.download.bandwidth.bytes")));
    }

    @Override
    public Type getType() {
        return Type.download;
    }

    @Override
    public List<TransferItem> list(final Session<?> session, final Path directory,
                                   final Local local, final ListProgressListener listener) throws BackgroundException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("List children for %s", directory));
        }
        if(directory.isSymbolicLink()
                && new DownloadSymlinkResolver(this.getRoots()).resolve(directory)) {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Do not list children for symbolic link %s", directory));
            }
            return Collections.emptyList();
        }
        else {
            final AttributedList<Path> list = session.list(directory, listener);
            final List<TransferItem> children = new ArrayList<TransferItem>();
            // Return copy with filtered result only
            for(Path f : new AttributedList<Path>(list.filter(filter))) {
                children.add(new TransferItem(f, LocalFactory.createLocal(local, f.getName())));
            }
            return children;
        }
    }

    @Override
    public AbstractDownloadFilter filter(final Session<?> session, final TransferAction action) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Filter transfer with action %s", action));
        }
        final DownloadSymlinkResolver resolver = new DownloadSymlinkResolver(this.getRoots());
        if(action.equals(TransferAction.resume)) {
            return new ResumeFilter(resolver, session).withCache(cache);
        }
        if(action.equals(TransferAction.rename)) {
            return new RenameFilter(resolver, session).withCache(cache);
        }
        if(action.equals(TransferAction.renameexisting)) {
            return new RenameExistingFilter(resolver, session).withCache(cache);
        }
        if(action.equals(TransferAction.skip)) {
            return new SkipFilter(resolver, session).withCache(cache);
        }
        if(action.equals(TransferAction.trash)) {
            return new TrashFilter(resolver, session).withCache(cache);
        }
        if(action.equals(TransferAction.comparison)) {
            return new CompareFilter(resolver, session).withCache(cache);
        }
        return new OverwriteFilter(resolver, session).withCache(cache);
    }

    @Override
    public TransferAction action(final Session<?> session, final boolean resumeRequested, final boolean reloadRequested,
                                 final TransferPrompt prompt) throws BackgroundException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Find transfer action for Resume=%s,Reload=%s", resumeRequested, reloadRequested));
        }
        final TransferAction action;
        if(resumeRequested) {
            // Force resume
            action = TransferAction.resume;
        }
        else if(reloadRequested) {
            action = TransferAction.forName(
                    Preferences.instance().getProperty("queue.download.reload.action"));
        }
        else {
            // Use default
            action = TransferAction.forName(
                    Preferences.instance().getProperty("queue.download.action")
            );
        }
        if(action.equals(TransferAction.callback)) {
            for(TransferItem download : roots) {
                final Local local = download.local;
                if(local.exists()) {
                    if(local.isDirectory()) {
                        if(local.list().isEmpty()) {
                            // Do not prompt for existing empty directories
                            continue;
                        }
                    }
                    if(local.isFile()) {
                        if(local.attributes().getSize() == 0) {
                            // Dragging a file to the local volume creates the file already
                            continue;
                        }
                    }
                    // Prompt user to choose a filter
                    return prompt.prompt();
                }
            }
            // No files exist yet therefore it is most straightforward to use the overwrite action
            return TransferAction.overwrite;
        }
        return action;
    }

    @Override
    public void transfer(final Session<?> session, final Path file, Local local, final TransferOptions options,
                         final TransferStatus status) throws BackgroundException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Transfer file %s with options %s", file, options));
        }
        final DownloadSymlinkResolver symlinkResolver = new DownloadSymlinkResolver(roots);
        if(file.isSymbolicLink() && symlinkResolver.resolve(file)) {
            // Make relative symbolic link
            final String target = symlinkResolver.relativize(file.getAbsolute(),
                    file.getSymlinkTarget().getAbsolute());
            if(log.isDebugEnabled()) {
                log.debug(String.format("Create symbolic link from %s to %s", local, target));
            }
            local.symlink(target);
        }
        else if(file.isFile()) {
            session.message(MessageFormat.format(LocaleFactory.localizedString("Downloading {0}", "Status"),
                    file.getName()));
            local.getParent().mkdir();
            // Transfer
            final Download download = session.getFeature(Download.class);
            download.download(file, local, bandwidth, new IconUpdateSreamListener(status, local) {
                @Override
                public void recv(long bytes) {
                    addTransferred(bytes);
                    super.recv(bytes);
                }
            }, status);
        }
        else if(file.isDirectory()) {
            if(!status.isExists()) {
                local.mkdir();
                status.setComplete();
            }
        }
    }
}
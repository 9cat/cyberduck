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

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.Filter;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathNormalizer;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Symlink;
import ch.cyberduck.core.features.Upload;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.filter.UploadRegexFilter;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.io.DisabledStreamListener;
import ch.cyberduck.core.transfer.normalizer.UploadRootPathsNormalizer;
import ch.cyberduck.core.transfer.symlink.UploadSymlinkResolver;
import ch.cyberduck.core.transfer.upload.AbstractUploadFilter;
import ch.cyberduck.core.transfer.upload.CompareFilter;
import ch.cyberduck.core.transfer.upload.OverwriteFilter;
import ch.cyberduck.core.transfer.upload.RenameExistingFilter;
import ch.cyberduck.core.transfer.upload.RenameFilter;
import ch.cyberduck.core.transfer.upload.ResumeFilter;
import ch.cyberduck.core.transfer.upload.SkipFilter;

import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * @version $Id$
 */
public class UploadTransfer extends Transfer {
    private static final Logger log = Logger.getLogger(UploadTransfer.class);

    private Filter<Local> filter;

    private Comparator<Local> comparator;

    private Cache cache
            = new Cache(Preferences.instance().getInteger("transfer.cache.size"));

    public UploadTransfer(final Host host, final Path root, final Local local) {
        this(host, Collections.singletonList(new TransferItem(root, local)));
    }

    public UploadTransfer(final Host session, final List<TransferItem> roots) {
        this(session, new UploadRootPathsNormalizer().normalize(roots), new UploadRegexFilter());
    }

    public UploadTransfer(final Host session, final List<TransferItem> roots, final Filter<Local> f) {
        this(session, roots, f, new Comparator<Local>() {
            @Override
            public int compare(Local o1, Local o2) {
                final String pattern = Preferences.instance().getProperty("queue.upload.priority.regex");
                if(PathNormalizer.name(o1.getAbsolute()).matches(pattern)) {
                    return -1;
                }
                if(PathNormalizer.name(o2.getAbsolute()).matches(pattern)) {
                    return 1;
                }
                return 0;
            }
        });
    }

    public UploadTransfer(final Host session, final List<TransferItem> roots, final Filter<Local> f, final Comparator<Local> comparator) {
        super(session, new UploadRootPathsNormalizer().normalize(roots), new BandwidthThrottle(
                Preferences.instance().getFloat("queue.upload.bandwidth.bytes")));
        filter = f;
        this.comparator = comparator;
    }

    @Override
    public Type getType() {
        return Type.upload;
    }

    @Override
    public List<TransferItem> list(final Session<?> session, final Path remote,
                                   final Local directory, final ListProgressListener listener) throws BackgroundException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("List children for %s", directory));
        }
        if(directory.isSymbolicLink()) {
            final Symlink symlink = session.getFeature(Symlink.class);
            if(new UploadSymlinkResolver(symlink, roots).resolve(directory)) {
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Do not list children for symbolic link %s", directory));
                }
                // We can resolve the target of the symbolic link and will create a link on the remote system
                // using the symlink feature of the session
                return Collections.emptyList();
            }
        }
        final List<TransferItem> children = new ArrayList<TransferItem>();
        for(Local local : directory.list().filter(comparator, filter)) {
            children.add(new TransferItem(new Path(remote, local.getName(),
                    local.isDirectory() ? EnumSet.of(Path.Type.directory) : EnumSet.of(Path.Type.file)), local));
        }
        return children;
    }

    @Override
    public synchronized void reset() {
        cache.clear();
        super.reset();
    }

    @Override
    public AbstractUploadFilter filter(final Session<?> session, final TransferAction action) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Filter transfer with action %s", action));
        }
        final Symlink symlink = session.getFeature(Symlink.class);
        final UploadSymlinkResolver resolver = new UploadSymlinkResolver(symlink, roots);
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
            action = TransferAction.forName(Preferences.instance().getProperty("queue.upload.reload.action"));
        }
        else {
            // Use default
            action = TransferAction.forName(Preferences.instance().getProperty("queue.upload.action"));
        }
        if(action.equals(TransferAction.callback)) {
            for(TransferItem f : roots) {
                final Write write = session.getFeature(Write.class);
                final Write.Append append = write.append(f.remote, f.local.attributes().getSize(), cache);
                if(append.override || append.append) {
                    // Found remote file
                    if(f.remote.isDirectory()) {
                        if(this.list(session, f.remote, f.local, new DisabledListProgressListener()).isEmpty()) {
                            // Do not prompt for existing empty directories
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
        final Symlink symlink = session.getFeature(Symlink.class);
        final UploadSymlinkResolver symlinkResolver = new UploadSymlinkResolver(symlink, roots);
        if(local.isSymbolicLink() && symlinkResolver.resolve(local)) {
            // Make relative symbolic link
            final String target = symlinkResolver.relativize(local.getAbsolute(),
                    local.getSymlinkTarget().getAbsolute());
            if(log.isDebugEnabled()) {
                log.debug(String.format("Create symbolic link from %s to %s", file, target));
            }
            symlink.symlink(file, target);
        }
        else if(file.isFile()) {
            session.message(MessageFormat.format(LocaleFactory.localizedString("Uploading {0}", "Status"),
                    file.getName()));
            // Transfer
            final Upload upload = session.getFeature(Upload.class);
            upload.upload(file, local, bandwidth, new DisabledStreamListener() {
                @Override
                public void sent(long bytes) {
                    addTransferred(bytes);
                }
            }, status);
        }
        else if(file.isDirectory()) {
            session.message(MessageFormat.format(LocaleFactory.localizedString("Making directory {0}", "Status"),
                    file.getName()));
            if(!status.isExists()) {
                session.getFeature(Directory.class).mkdir(file);
                status.setComplete();
            }
        }
    }
}

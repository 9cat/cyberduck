package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
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

import ch.cyberduck.core.*;
import ch.cyberduck.core.editor.WatchEditor;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.features.Touch;
import ch.cyberduck.core.formatter.SizeFormatterFactory;
import ch.cyberduck.core.local.FileDescriptor;
import ch.cyberduck.core.local.FileDescriptorFactory;
import ch.cyberduck.core.local.IconServiceFactory;
import ch.cyberduck.core.local.LocalTouchFactory;
import ch.cyberduck.core.transfer.CopyTransfer;
import ch.cyberduck.core.transfer.DownloadTransfer;
import ch.cyberduck.core.transfer.TransferItem;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.UploadTransfer;
import ch.cyberduck.ui.action.SessionListWorker;
import ch.cyberduck.ui.cocoa.application.NSApplication;
import ch.cyberduck.ui.cocoa.application.NSDraggingInfo;
import ch.cyberduck.ui.cocoa.application.NSDraggingSource;
import ch.cyberduck.ui.cocoa.application.NSEvent;
import ch.cyberduck.ui.cocoa.application.NSImage;
import ch.cyberduck.ui.cocoa.application.NSPasteboard;
import ch.cyberduck.ui.cocoa.application.NSTableView;
import ch.cyberduck.ui.cocoa.foundation.NSArray;
import ch.cyberduck.ui.cocoa.foundation.NSAttributedString;
import ch.cyberduck.ui.cocoa.foundation.NSFileManager;
import ch.cyberduck.ui.cocoa.foundation.NSMutableArray;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.foundation.NSString;
import ch.cyberduck.ui.cocoa.foundation.NSURL;
import ch.cyberduck.ui.pasteboard.PathPasteboard;
import ch.cyberduck.ui.pasteboard.PathPasteboardFactory;
import ch.cyberduck.ui.resources.IconCacheFactory;
import ch.cyberduck.ui.threading.WorkerBackgroundAction;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSInteger;
import org.rococoa.cocoa.foundation.NSPoint;
import org.rococoa.cocoa.foundation.NSRect;
import org.rococoa.cocoa.foundation.NSSize;
import org.rococoa.cocoa.foundation.NSUInteger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @version $Id$
 */
public abstract class BrowserTableDataSource extends ProxyController implements NSDraggingSource {
    private static final Logger log = Logger.getLogger(BrowserTableDataSource.class);

    public enum Column {
        icon,
        filename,
        size,
        modified,
        owner,
        group,
        permission,
        kind,
        extension,
        region,
        version
    }

    private FileDescriptor descriptor = FileDescriptorFactory.get();

    protected BrowserController controller;

    protected Cache<Path> cache;

    private ListProgressListener listener;

    protected BrowserTableDataSource(final BrowserController controller, final Cache cache) {
        this.controller = controller;
        this.cache = cache;
        this.listener = new PromptLimitedListProgressListener(controller);
    }

    /**
     * Must be efficient; called very frequently by the table view
     *
     * @param directory The directory to fetch the children from
     * @return The cached or newly fetched file listing of the directory
     */
    protected AttributedList<Path> list(final Path directory) {
        if(!cache.isCached(directory.getReference())) {
            // Reloading a working directory that is not cached yet would cause the interface to freeze;
            // Delay until path is cached in the background
            controller.background(new WorkerBackgroundAction(controller, controller.getSession(),
                    new SessionListWorker(controller.getSession(), cache, directory, listener) {
                        @Override
                        public void cleanup(final AttributedList<Path> list) {
                            if(controller.getActions().isEmpty()) {
                                controller.reloadData(true, true);
                            }
                        }
                    }
            )
            );
        }
        return this.get(directory);
    }

    protected AttributedList<Path> get(final Path directory) {
        return cache.get(directory.getReference()).filter(controller.getComparator(), controller.getFilter());
    }

    public int indexOf(NSTableView view, PathReference reference) {
        return this.get(controller.workdir()).indexOf(reference);
    }

    protected void setObjectValueForItem(final Path item, final NSObject value, final String identifier) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Set new value %s for item %s", value, item));
        }
        if(identifier.equals(Column.filename.name())) {
            if(StringUtils.isNotBlank(value.toString()) && !item.getName().equals(value.toString())) {
                final Path renamed = new Path(
                        item.getParent(), value.toString(), item.getType());
                controller.renamePath(item, renamed);
            }
        }
    }

    protected NSImage iconForPath(final Path item) {
        if(item.isVolume()) {
            return IconCacheFactory.<NSImage>get().volumeIcon(controller.getSession().getHost().getProtocol(), 16);
        }
        return IconCacheFactory.<NSImage>get().fileIcon(item, 16);
    }

    protected NSObject objectValueForItem(final Path item, final String identifier) {
        if(null == item) {
            return null;
        }
        if(log.isTraceEnabled()) {
            log.trace("objectValueForItem:" + item.getAbsolute());
        }
        if(identifier.equals(Column.icon.name())) {
            return this.iconForPath(item);
        }
        if(identifier.equals(Column.filename.name())) {
            return NSAttributedString.attributedStringWithAttributes(
                    item.getName(),
                    TableCellAttributes.browserFontLeftAlignment());
        }
        if(identifier.equals(Column.size.name())) {
            return NSAttributedString.attributedStringWithAttributes(
                    SizeFormatterFactory.get().format(item.attributes().getSize()),
                    TableCellAttributes.browserFontRightAlignment());
        }
        if(identifier.equals(Column.modified.name())) {
            return NSAttributedString.attributedStringWithAttributes(
                    UserDateFormatterFactory.get().getShortFormat(item.attributes().getModificationDate(),
                            Preferences.instance().getBoolean("browser.date.natural")),
                    TableCellAttributes.browserFontLeftAlignment()
            );
        }
        if(identifier.equals(Column.owner.name())) {
            return NSAttributedString.attributedStringWithAttributes(
                    StringUtils.isBlank(item.attributes().getOwner()) ?
                            LocaleFactory.localizedString("Unknown") : item.attributes().getOwner(),
                    TableCellAttributes.browserFontLeftAlignment());
        }
        if(identifier.equals(Column.group.name())) {
            return NSAttributedString.attributedStringWithAttributes(
                    StringUtils.isBlank(item.attributes().getGroup()) ?
                            LocaleFactory.localizedString("Unknown") : item.attributes().getGroup(),
                    TableCellAttributes.browserFontLeftAlignment());
        }
        if(identifier.equals(Column.permission.name())) {
            final Acl acl = item.attributes().getAcl();
            if(!Acl.EMPTY.equals(acl)) {
                final StringBuilder s = new StringBuilder();
                for(Map.Entry<Acl.User, Set<Acl.Role>> entry : acl.entrySet()) {
                    s.append(String.format("%s%s:%s", s.length() == 0 ? StringUtils.EMPTY : ", ",
                            entry.getKey().getDisplayName(), entry.getValue()));
                }
                return NSAttributedString.attributedStringWithAttributes(s.toString(),
                        TableCellAttributes.browserFontLeftAlignment());
            }
            final Permission permission = item.attributes().getPermission();
            return NSAttributedString.attributedStringWithAttributes(
                    permission.toString(),
                    TableCellAttributes.browserFontLeftAlignment());
        }
        if(identifier.equals(Column.kind.name())) {
            return NSAttributedString.attributedStringWithAttributes(
                    descriptor.getKind(item),
                    TableCellAttributes.browserFontLeftAlignment());
        }
        if(identifier.equals(Column.extension.name())) {
            return NSAttributedString.attributedStringWithAttributes(
                    item.isFile() ? StringUtils.isNotBlank(item.getExtension()) ? item.getExtension() :
                            LocaleFactory.localizedString("None") : LocaleFactory.localizedString("None"),
                    TableCellAttributes.browserFontLeftAlignment());
        }
        if(identifier.equals(Column.region.name())) {
            return NSAttributedString.attributedStringWithAttributes(
                    StringUtils.isNotBlank(item.attributes().getRegion()) ? item.attributes().getRegion() :
                            LocaleFactory.localizedString("Unknown"),
                    TableCellAttributes.browserFontLeftAlignment());
        }
        if(identifier.equals(Column.version.name())) {
            return NSAttributedString.attributedStringWithAttributes(
                    StringUtils.isNotBlank(item.attributes().getVersionId()) ? item.attributes().getVersionId() :
                            LocaleFactory.localizedString("None"),
                    TableCellAttributes.browserFontLeftAlignment());
        }
        throw new IllegalArgumentException(String.format("Unknown identifier %s", identifier));
    }

    /**
     * Sets whether the use of modifier keys should have an effect on the type of operation performed.
     *
     * @return Always false
     * @see NSDraggingSource
     */
    @Override
    public boolean ignoreModifierKeysWhileDragging() {
        // If this method is not implemented or returns false, the user can tailor the drag operation by
        // holding down a modifier key during the drag.
        return false;
    }

    /**
     * @param local indicates that the candidate destination object (the window or view over which the dragged
     *              image is currently poised) is in the same application as the source, while a NO value indicates that
     *              the destination object is in a different application
     * @return A mask, created by combining the dragging operations listed in the NSDragOperation section of
     *         NSDraggingInfo protocol reference using the C bitwise OR operator.If the source does not permit
     *         any dragging operations, it should return NSDragOperationNone.
     * @see NSDraggingSource
     */
    @Override
    public NSUInteger draggingSourceOperationMaskForLocal(final boolean local) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Request dragging source operation mask for %s", local));
        }
        if(local) {
            // Move or copy within the browser
            return new NSUInteger(NSDraggingInfo.NSDragOperationMove.intValue() | NSDraggingInfo.NSDragOperationCopy.intValue());
        }
        // Copy to a thirdparty application or drag to trash to delete
        return new NSUInteger(NSDraggingInfo.NSDragOperationCopy.intValue() | NSDraggingInfo.NSDragOperationDelete.intValue());
    }

    /**
     * @param view        Table
     * @param destination A directory or null to mount an URL
     * @param info        Dragging pasteboard
     * @return True if accepted
     */
    public boolean acceptDrop(final NSTableView view, final Path destination, final NSDraggingInfo info) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Accept drop for destination %s", destination));
        }
        if(info.draggingPasteboard().availableTypeFromArray(NSArray.arrayWithObject(NSPasteboard.URLPboardType)) != null) {
            NSObject o = info.draggingPasteboard().propertyListForType(NSPasteboard.URLPboardType);
            // Mount .webloc URLs dragged to browser window
            if(o != null) {
                final NSArray elements = Rococoa.cast(o, NSArray.class);
                for(int i = 0; i < elements.count().intValue(); i++) {
                    if(ProtocolFactory.isURL(elements.objectAtIndex(new NSUInteger(i)).toString())) {
                        controller.mount(HostParser.parse(elements.objectAtIndex(new NSUInteger(i)).toString()));
                        return true;
                    }
                }
            }
        }
        if(controller.isMounted()) {
            if(info.draggingPasteboard().availableTypeFromArray(NSArray.arrayWithObject(NSPasteboard.FilenamesPboardType)) != null) {
                NSObject o = info.draggingPasteboard().propertyListForType(NSPasteboard.FilenamesPboardType);
                // A file drag has been received by another application; upload to the dragged directory
                if(o != null) {
                    final NSArray elements = Rococoa.cast(o, NSArray.class);
                    final List<TransferItem> roots = new ArrayList<TransferItem>();
                    for(int i = 0; i < elements.count().intValue(); i++) {
                        final Local local = LocalFactory.createLocal(elements.objectAtIndex(new NSUInteger(i)).toString());
                        roots.add(new TransferItem(new Path(destination, local.getName(),
                                local.isDirectory() ? EnumSet.of(Path.Type.directory) : EnumSet.of(Path.Type.file)), local));
                    }
                    controller.transfer(new UploadTransfer(controller.getSession().getHost(), roots));
                    return true;
                }
                return false;
            }
            final List<PathPasteboard> pasteboards = PathPasteboardFactory.allPasteboards();
            for(PathPasteboard pasteboard : pasteboards) {
                // A file dragged within the browser has been received
                if(pasteboard.isEmpty()) {
                    continue;
                }
                if(info.draggingSourceOperationMask().intValue() == NSDraggingInfo.NSDragOperationCopy.intValue()
                        || pasteboard.getSession().getHost().compareTo(controller.getSession().getHost()) != 0) {
                    // Drag to browser windows with different session or explicit copy requested by user.
                    final Map<Path, Path> files = new HashMap<Path, Path>();
                    for(Path file : pasteboard) {
                        files.put(file, new Path(destination, file.getName(), file.getType()));
                    }
                    controller.transfer(new CopyTransfer(pasteboard.getSession().getHost(),
                            controller.getSession().getHost(),
                            files), new ArrayList<Path>(files.values()), false);
                }
                else {
                    // The file should be renamed
                    final Map<Path, Path> files = new HashMap<Path, Path>();
                    for(Path next : pasteboard) {
                        Path renamed = new Path(
                                destination, next.getName(), next.getType());
                        files.put(next, renamed);
                    }
                    controller.renamePaths(files);
                }
                pasteboard.clear();
            }
            return true;
        }
        return false;
    }

    /**
     * @param view        Table
     * @param destination A directory or null to mount an URL
     * @param row         Index
     * @param info        Dragging pasteboard
     * @return Drag operation
     */
    public NSUInteger validateDrop(NSTableView view, Path destination, NSInteger row, NSDraggingInfo info) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Validate drop for destination %s", destination));
        }
        if(info.draggingPasteboard().availableTypeFromArray(NSArray.arrayWithObject(NSPasteboard.URLPboardType)) != null) {
            // Dragging URLs to mount new session
            NSObject o = info.draggingPasteboard().propertyListForType(NSPasteboard.URLPboardType);
            if(o != null) {
                NSArray elements = Rococoa.cast(o, NSArray.class);
                for(int i = 0; i < elements.count().intValue(); i++) {
                    // Validate if .webloc URLs dragged to browser window have a known protocol
                    if(ProtocolFactory.isURL(elements.objectAtIndex(new NSUInteger(i)).toString())) {
                        // Passing a value of –1 for row, and NSTableViewDropOn as the operation causes the
                        // entire table view to be highlighted rather than a specific row.
                        view.setDropRow(new NSInteger(-1), NSTableView.NSTableViewDropOn);
                        return NSDraggingInfo.NSDragOperationCopy;
                    }
                    else {
                        log.warn(String.format("Protocol not supported for URL %s", elements.objectAtIndex(new NSUInteger(i)).toString()));
                    }
                }
            }
            else {
                log.warn("URL dragging pasteboard is empty.");
            }
        }
        if(controller.isMounted()) {
            if(null == destination) {
                log.warn("Dragging destination is null.");
                return NSDraggingInfo.NSDragOperationNone;
            }
            final Touch feature = controller.getSession().getFeature(Touch.class);
            if(!feature.isSupported(destination)) {
                // Target file system does not support creating files. Creating files is not supported
                // for example in root of cloud storage accounts.
                return NSDraggingInfo.NSDragOperationNone;
            }
            // Files dragged form other application
            if(info.draggingPasteboard().availableTypeFromArray(NSArray.arrayWithObject(NSPasteboard.FilenamesPboardType)) != null) {
                this.setDropRowAndDropOperation(view, destination, row);
                return NSDraggingInfo.NSDragOperationCopy;
            }
            // Files dragged from browser
            for(Path next : controller.getPasteboard()) {
                if(destination.equals(next)) {
                    // Do not allow dragging onto myself
                    return NSDraggingInfo.NSDragOperationNone;
                }
                if(next.isDirectory() && destination.isChild(next)) {
                    // Do not allow dragging a directory into its own containing items
                    return NSDraggingInfo.NSDragOperationNone;
                }
                if(next.isFile() && next.getParent().equals(destination)) {
                    // Moving a file to the same destination makes no sense
                    return NSDraggingInfo.NSDragOperationNone;
                }
            }
            if(log.isDebugEnabled()) {
                log.debug(String.format("Drag operation mas is %d", info.draggingSourceOperationMask().intValue()));
            }
            this.setDropRowAndDropOperation(view, destination, row);
            final List<PathPasteboard> pasteboards = PathPasteboardFactory.allPasteboards();
            for(PathPasteboard pasteboard : pasteboards) {
                if(pasteboard.isEmpty()) {
                    continue;
                }
                if(pasteboard.getSession().getHost().compareTo(controller.getSession().getHost()) == 0) {
                    if(info.draggingSourceOperationMask().intValue() == NSDraggingInfo.NSDragOperationCopy.intValue()) {
                        // Explicit copy requested if drag operation is already NSDragOperationCopy. User is pressing the option key.
                        return NSDraggingInfo.NSDragOperationCopy;
                    }
                    // Defaulting to move for same session
                    return NSDraggingInfo.NSDragOperationMove;
                }
                else {
                    // If copying between sessions is supported
                    return NSDraggingInfo.NSDragOperationCopy;
                }
            }
        }
        return NSDraggingInfo.NSDragOperationNone;
    }

    private void setDropRowAndDropOperation(final NSTableView view, final Path destination, final NSInteger row) {
        if(destination.equals(controller.workdir())) {
            log.debug("setDropRowAndDropOperation:-1");
            // Passing a value of –1 for row, and NSTableViewDropOn as the operation causes the
            // entire table view to be highlighted rather than a specific row.
            view.setDropRow(new NSInteger(-1), NSTableView.NSTableViewDropOn);
        }
        else if(destination.isDirectory()) {
            log.debug("setDropRowAndDropOperation:" + row.intValue());
            view.setDropRow(row, NSTableView.NSTableViewDropOn);
        }
    }

    public boolean writeItemsToPasteBoard(final NSTableView view, final List<Path> selected, final NSPasteboard pboard) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Write items to pasteboard %s", pboard));
        }
        if(controller.isMounted()) {
            if(selected.size() > 0) {
                // The fileTypes argument is the list of fileTypes being promised.
                // The array elements can consist of file extensions and HFS types encoded
                // with the NSHFSFileTypes method fileTypeForHFSTypeCode. If promising a directory
                // of files, only include the top directory in the array.
                final NSMutableArray fileTypes = NSMutableArray.array();
                final PathPasteboard pasteboard = controller.getPasteboard();
                for(final Path f : selected) {
                    if(f.isFile()) {
                        if(StringUtils.isNotEmpty(f.getExtension())) {
                            fileTypes.addObject(NSString.stringWithString(f.getExtension()));
                        }
                        else {
                            fileTypes.addObject(NSString.stringWithString(NSFileManager.NSFileTypeRegular));
                        }
                    }
                    else if(f.isDirectory()) {
                        fileTypes.addObject(NSString.stringWithString("'fldr'")); //NSFileTypeForHFSTypeCode('fldr')
                    }
                    else {
                        fileTypes.addObject(NSString.stringWithString(NSFileManager.NSFileTypeUnknown));
                    }
                    // Writing data for private use when the item gets dragged to the transfer queue.
                    pasteboard.add(f);
                }
                NSEvent event = NSApplication.sharedApplication().currentEvent();
                if(event != null) {
                    NSPoint dragPosition = view.convertPoint_fromView(event.locationInWindow(), null);
                    NSRect imageRect = new NSRect(new NSPoint(dragPosition.x.doubleValue() - 16, dragPosition.y.doubleValue() - 16), new NSSize(32, 32));
                    view.dragPromisedFilesOfTypes(fileTypes, imageRect, this.id(), true, event);
                    // @see http://www.cocoabuilder.com/archive/message/cocoa/2003/5/15/81424
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void draggedImage_beganAt(final NSImage image, final NSPoint point) {
        if(log.isTraceEnabled()) {
            log.trace("draggedImage_beganAt:" + point);
        }
    }

    /**
     * See http://www.cocoabuilder.com/archive/message/2005/10/5/118857
     */
    @Override
    public void draggedImage_endedAt_operation(final NSImage image, final NSPoint point, final NSUInteger operation) {
        if(log.isTraceEnabled()) {
            log.trace("draggedImage_endedAt_operation:" + operation);
        }
        final PathPasteboard pasteboard = controller.getPasteboard();
        if(NSDraggingInfo.NSDragOperationDelete.intValue() == operation.intValue()) {
            controller.deletePaths(pasteboard);
        }
        pasteboard.clear();
    }

    @Override
    public void draggedImage_movedTo(final NSImage image, final NSPoint point) {
        if(log.isTraceEnabled()) {
            log.trace("draggedImage_movedTo:" + point);
        }
    }

    /**
     * @return the names (not full paths) of the files that the receiver promises to create at dropDestination.
     *         This method is invoked when the drop has been accepted by the destination and the destination, in the case of another
     *         Cocoa application, invokes the NSDraggingInfo method namesOfPromisedFilesDroppedAtDestination. For long operations,
     *         you can cache dropDestination and defer the creation of the files until the finishedDraggingImage method to avoid
     *         blocking the destination application.
     */
    @Override
    public NSArray namesOfPromisedFilesDroppedAtDestination(final NSURL url) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Return names of promised files dropped at %s", url));
        }
        NSMutableArray promisedDragNames = NSMutableArray.array();
        if(null != url) {
            final Local destination = LocalFactory.createLocal(url.path());
            final PathPasteboard pasteboard = controller.getPasteboard();
            final List<TransferItem> downloads = new ArrayList<TransferItem>();
            for(Path p : pasteboard) {
                downloads.add(new TransferItem(p, LocalFactory.createLocal(destination, p.getName())));
                // Add to returned path names
                promisedDragNames.addObject(NSString.stringWithString(p.getName()));
            }
            if(downloads.size() == 1) {
                if(downloads.iterator().next().remote.isFile()) {
                    final Local file = downloads.iterator().next().local;
                    if(!file.exists()) {
                        try {
                            LocalTouchFactory.get().touch(file);
                            IconServiceFactory.get().set(file, new TransferStatus());
                        }
                        catch(AccessDeniedException e) {
                            log.warn(String.format("Failure creating file %s %s", file, e.getMessage()));
                        }
                    }
                }
                if(downloads.iterator().next().remote.isDirectory()) {
                    final Local file = downloads.iterator().next().local;
                    if(!file.exists()) {
                        try {
                            file.mkdir();
                        }
                        catch(AccessDeniedException e) {
                            log.warn(e.getMessage());
                        }
                    }
                }
            }
            // kTemporaryFolderType
            final boolean dock = destination.equals(LocalFactory.createLocal("~/Library/Caches/TemporaryItems"));
            if(dock) {
                for(Path p : pasteboard) {
                    // Drag to application icon in dock.
                    WatchEditor editor = new WatchEditor(controller, controller.getSession(), null, p);
                    try {
                        // download
                        editor.watch();
                    }
                    catch(IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }
            else {
                final DownloadTransfer transfer = new DownloadTransfer(controller.getSession().getHost(), downloads);
                controller.transfer(transfer, Collections.<Path>emptyList());
            }
            pasteboard.clear();
        }
        // Filenames
        return promisedDragNames;
    }
}

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

import ch.cyberduck.core.transfer.SyncTransfer;
import ch.cyberduck.core.transfer.TransferAction;
import ch.cyberduck.ui.cocoa.application.NSOutlineView;
import ch.cyberduck.ui.cocoa.application.NSTableColumn;
import ch.cyberduck.ui.cocoa.application.NSText;

import org.apache.commons.lang3.StringUtils;

/**
 * @version $Id$
 */
public class SyncPromptController extends TransferPromptController {

    private final TableColumnFactory tableColumnsFactory
            = new TableColumnFactory();

    public SyncPromptController(final WindowController parent, final SyncTransfer transfer) {
        super(parent, transfer);
        browserModel = new SyncPromptModel(this, transfer, cache);
    }

    @Override
    public void setBrowserView(NSOutlineView view) {
        super.setBrowserView(view);
        {
            NSTableColumn c = tableColumnsFactory.create(SyncPromptModel.Column.sync.name());
            c.headerCell().setStringValue(StringUtils.EMPTY);
            c.setMinWidth(20f);
            c.setWidth(20f);
            c.setMaxWidth(20f);
            c.setResizingMask(NSTableColumn.NSTableColumnAutoresizingMask);
            c.setEditable(false);
            c.setDataCell(imageCellPrototype);
            c.dataCell().setAlignment(NSText.NSCenterTextAlignment);
            view.addTableColumn(c);
        }
        {
            NSTableColumn c = tableColumnsFactory.create(SyncPromptModel.Column.create.name());
            c.headerCell().setStringValue(StringUtils.EMPTY);
            c.setMinWidth(20f);
            c.setWidth(20f);
            c.setMaxWidth(20f);
            c.setResizingMask(NSTableColumn.NSTableColumnAutoresizingMask);
            c.setEditable(false);
            c.setDataCell(imageCellPrototype);
            c.dataCell().setAlignment(NSText.NSCenterTextAlignment);
            view.addTableColumn(c);
        }
        view.sizeToFit();
    }

    @Override
    protected TransferAction[] getTransferActions() {
        return new TransferAction[]{
                SyncTransfer.ACTION_DOWNLOAD,
                SyncTransfer.ACTION_UPLOAD,
                SyncTransfer.ACTION_MIRROR
        };
    }
}
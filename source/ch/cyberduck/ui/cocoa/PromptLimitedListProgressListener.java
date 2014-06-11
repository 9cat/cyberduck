package ch.cyberduck.ui.cocoa;

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
import ch.cyberduck.core.LimitedListProgressListener;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.exception.ListCanceledException;
import ch.cyberduck.ui.cocoa.application.NSAlert;
import ch.cyberduck.ui.cocoa.application.NSCell;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @version $Id$
 */
public class PromptLimitedListProgressListener extends LimitedListProgressListener {

    private WindowController controller;

    private boolean suppressed;

    public PromptLimitedListProgressListener(final WindowController controller) {
        this(controller, Preferences.instance().getInteger("browser.model.size.limit"));
    }

    public PromptLimitedListProgressListener(final WindowController controller, final Integer limit) {
        super(limit);
        this.controller = controller;
    }

    @Override
    public void chunk(final AttributedList<Path> list) throws ListCanceledException {
        if(suppressed) {
            return;
        }
        try {
            super.chunk(list);
        }
        catch(ListCanceledException e) {
            if(controller.isVisible()) {
                final AtomicBoolean c = new AtomicBoolean(true);
                final NSAlert alert = NSAlert.alert(
                        MessageFormat.format(LocaleFactory.localizedString("Listing directory {0}", "Status"), StringUtils.EMPTY),
                        MessageFormat.format(LocaleFactory.localizedString("Continue listing directory with more than {0} files.", "Alert"), e.getChunk().size()),
                        LocaleFactory.localizedString("Continue", "Credentials"),
                        null,
                        LocaleFactory.localizedString("Cancel")
                );
                alert.setShowsSuppressionButton(true);
                alert.suppressionButton().setTitle(LocaleFactory.localizedString("Always"));
                final AlertController controller = new AlertController(PromptLimitedListProgressListener.this.controller, alert) {
                    @Override
                    public void callback(final int returncode) {
                        if(returncode == SheetCallback.CANCEL_OPTION) {
                            c.set(false);
                        }
                        if(alert.suppressionButton().state() == NSCell.NSOnState) {
                            suppressed = true;
                        }
                    }
                };
                controller.beginSheet();
                if(!c.get()) {
                    throw e;
                }
            }
        }
    }
}

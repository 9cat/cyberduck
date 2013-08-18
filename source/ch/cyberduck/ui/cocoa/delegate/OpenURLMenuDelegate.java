package ch.cyberduck.ui.cocoa.delegate;

/*
 * Copyright (c) 2012 David Kocher. All rights reserved.
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
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.DescriptiveUrl;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.UrlProvider;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.local.BrowserLauncherFactory;
import ch.cyberduck.ui.cocoa.application.NSEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @version $Id:$
 */
public abstract class OpenURLMenuDelegate extends URLMenuDelegate {

    @Override
    protected String getKeyEquivalent() {
        return "b";
    }

    @Override
    protected int getModifierMask() {
        return NSEvent.NSCommandKeyMask | NSEvent.NSShiftKeyMask;
    }

    @Override
    protected List<DescriptiveUrl> getURLs(Path selected) {
        final ArrayList<DescriptiveUrl> list = new ArrayList<DescriptiveUrl>();
        list.addAll(this.getSession().getFeature(UrlProvider.class).toUrl(selected).filter(
                DescriptiveUrl.Type.http, DescriptiveUrl.Type.cname, DescriptiveUrl.Type.cdn,
                DescriptiveUrl.Type.signed, DescriptiveUrl.Type.authenticated, DescriptiveUrl.Type.torrent));
        list.addAll(this.getSession().getFeature(DistributionConfiguration.class).toUrl(selected));
        return list;
    }

    @Override
    public void handle(final List<DescriptiveUrl> selected) {
        for(DescriptiveUrl url : selected) {
            BrowserLauncherFactory.get().open(url.getUrl());
        }
    }
}
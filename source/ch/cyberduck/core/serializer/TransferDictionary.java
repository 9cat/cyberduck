package ch.cyberduck.core.serializer;

/*
 * Copyright (c) 2002-2014 David Kocher. All rights reserved.
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

import ch.cyberduck.core.DeserializerFactory;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.transfer.CopyTransfer;
import ch.cyberduck.core.transfer.DownloadTransfer;
import ch.cyberduck.core.transfer.SyncTransfer;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferItem;
import ch.cyberduck.core.transfer.UploadTransfer;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version $Id$
 */
public class TransferDictionary {
    private static final Logger log = Logger.getLogger(TransferDictionary.class);

    public <T> Transfer deserialize(final T serialized) {
        final Deserializer dict = DeserializerFactory.createDeserializer(serialized);
        final Object hostObj = dict.objectForKey("Host");
        if(null == hostObj) {
            log.warn("Missing host in transfer");
            return null;
        }
        final Host host = new HostDictionary().deserialize(hostObj);
        final List<T> itemsObj = dict.listForKey("Items");
        final List<TransferItem> roots = new ArrayList<TransferItem>();
        if(itemsObj != null) {
            for(T rootDict : itemsObj) {
                final TransferItem item = new TransferItemDictionary().deserialize(rootDict);
                if(null == item) {
                    log.warn("Invalid item in transfer");
                    continue;
                }
                roots.add(item);
            }
        }
        // Legacy
        final List<T> rootsObj = dict.listForKey("Roots");
        if(rootsObj != null) {
            for(T rootDict : rootsObj) {
                final Path remote = new PathDictionary().deserialize(rootDict);
                if(null == remote) {
                    log.warn("Invalid remote in transfer");
                    continue;
                }
                final TransferItem item = new TransferItem(remote);
                // Legacy
                final String localObjDeprecated
                        = DeserializerFactory.createDeserializer(serialized).stringForKey("Local");
                if(localObjDeprecated != null) {
                    Local local = LocalFactory.createLocal(localObjDeprecated);
                    item.setLocal(local);
                }
                final Object localObj
                        = DeserializerFactory.createDeserializer(serialized).objectForKey("Local Dictionary");
                if(localObj != null) {
                    Local local = new LocalDictionary().deserialize(localObj);
                    if(null == local) {
                        log.warn("Invalid local in transfer item");
                        continue;
                    }
                    item.setLocal(local);
                }
                roots.add(item);
            }
        }
        if(roots.isEmpty()) {
            log.warn("No files in transfer");
            return null;
        }
        final Transfer transfer;
        String kindObj = dict.stringForKey("Kind");
        if(kindObj == null) {
            log.warn("Missing transfer type");
            return null;
        }
        switch(Transfer.Type.values()[Integer.parseInt(kindObj)]) {
            case download:
                transfer = new DownloadTransfer(host, roots);
                break;
            case upload:
                transfer = new UploadTransfer(host, roots);
                break;
            case sync:
                transfer = new SyncTransfer(host, roots.iterator().next());
                break;
            case copy:
                Object destinationObj = dict.objectForKey("Destination");
                if(null == destinationObj) {
                    log.warn("Missing destination for copy transfer");
                    return null;
                }
                final List<T> destinations = dict.listForKey("Destinations");
                if(destinations.isEmpty()) {
                    log.warn("No destinations in copy transfer");
                    return null;
                }
                if(roots.size() == destinations.size()) {
                    final Map<Path, Path> files = new HashMap<Path, Path>();
                    for(int i = 0; i < roots.size(); i++) {
                        files.put(roots.get(i).remote, new PathDictionary().deserialize(destinations.get(i)));
                    }
                    final Host target = new HostDictionary().deserialize(destinationObj);
                    if(null == target) {
                        log.warn("Missing target host in copy transfer");
                        return null;
                    }
                    transfer = new CopyTransfer(host, target, files);
                }
                else {
                    log.warn("Invalid file mapping for copy transfer");
                    return null;
                }
                break;
            default:
                log.warn(String.format("Unknown transfer type %s", kindObj));
                return null;
        }
        switch(Transfer.Type.values()[Integer.parseInt(kindObj)]) {
            case download:
            case upload:
            case sync:
                // Verify we have valid items
                for(TransferItem item : roots) {
                    if(null == item.remote) {
                        log.warn(String.format("Missing remote in transfer item %s", item));
                        return null;
                    }
                    if(null == item.local) {
                        log.warn(String.format("Missing local in transfer item %s", item));
                        return null;
                    }
                }
        }
        Object sizeObj = dict.stringForKey("Size");
        if(sizeObj != null) {
            transfer.setSize((long) Double.parseDouble(sizeObj.toString()));
        }
        Object timestampObj = dict.stringForKey("Timestamp");
        if(timestampObj != null) {
            transfer.setTimestamp(new Date(Long.parseLong(timestampObj.toString())));
        }
        Object currentObj = dict.stringForKey("Current");
        if(currentObj != null) {
            transfer.setTransferred((long) Double.parseDouble(currentObj.toString()));
        }
        Object bandwidthObj = dict.stringForKey("Bandwidth");
        if(bandwidthObj != null) {
            transfer.getBandwidth().setRate(Float.parseFloat(bandwidthObj.toString()));
        }
        return transfer;
    }
}

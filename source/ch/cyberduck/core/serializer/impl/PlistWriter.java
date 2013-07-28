package ch.cyberduck.core.serializer.impl;

/*
 * Copyright (c) 2009 David Kocher. All rights reserved.
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

import ch.cyberduck.core.Factory;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostWriterFactory;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.ProfileWriterFactory;
import ch.cyberduck.core.Serializable;
import ch.cyberduck.core.SerializerFactory;
import ch.cyberduck.core.TransferWriterFactory;
import ch.cyberduck.core.serializer.Writer;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.ui.cocoa.foundation.NSDictionary;
import ch.cyberduck.ui.cocoa.foundation.NSMutableArray;

import java.util.Collection;

/**
 * @version $Id$
 */
public class PlistWriter<S extends Serializable> implements Writer<S> {

    public static void register() {
        HostWriterFactory.addFactory(Factory.NATIVE_PLATFORM, new HostFactory());
        TransferWriterFactory.addFactory(Factory.NATIVE_PLATFORM, new TransferFactory());
    }

    private static final class HostFactory extends HostWriterFactory {
        @Override
        public Writer<Host> create() {
            return new PlistWriter<Host>();
        }
    }

    private static final class TransferFactory extends TransferWriterFactory {
        @Override
        public Writer<Transfer> create() {
            return new PlistWriter<Transfer>();
        }
    }

    private static final class ProtocolFactory extends ProfileWriterFactory {
        @Override
        public Writer<Profile> create() {
            return new PlistWriter<Profile>();
        }
    }

    @Override
    public void write(Collection<S> collection, Local file) {
        NSMutableArray list = NSMutableArray.array();
        for(S bookmark : collection) {
            list.addObject(bookmark.<NSDictionary>serialize(SerializerFactory.get()));
        }
        list.writeToFile(file.getAbsolute());
    }

    @Override
    public void write(S item, Local file) {
        item.<NSDictionary>serialize(SerializerFactory.get()).writeToFile(file.getAbsolute());
    }
}
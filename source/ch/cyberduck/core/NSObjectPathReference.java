package ch.cyberduck.core;

/*
 * Copyright (c) 2002-2009 David Kocher. All rights reserved.
 *
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

import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.foundation.NSString;

import org.apache.commons.collections.map.LRUMap;

import java.util.Collections;
import java.util.Map;

/**
 * Mapper between path references returned from the outline view model and its internal
 * string representation.
 *
 * @version $Id$
 */
public class NSObjectPathReference implements PathReference<NSObject> {

    private static class Factory extends PathReferenceFactory {
        @Override
        protected PathReference create() {
            throw new FactoryException();
        }

        @Override
        protected <T> PathReference<T> create(Path param) {
            return (PathReference<T>) new NSObjectPathReference(param);
        }
    }

    public static void register() {
        PathReferenceFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private NSObject reference;

    private int hashcode;

    private static Map<String, NSString> cache = Collections.synchronizedMap(new LRUMap(
            Preferences.instance().getInteger("browser.model.cache.size")
    ));


    protected NSObjectPathReference(final Path path) {
        // Unique name
        final String name = new DefaultPathReference(path).unique();
        if(!cache.containsKey(name)) {
            cache.put(name, NSString.stringWithString(name));
        }
        this.reference = cache.get(name);
        this.hashcode = name.hashCode();
    }

    public NSObjectPathReference(NSObject reference) {
        this.reference = reference;
        this.hashcode = reference.toString().hashCode();
    }

    @Override
    public NSObject unique() {
        return reference;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final NSObjectPathReference that = (NSObjectPathReference) o;
        if(hashcode != that.hashcode) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NSObjectPathReference{");
        sb.append("reference=").append(reference);
        sb.append(", hashcode=").append(hashcode);
        sb.append('}');
        return sb.toString();
    }
}
package ch.cyberduck.core.local;

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

import ch.cyberduck.core.LocalAttributes;
import ch.cyberduck.core.Permission;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.ui.cocoa.application.NSWorkspace;
import ch.cyberduck.ui.cocoa.foundation.NSArray;
import ch.cyberduck.ui.cocoa.foundation.NSDate;
import ch.cyberduck.ui.cocoa.foundation.NSDictionary;
import ch.cyberduck.ui.cocoa.foundation.NSFileManager;
import ch.cyberduck.ui.cocoa.foundation.NSNumber;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.foundation.NSURL;

import org.apache.log4j.Logger;
import org.rococoa.ObjCObjectByReference;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSError;

/**
 * Extending attributes with <code>NSFileManager</code>.
 *
 * @see ch.cyberduck.ui.cocoa.foundation.NSFileManager
 */
public class FinderLocalAttributes extends LocalAttributes {
    private static final Logger log = Logger.getLogger(FinderLocalAttributes.class);

    private FinderLocal local;

    public FinderLocalAttributes(final FinderLocal local) {
        super(local.getAbsolute());
        this.local = local;
    }

    /**
     * @return Null if no such file.
     */
    private NSDictionary getNativeAttributes() throws AccessDeniedException, NotfoundException {
        if((!local.exists())) {
            throw new NotfoundException(local.getAbsolute());
        }
        final ObjCObjectByReference error = new ObjCObjectByReference();
        // If flag is true and path is a symbolic link, the attributes of the linked-to file are returned;
        // if the link points to a nonexistent file, this method returns null. If flag is false,
        // the attributes of the symbolic link are returned.
        final NSDictionary dict = NSFileManager.defaultManager().attributesOfItemAtPath_error(
                local.getAbsolute(), error);
        if(null == dict) {
            final NSError f = error.getValueAs(NSError.class);
            throw new AccessDeniedException(String.format("%s", f));
        }
        return dict;
    }

    /**
     * @param name File manager attribute name
     * @return Null if no such file or attribute.
     */
    private NSObject getNativeAttribute(final String name) throws AccessDeniedException, NotfoundException {
        final NSDictionary dict = this.getNativeAttributes();
        // Returns an entry’s value given its key, or null if no value is associated with key.
        return dict.objectForKey(name);
    }

    @Override
    public long getSize() {
        try {
            final NSObject object = this.getNativeAttribute(NSFileManager.NSFileSize);
            // Refer to #5503 and http://code.google.com/p/rococoa/issues/detail?id=3
            return (long) Rococoa.cast(object, NSNumber.class).doubleValue();
        }
        catch(AccessDeniedException e) {
            return -1;
        }
        catch(NotfoundException e) {
            return -1;
        }
    }

    @Override
    public Permission getPermission() {
        try {
            try {
                final NSObject object = this.getNativeAttribute(NSFileManager.NSFilePosixPermissions);
                String posixString = Integer.toOctalString(Rococoa.cast(object, NSNumber.class).intValue());
                return new FinderLocalPermission(Integer.parseInt(posixString.substring(posixString.length() - 3)));
            }
            catch(AccessDeniedException e) {
                return Permission.EMPTY;
            }
            catch(NotfoundException e) {
                return Permission.EMPTY;
            }
        }
        catch(NumberFormatException e) {
            log.error(e.getMessage());
        }
        return Permission.EMPTY;
    }

    @Override
    public void setPermission(final Permission permission) throws AccessDeniedException {
        synchronized(NSWorkspace.class) {
            final ObjCObjectByReference error = new ObjCObjectByReference();
            boolean success = NSFileManager.defaultManager().setAttributes_ofItemAtPath_error(
                    NSDictionary.dictionaryWithObjectsForKeys(
                            NSArray.arrayWithObject(NSNumber.numberWithInt(Integer.valueOf(permission.getMode(), 8))),
                            NSArray.arrayWithObject(NSFileManager.NSFilePosixPermissions)),
                    local.getAbsolute(), error
            );
            if(!success) {
                final NSError f = error.getValueAs(NSError.class);
                throw new AccessDeniedException(String.format("%s", f));
            }
        }
    }

    /**
     * Write <code>NSFileModificationDate</code>.
     *
     * @param modified Milliseconds
     */
    @Override
    public void setModificationDate(final long modified) throws AccessDeniedException {
        synchronized(NSWorkspace.class) {
            final ObjCObjectByReference error = new ObjCObjectByReference();
            boolean success = NSFileManager.defaultManager().setAttributes_ofItemAtPath_error(
                    NSDictionary.dictionaryWithObjectsForKeys(
                            NSArray.arrayWithObject(NSDate.dateWithTimeIntervalSince1970(modified / 1000d)),
                            NSArray.arrayWithObject(NSFileManager.NSFileModificationDate)),
                    local.getAbsolute(), error
            );
            if(!success) {
                final NSError f = error.getValueAs(NSError.class);
                throw new AccessDeniedException(String.format("%s", f));
            }
        }
    }

    /**
     * Read <code>NSFileCreationDate</code>.
     *
     * @return Milliseconds since 1970
     */
    @Override
    public long getCreationDate() {
        try {
            final NSObject object = this.getNativeAttribute(NSFileManager.NSFileCreationDate);
            return (long) (Rococoa.cast(object, NSDate.class).timeIntervalSince1970() * 1000);
        }
        catch(AccessDeniedException e) {
            return -1;
        }
        catch(NotfoundException e) {
            return -1;
        }
    }

    @Override
    public String getOwner() {
        try {
            final NSObject object = this.getNativeAttribute(NSFileManager.NSFileOwnerAccountName);
            return object.toString();
        }
        catch(AccessDeniedException e) {
            return null;
        }
        catch(NotfoundException e) {
            return null;
        }
    }

    @Override
    public String getGroup() {
        try {
            final NSObject object = this.getNativeAttribute(NSFileManager.NSFileGroupOwnerAccountName);
            return object.toString();
        }
        catch(AccessDeniedException e) {
            return null;
        }
        catch(NotfoundException e) {
            return null;
        }
    }

    /**
     * @return The value for the key NSFileSystemFileNumber, or 0 if the receiver doesn’t have an entry for the key
     */
    public Long getInode() {
        try {
            final NSObject object = this.getNativeAttribute(NSFileManager.NSFileSystemFileNumber);
            final NSNumber number = Rococoa.cast(object, NSNumber.class);
            return number.longValue();
        }
        catch(AccessDeniedException e) {
            return null;
        }
        catch(NotfoundException e) {
            return null;
        }
    }

    @Override
    public boolean isBundle() {
        return NSWorkspace.sharedWorkspace().isFilePackageAtPath(local.getAbsolute());
    }

    public boolean isSymbolicLink() {
        try {
            final NSObject object = this.getNativeAttribute(NSFileManager.NSFileType);
            return NSFileManager.NSFileTypeSymbolicLink.equals(object.toString());
        }
        catch(AccessDeniedException e) {
            return false;
        }
        catch(NotfoundException e) {
            return false;
        }
    }

    /**
     * Executable, readable and writable flags based on <code>NSFileManager</code>.
     */
    private final class FinderLocalPermission extends LocalPermission {
        private FinderLocalPermission() {
            //
        }

        private FinderLocalPermission(final String mode) {
            super(mode);
        }

        private FinderLocalPermission(final int mode) {
            super(mode);
        }

        @Override
        public boolean isExecutable() {
            final NSURL resolved;
            try {
                resolved = local.lock();
            }
            catch(AccessDeniedException e) {
                return super.isExecutable();
            }
            try {
                return NSFileManager.defaultManager().isExecutableFileAtPath(resolved.path());
            }
            finally {
                local.release(resolved);
            }
        }

        @Override
        public boolean isReadable() {
            final NSURL resolved;
            try {
                resolved = local.lock();
            }
            catch(AccessDeniedException e) {
                return super.isReadable();
            }
            try {
                return NSFileManager.defaultManager().isReadableFileAtPath(resolved.path());
            }
            finally {
                local.release(resolved);
            }
        }

        @Override
        public boolean isWritable() {
            final NSURL resolved;
            try {
                resolved = local.lock();
            }
            catch(AccessDeniedException e) {
                return super.isWritable();
            }
            try {
                return NSFileManager.defaultManager().isWritableFileAtPath(resolved.path());
            }
            finally {
                local.release(resolved);
            }
        }
    }
}
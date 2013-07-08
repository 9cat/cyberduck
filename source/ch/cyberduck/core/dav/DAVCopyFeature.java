package ch.cyberduck.core.dav;

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

import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.DefaultIOExceptionMappingService;
import ch.cyberduck.core.exception.SardineExceptionMappingService;
import ch.cyberduck.core.features.Copy;

import java.io.IOException;

import com.googlecode.sardine.impl.SardineException;

/**
 * @version $Id$
 */
public class DAVCopyFeature implements Copy {

    private DAVSession session;

    public DAVCopyFeature(final DAVSession session) {
        this.session = session;
    }

    @Override
    public void copy(final Path source, final Path copy) throws BackgroundException {
        try {
            if(source.attributes().isFile()) {
                session.getClient().copy(new DAVPathEncoder().encode(source), session.toURL(copy, false));
            }
        }
        catch(SardineException e) {
            throw new SardineExceptionMappingService().map("Cannot copy {0}", e, source);
        }
        catch(IOException e) {
            throw new DefaultIOExceptionMappingService().map(e, source);
        }
    }
}

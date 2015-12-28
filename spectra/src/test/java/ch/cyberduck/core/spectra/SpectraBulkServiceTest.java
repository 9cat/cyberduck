/*
 * Copyright (c) 2015-2016 Spectra Logic Corporation. All rights reserved.
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
 */

package ch.cyberduck.core.spectra;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledTranscriptListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferStatus;

import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

public class SpectraBulkServiceTest extends AbstractTestCase {

    @Test
    public void testPreUpload() throws Exception {
        final Host host = new Host(new SpectraProtocol(), properties.getProperty("spectra.hostname"), 8080, new Credentials(
                properties.getProperty("spectra.user"), properties.getProperty("spectra.key")
        ));
        final SpectraSession session = new SpectraSession(host, new DisabledX509TrustManager(),
                new DefaultX509KeyManager());
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        new SpectraBulkService(session).pre(Transfer.Type.upload, Collections.singletonMap(
                new Path(String.format("/cyberduck/%s", UUID.randomUUID().toString()), EnumSet.of(Path.Type.directory, Path.Type.volume)), new TransferStatus().length(1L)
        ));
        session.close();
    }

    @Test(expected = NotfoundException.class)
    public void testPreDownload() throws Exception {
        final Host host = new Host(new SpectraProtocol(), properties.getProperty("spectra.hostname"), 8080, new Credentials(
                properties.getProperty("spectra.user"), properties.getProperty("spectra.key")
        ));
        final SpectraSession session = new SpectraSession(host, new DisabledX509TrustManager(),
                new DefaultX509KeyManager());
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        new SpectraBulkService(session).pre(Transfer.Type.download, Collections.singletonMap(
                new Path(String.format("/cyberduck/%s", UUID.randomUUID().toString()), EnumSet.of(Path.Type.directory, Path.Type.volume)), new TransferStatus().length(1L)
        ));
        session.close();
    }
}
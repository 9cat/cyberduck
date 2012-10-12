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

import ch.cyberduck.core.aquaticprime.Donation;
import ch.cyberduck.core.local.FinderLocal;
import ch.cyberduck.core.threading.AutoreleaseActionOperationBatcher;
import ch.cyberduck.ui.cocoa.UserDefaultsDateFormatter;
import ch.cyberduck.ui.cocoa.UserDefaultsPreferences;
import ch.cyberduck.ui.cocoa.foundation.NSAutoreleasePool;
import ch.cyberduck.ui.cocoa.i18n.BundleLocale;
import ch.cyberduck.ui.cocoa.serializer.HostPlistReader;
import ch.cyberduck.ui.cocoa.serializer.PlistDeserializer;
import ch.cyberduck.ui.cocoa.serializer.PlistSerializer;
import ch.cyberduck.ui.cocoa.serializer.PlistWriter;
import ch.cyberduck.ui.cocoa.serializer.ProtocolPlistReader;
import ch.cyberduck.ui.cocoa.serializer.TransferPlistReader;
import ch.cyberduck.ui.growl.GrowlNative;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @version $Id$
 */
public class AbstractTestCase {

    static {
        BasicConfigurator.configure();
    }

    NSAutoreleasePool pool;

    @Before
    public void register() {
        pool = NSAutoreleasePool.push();

        AutoreleaseActionOperationBatcher.register();
        FinderLocal.register();
        UserDefaultsPreferences.register();
        BundleLocale.register();
        GrowlNative.register();
        Donation.register();

        PlistDeserializer.register();
        PlistSerializer.register();

        HostPlistReader.register();
        TransferPlistReader.register();
        ProtocolPlistReader.register();
        NSObjectPathReference.register();

        PlistWriter.register();

        UserDefaultsDateFormatter.register();

        ProtocolFactory.register();

        Preferences.instance().setProperty("growl.enable", false);
        Preferences.instance().setProperty("application.support.path", ".");
    }

    @After
    public void post() {
        pool.drain();
    }


    protected void repeat(final Callable<Local> c, int repeat) throws InterruptedException, ExecutionException {
        final ExecutorService service = Executors.newCachedThreadPool();
        final BlockingQueue<Future<Local>> queue = new LinkedBlockingQueue<Future<Local>>();
        final CompletionService<Local> completion = new ExecutorCompletionService<Local>(service, queue);
        for(int i = 0; i < repeat; i++) {
            completion.submit(new Callable<Local>() {
                @Override
                public Local call() throws Exception {
                    final NSAutoreleasePool p = NSAutoreleasePool.push();
                    try {
                        return c.call();
                    }
                    finally {
                        p.drain();
                    }
                }
            });
        }
        for(int i = 0; i < repeat; i++) {
            queue.take().get();
        }
    }
}
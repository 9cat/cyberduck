package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2007 David Kocher. All rights reserved.
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

import ch.cyberduck.core.*;
import ch.cyberduck.core.exception.ChecksumException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.io.MD5ChecksumCompute;
import ch.cyberduck.core.sftp.openssh.OpenSSHHostKeyVerifier;
import ch.cyberduck.ui.Controller;
import ch.cyberduck.ui.HostKeyControllerFactory;
import ch.cyberduck.ui.cocoa.application.NSAlert;
import ch.cyberduck.ui.cocoa.application.NSCell;

import org.apache.log4j.Logger;

import java.security.PublicKey;
import java.text.MessageFormat;

import net.schmizz.sshj.common.KeyType;

/**
 * Using known_hosts from OpenSSH to store accepted host keys.
 *
 * @version $Id$
 */
public class AlertHostKeyController extends OpenSSHHostKeyVerifier {
    private static final Logger log = Logger.getLogger(AlertHostKeyController.class);

    public static void register() {
        HostKeyControllerFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private static class Factory extends HostKeyControllerFactory {
        @Override
        protected HostKeyCallback create() {
            throw new FactoryException();
        }

        @Override
        public HostKeyCallback create(final Controller c, final Protocol protocol) {
            if(Scheme.sftp.equals(protocol.getScheme())) {
                return new AlertHostKeyController((WindowController) c);
            }
            return new DisabledHostKeyCallback();
        }
    }

    private WindowController parent;

    public AlertHostKeyController(final WindowController c) {
        this(c, LocalFactory.createLocal(Preferences.instance().getProperty("ssh.knownhosts")).withBookmark(
                Preferences.instance().getProperty("ssh.knownhosts.bookmark")
        ));
    }

    public AlertHostKeyController(final WindowController parent, final Local file) {
        super(file);
        this.parent = parent;
    }

    @Override
    protected boolean isUnknownKeyAccepted(final String hostname, final PublicKey key)
            throws ConnectionCanceledException, ChecksumException {
        final NSAlert alert = NSAlert.alert(MessageFormat.format(LocaleFactory.localizedString("Unknown fingerprint", "Sftp"), hostname), //title
                MessageFormat.format(LocaleFactory.localizedString("The fingerprint for the {1} key sent by the server is {0}.", "Sftp"),
                        new MD5ChecksumCompute().fingerprint(key),
                        KeyType.fromKey(key).name()),
                LocaleFactory.localizedString("Allow"), // default button
                LocaleFactory.localizedString("Deny"), // alternate button
                null //other button
        );
        alert.setShowsSuppressionButton(true);
        alert.suppressionButton().setTitle(LocaleFactory.localizedString("Always"));
        alert.setShowsHelp(true);
        SheetController c = new AlertController(parent, alert) {
            @Override
            public void callback(final int returncode) {
                if(returncode == DEFAULT_OPTION) {// allow host (once)
                    allow(hostname, key,
                            alert.suppressionButton().state() == NSCell.NSOnState);
                }
                else {
                    log.warn("Cannot continue without a valid host key");
                }
            }

            @Override
            protected void help() {
                new DefaultProviderHelpService().help(Scheme.sftp);
            }
        };
        c.beginSheet();
        if(c.returnCode() == SheetCallback.ALTERNATE_OPTION) {
            throw new ConnectionCanceledException();
        }
        return c.returnCode() == SheetCallback.DEFAULT_OPTION;

    }

    @Override
    protected boolean isChangedKeyAccepted(final String hostname, final PublicKey key)
            throws ConnectionCanceledException, ChecksumException {
        NSAlert alert = NSAlert.alert(MessageFormat.format(LocaleFactory.localizedString("Changed fingerprint", "Sftp"), hostname), //title
                MessageFormat.format(LocaleFactory.localizedString("The fingerprint for the {1} key sent by the server is {0}.", "Sftp"),
                        new MD5ChecksumCompute().fingerprint(key),
                        KeyType.fromKey(key).name()),
                LocaleFactory.localizedString("Allow"), // defaultbutton
                LocaleFactory.localizedString("Deny"), //alternative button
                null //other button
        );
        alert.setShowsSuppressionButton(true);
        alert.suppressionButton().setTitle(LocaleFactory.localizedString("Always"));
        alert.setShowsHelp(true);
        SheetController c = new AlertController(parent, alert) {
            @Override
            public void callback(final int returncode) {
                if(returncode == DEFAULT_OPTION) {
                    allow(hostname, key,
                            alert.suppressionButton().state() == NSCell.NSOnState);
                }
                else {
                    log.warn("Cannot continue without a valid host key");
                }
            }

            @Override
            protected void help() {
                new DefaultProviderHelpService().help(Scheme.sftp);
            }
        };
        c.beginSheet();
        if(c.returnCode() == SheetCallback.ALTERNATE_OPTION) {
            throw new ConnectionCanceledException();
        }
        return c.returnCode() == SheetCallback.DEFAULT_OPTION;
    }
}

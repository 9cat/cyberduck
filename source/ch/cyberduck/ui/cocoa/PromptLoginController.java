package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
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
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.ui.Controller;
import ch.cyberduck.ui.LoginControllerFactory;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.foundation.NSAttributedString;
import ch.cyberduck.ui.cocoa.foundation.NSNotification;
import ch.cyberduck.ui.cocoa.foundation.NSNotificationCenter;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.resources.IconCacheFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.Foundation;

/**
 * @version $Id$
 */
public final class PromptLoginController implements LoginCallback {
    private static final Logger log = Logger.getLogger(PromptLoginController.class);

    public static void register() {
        LoginControllerFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private static class Factory extends LoginControllerFactory {
        @Override
        protected LoginCallback create() {
            throw new FactoryException();
        }

        @Override
        public LoginCallback create(Controller c) {
            return new PromptLoginController((WindowController) c);
        }
    }

    private WindowController parent;

    private PromptLoginController(final WindowController parent) {
        this.parent = parent;
    }

    @Override
    public void warn(final Protocol protocol, final String title, final String message,
                     final String continueButton, final String disconnectButton, final String preference)
            throws LoginCanceledException {
        if(log.isDebugEnabled()) {
            log.debug("Display insecure connection alert");
        }
        final NSAlert alert = NSAlert.alert(title, message,
                continueButton, // Default Button
                null, // Alternate button
                disconnectButton // Other
        );
        alert.setShowsHelp(true);
        alert.setShowsSuppressionButton(true);
        alert.suppressionButton().setTitle(LocaleFactory.localizedString("Don't show again", "Credentials"));
        alert.setAlertStyle(NSAlert.NSWarningAlertStyle);
        final StringBuilder site = new StringBuilder(Preferences.instance().getProperty("website.help"));
        site.append("/").append(protocol.getScheme().name());
        int option = parent.alert(alert, site.toString());
        if(alert.suppressionButton().state() == NSCell.NSOnState) {
            // Never show again.
            Preferences.instance().setProperty(preference, true);
        }
        switch(option) {
            case SheetCallback.CANCEL_OPTION:
                throw new LoginCanceledException();
        }
        //Proceed nevertheless.
    }

    @Override
    public void prompt(final Protocol protocol, final Credentials credentials,
                       final String title, final String reason,
                       final LoginOptions options) throws LoginCanceledException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Prompt for credentials for %s", protocol));
        }
        final SheetController sheet = new SheetController(parent) {
            @Override
            protected String getBundleName() {
                return "Login";
            }

            @Override
            public void awakeFromNib() {
                this.update();
                this.window().makeFirstResponder(this.usernameField);
                super.awakeFromNib();
            }

            @Override
            public void helpButtonClicked(NSButton sender) {
                new DefaultProviderHelpService().help(protocol);
            }

            @Outlet
            protected NSImageView iconView;

            public void setIconView(NSImageView iconView) {
                this.iconView = iconView;
                this.iconView.setImage(IconCacheFactory.<NSImage>get().iconNamed(protocol.disk()));
            }

            @Outlet
            private NSTextField usernameLabel;

            public void setUsernameLabel(NSTextField usernameLabel) {
                this.usernameLabel = usernameLabel;
                this.usernameLabel.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                        StringUtils.isNotBlank(credentials.getUsernamePlaceholder()) ? String.format("%s:", credentials.getUsernamePlaceholder()) : StringUtils.EMPTY,
                        TRUNCATE_MIDDLE_ATTRIBUTES));
            }

            @Outlet
            private NSTextField passwordLabel;

            public void setPasswordLabel(NSTextField passwordLabel) {
                this.passwordLabel = passwordLabel;
                this.passwordLabel.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                        StringUtils.isNotBlank(credentials.getPasswordPlaceholder()) ? String.format("%s:", credentials.getPasswordPlaceholder()) : StringUtils.EMPTY,
                        TRUNCATE_MIDDLE_ATTRIBUTES));
            }

            @Outlet
            private NSTextField titleField;

            public void setTitleField(NSTextField titleField) {
                this.titleField = titleField;
                this.updateField(this.titleField, LocaleFactory.localizedString(title, "Credentials"));
            }

            @Outlet
            private NSTextField usernameField;

            public void setUsernameField(NSTextField usernameField) {
                this.usernameField = usernameField;
                this.updateField(this.usernameField, credentials.getUsername());
                this.usernameField.cell().setPlaceholderString(credentials.getUsernamePlaceholder());
                NSNotificationCenter.defaultCenter().addObserver(this.id(),
                        Foundation.selector("userFieldTextDidChange:"),
                        NSControl.NSControlTextDidChangeNotification,
                        this.usernameField);
            }

            public void userFieldTextDidChange(NSNotification notification) {
                credentials.setUsername(usernameField.stringValue());
                if(StringUtils.isNotBlank(credentials.getUsername())) {
                    String password = PasswordStoreFactory.get().getPassword(protocol.getScheme(), protocol.getDefaultPort(),
                            protocol.getDefaultHostname(), credentials.getUsername());
                    if(StringUtils.isNotBlank(password)) {
                        passwordField.setStringValue(password);
                        this.passFieldTextDidChange(notification);
                    }
                }
                this.update();
            }

            @Outlet
            private NSTextField textField;

            public void setTextField(NSTextField textField) {
                this.textField = textField;
                this.textField.setSelectable(true);
                if(reason.startsWith(Scheme.http.name())) {
                    // For OAuth2
                    this.textField.setAttributedStringValue(HyperlinkAttributedStringFactory.create(reason));
                    this.textField.setAllowsEditingTextAttributes(true);
                    this.textField.setSelectable(true);
                }
                else {
                    this.updateField(this.textField, new StringAppender().append(new StringBuilder(), reason).toString());
                }
            }

            @Outlet
            private NSSecureTextField passwordField;

            public void setPasswordField(NSSecureTextField passwordField) {
                this.passwordField = passwordField;
                this.updateField(this.passwordField, credentials.getPassword());
                this.passwordField.cell().setPlaceholderString(credentials.getPasswordPlaceholder());
                NSNotificationCenter.defaultCenter().addObserver(this.id(),
                        Foundation.selector("passFieldTextDidChange:"),
                        NSControl.NSControlTextDidChangeNotification,
                        this.passwordField);
            }

            public void passFieldTextDidChange(NSNotification notification) {
                credentials.setPassword(passwordField.stringValue());
            }

            @Outlet
            private NSButton keychainCheckbox;

            public void setKeychainCheckbox(NSButton keychainCheckbox) {
                this.keychainCheckbox = keychainCheckbox;
                this.keychainCheckbox.setTarget(this.id());
                this.keychainCheckbox.setAction(Foundation.selector("keychainCheckboxClicked:"));
                this.keychainCheckbox.setState(Preferences.instance().getBoolean("connection.login.useKeychain")
                        && Preferences.instance().getBoolean("connection.login.addKeychain") ? NSCell.NSOnState : NSCell.NSOffState);
            }

            public void keychainCheckboxClicked(final NSButton sender) {
                final boolean enabled = sender.state() == NSCell.NSOnState;
                Preferences.instance().setProperty("connection.login.addKeychain", enabled);
            }

            @Outlet
            private NSButton anonymousCheckbox;

            public void setAnonymousCheckbox(NSButton anonymousCheckbox) {
                this.anonymousCheckbox = anonymousCheckbox;
                this.anonymousCheckbox.setTarget(this.id());
                this.anonymousCheckbox.setAction(Foundation.selector("anonymousCheckboxClicked:"));
            }

            @Action
            public void anonymousCheckboxClicked(final NSButton sender) {
                if(sender.state() == NSCell.NSOnState) {
                    credentials.setUsername(Preferences.instance().getProperty("connection.login.anon.name"));
                    credentials.setPassword(Preferences.instance().getProperty("connection.login.anon.pass"));
                }
                if(sender.state() == NSCell.NSOffState) {
                    credentials.setUsername(Preferences.instance().getProperty("connection.login.name"));
                    credentials.setPassword(null);
                }
                this.updateField(this.usernameField, credentials.getUsername());
                this.updateField(this.passwordField, credentials.getPassword());
                this.update();
            }

            @Outlet
            private NSTextField pkLabel;

            public void setPkLabel(NSTextField pkLabel) {
                this.pkLabel = pkLabel;
            }

            @Outlet
            private NSButton pkCheckbox;

            public void setPkCheckbox(NSButton pkCheckbox) {
                this.pkCheckbox = pkCheckbox;
                this.pkCheckbox.setTarget(this.id());
                this.pkCheckbox.setAction(Foundation.selector("pkCheckboxSelectionChanged:"));
            }

            @Action
            public void pkCheckboxSelectionChanged(final NSButton sender) {
                if(sender.state() == NSCell.NSOnState) {
                    try {
                        select(this, new SheetCallback() {
                            @Override
                            public void callback(final int returncode) {
                                if(returncode == SheetCallback.DEFAULT_OPTION) {
                                    final NSObject selected = select.filenames().lastObject();
                                    if(selected != null) {
                                        credentials.setIdentity(LocalFactory.createLocal(selected.toString()));
                                        update();
                                    }
                                }
                            }
                        });
                    }
                    catch(LoginCanceledException e) {
                        //
                    }
                }
                else {
                    credentials.setIdentity(null);
                }
                update();
            }

            private void update() {
                this.usernameField.setEnabled(options.user && !credentials.isAnonymousLogin());
                this.passwordField.setEnabled(options.password && !credentials.isAnonymousLogin());
                {
                    boolean enable = options.keychain && !credentials.isAnonymousLogin();
                    this.keychainCheckbox.setEnabled(enable);
                    if(!enable) {
                        this.keychainCheckbox.setState(NSCell.NSOffState);
                    }
                }
                this.anonymousCheckbox.setEnabled(options.anonymous);
                if(options.anonymous && credentials.isAnonymousLogin()) {
                    this.anonymousCheckbox.setState(NSCell.NSOnState);
                }
                else {
                    this.anonymousCheckbox.setState(NSCell.NSOffState);
                }
                this.pkCheckbox.setEnabled(options.publickey);
                if(options.publickey && credentials.isPublicKeyAuthentication()) {
                    this.pkCheckbox.setState(NSCell.NSOnState);
                    this.updateField(this.pkLabel, credentials.getIdentity().getAbbreviatedPath());
                    this.pkLabel.setTextColor(NSColor.textColor());
                }
                else {
                    this.pkCheckbox.setState(NSCell.NSOffState);
                    this.pkLabel.setStringValue(LocaleFactory.localizedString("No private key selected"));
                    this.pkLabel.setTextColor(NSColor.disabledControlTextColor());
                }
            }

            @Override
            protected boolean validateInput() {
                credentials.setUsername(usernameField.stringValue());
                credentials.setPassword(passwordField.stringValue());
                return credentials.validate(protocol, options);
            }

            @Override
            public void callback(final int returncode) {
                if(returncode == SheetCallback.DEFAULT_OPTION) {
                    this.window().endEditingFor(null);
                    credentials.setSaved(keychainCheckbox.state() == NSCell.NSOnState);
                    credentials.setUsername(usernameField.stringValue());
                    credentials.setPassword(passwordField.stringValue());
                }
            }
        };
        sheet.beginSheet();
        if(sheet.returnCode() == SheetCallback.CANCEL_OPTION) {
            throw new LoginCanceledException();
        }
    }

    private NSOpenPanel select;

    public Local select() throws LoginCanceledException {
        return this.select(parent, new SheetCallback() {
            @Override
            public void callback(final int returncode) {
                //
            }
        });
    }

    protected Local select(final WindowController parent, final SheetCallback callback) throws LoginCanceledException {
        final SheetController sheet = new SheetController(parent) {
            @Override
            public void callback(final int returncode) {
                callback.callback(returncode);
            }

            @Override
            protected void beginSheetImpl() {
                select = NSOpenPanel.openPanel();
                select.setCanChooseDirectories(false);
                select.setCanChooseFiles(true);
                select.setAllowsMultipleSelection(false);
                select.setMessage(LocaleFactory.localizedString("Select the private key in PEM or PuTTY format", "Credentials"));
                select.setPrompt(LocaleFactory.localizedString("Choose"));
                select.beginSheetForDirectory(LocalFactory.createLocal("~/.ssh").getAbsolute(),
                        null, parent.window(), this.id(), Foundation.selector("sheetDidClose:returnCode:contextInfo:"), null);
            }

            @Override
            public NSWindow window() {
                return select;
            }
        };
        sheet.beginSheet();
        if(sheet.returnCode() == SheetCallback.DEFAULT_OPTION) {
            final NSObject selected = select.filenames().lastObject();
            if(selected != null) {
                return LocalFactory.createLocal(selected.toString());
            }
        }
        throw new LoginCanceledException();
    }
}

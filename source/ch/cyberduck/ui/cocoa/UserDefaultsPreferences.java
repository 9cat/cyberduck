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

import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.PreferencesFactory;
import ch.cyberduck.ui.cocoa.foundation.FoundationKitFunctions;
import ch.cyberduck.ui.cocoa.foundation.FoundationKitFunctionsLibrary;
import ch.cyberduck.ui.cocoa.foundation.NSArray;
import ch.cyberduck.ui.cocoa.foundation.NSBundle;
import ch.cyberduck.ui.cocoa.foundation.NSEnumerator;
import ch.cyberduck.ui.cocoa.foundation.NSLocale;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.foundation.NSString;
import ch.cyberduck.ui.cocoa.foundation.NSUserDefaults;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSInteger;
import org.rococoa.cocoa.foundation.NSUInteger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Concrete subclass using the Cocoa Preferences classes. The NSUserDefaults class is thread-safe.
 *
 * @version $Id$
 * @see ch.cyberduck.ui.cocoa.foundation.NSUserDefaults
 */
public class UserDefaultsPreferences extends Preferences {
    private static final Logger log = Logger.getLogger(Preferences.class);

    public static void register() {
        PreferencesFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private static class Factory extends PreferencesFactory {
        @Override
        protected Preferences create() {
            return new UserDefaultsPreferences();
        }
    }

    private NSUserDefaults store;

    /**
     * Additionally look for default values in Info.plist of application bundle.
     *
     * @param property The property to query.
     * @return A default value if any or null if not found.
     */
    @Override
    public String getDefault(final String property) {
        // Lookup in the default map
        final String value = super.getDefault(property);
        if(null == value) {
            // Missing in default. Lookup in Info.plist
            NSObject plist = NSBundle.mainBundle().infoDictionary().objectForKey(property);
            if(null == plist) {
                log.warn(String.format("No default value for property %s", property));
                return null;
            }
            return plist.toString();
        }
        // Default value of property found
        return value;
    }

    @Override
    public String getProperty(final String property) {
        final NSObject value = store.objectForKey(property);
        if(null == value) {
            return this.getDefault(property);
        }
        // Customized property found
        return value.toString();
    }

    @Override
    public void setProperty(final String property, final String value) {
        if(log.isInfoEnabled()) {
            log.info(String.format("Set property %s for key %s", value, property));
        }
        if(StringUtils.isNotEmpty(value)) {
            // Sets the value of the default identified by defaultName in the standard application domain.
            // Setting a default has no effect on the value returned by the objectForKey method if
            // the same key exists in a domain that precedes the application domain in the search list.
            store.setObjectForKey(NSString.stringWithString(value), property);
        }
        else {
            this.deleteProperty(property);
        }
    }

    @Override
    public void setProperty(final String property, final List<String> value) {
        // Sets the value of the default identified by defaultName in the standard application domain.
        // Setting a default has no effect on the value returned by the objectForKey method if
        // the same key exists in a domain that precedes the application domain in the search list.
        store.setObjectForKey(NSArray.arrayWithObjects(value.toArray(new String[value.size()])), property);
    }

    @Override
    public void deleteProperty(final String property) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Delete property %s", property));
        }
        store.removeObjectForKey(property);
    }

    /**
     * Overwrite the default values with user props if any.
     */
    @Override
    protected void load() {
        store = NSUserDefaults.standardUserDefaults();
    }

    /**
     * Properties that must be accessible in NSUserDefaults with default values
     */
    @Override
    protected void post() {
        _init("browser.view.autoexpand.useDelay");
        _init("browser.view.autoexpand.delay");

        _init("queue.maxtransfers");

        _init("connection.retry");
        _init("connection.retry.delay");
        _init("connection.timeout.seconds");

        _init("bookmark.icon.size");

        if(this.getBoolean("update.check")) {
            // Will override SUCheckAtStartup
            store.setInteger_forKey(new NSInteger(Long.parseLong(this.getProperty("update.check.interval"))),
                    "SUScheduledCheckInterval");
        }

        super.post();
    }

    @Override
    protected void setDefaults() {
        /**
         * The logging level (debug, info, warn, error)
         */
        defaults.put("logging.config", "log4j-cocoa.xml");
        // Parent defaults
        super.setDefaults();
        defaults.put("tmp.dir", FoundationKitFunctionsLibrary.NSTemporaryDirectory());
        final NSBundle bundle = NSBundle.mainBundle();
        if(null != bundle) {
            final NSObject bundleName = bundle.objectForInfoDictionaryKey("CFBundleName");
            final String applicationName;
            if(null != bundleName) {
                applicationName = bundleName.toString();
            }
            else {
                log.error("No CFBundleName property in main bundle info dictionary");
                applicationName = "Cyberduck";
            }
            final NSArray directories = FoundationKitFunctionsLibrary.NSSearchPathForDirectoriesInDomains(
                    FoundationKitFunctions.NSSearchPathDirectory.NSApplicationSupportDirectory,
                    FoundationKitFunctions.NSSearchPathDomainMask.NSUserDomainMask,
                    true);
            if(directories.count().intValue() == 0) {
                log.error("Failed searching for application support directory");
                defaults.put("application.support.path", LocalFactory.createLocal("~/Library/Application Support", applicationName).getAbbreviatedPath());
            }
            else {
                final String directory = directories.objectAtIndex(new NSUInteger(0)).toString();
                if(log.isInfoEnabled()) {
                    log.info(String.format("Found application support directory in %s", directory));
                }
                defaults.put("application.support.path", LocalFactory.createLocal(directory, applicationName).getAbbreviatedPath());
            }
            defaults.put("application.name", applicationName);
            defaults.put("application.version",
                    bundle.objectForInfoDictionaryKey("CFBundleShortVersionString").toString());
            defaults.put("application.receipt.path", bundle.bundlePath() + "/Contents/_MASReceipt");
            defaults.put("application.bookmarks.path", bundle.bundlePath() + "/Contents/Resources/Bookmarks");
            defaults.put("application.profiles.path", bundle.bundlePath() + "/Contents/Resources/Profiles");
        }

        defaults.put("update.feed.release", "https://version.cyberduck.io/changelog.rss");
        defaults.put("update.feed.beta", "https://version.cyberduck.io/beta/changelog.rss");
        defaults.put("update.feed.nightly", "https://version.cyberduck.io/nightly/changelog.rss");

        defaults.put("bookmark.import.filezilla.location", "~/.filezilla/sitemanager.xml");
        defaults.put("bookmark.import.fetch.location", "~/Library/Preferences/com.fetchsoftworks.Fetch.Shortcuts.plist");
        defaults.put("bookmark.import.flow.location", "~/Library/Application Support/Flow/Bookmarks.plist");
        defaults.put("bookmark.import.interarchy.location", "~/Library/Application Support/Interarchy/Bookmarks.plist");
        defaults.put("bookmark.import.transmit.location", "~/Library/Preferences/com.panic.Transmit.plist");
        defaults.put("bookmark.import.crossftp.location", "~/.crossftp/sites.xml");
        defaults.put("bookmark.import.fireftp.location", "~/Library/Application Support/Firefox/Profiles");
        if(LocalFactory.createLocal("~/Downloads").exists()) {
            // For 10.5 this usually exists and should be preferrred
            defaults.put("queue.download.folder", "~/Downloads");
        }
        else {
            defaults.put("queue.download.folder", "~/Desktop");
        }
        defaults.put("browser.filesize.decimal", String.valueOf(!Factory.VERSION_PLATFORM.matches("10\\.5.*")));

        defaults.put("connection.ssl.keystore.type", "KeychainStore");
        defaults.put("connection.ssl.keystore.provider", "Apple");
    }

    /**
     * Setting default values that must be accessible using [NSUserDefaults standardUserDefaults]
     *
     * @param property Initial property name to store default value for.
     */
    private void _init(final String property) {
        if(null == store.objectForKey(property)) {
            // Set the default value
            this.setProperty(property, this.getDefault(property));
        }
    }

    @Override
    public void save() {
        // Saves any modifications to the persistent domains and updates all
        // persistent domains that were not modified to  what is on disk.
        // Returns false if it could not save data to disk. Because synchronize
        // is automatically invoked at periodic intervals, use this method only
        // if you cannot wait for the automatic synchronization (for example, if
        // your application is about to exit) or if you want to update user props
        // to what is on disk even though you have not made any changes.
        store.synchronize();
    }

    @Override
    public String locale() {
        final NSObject value = store.objectForKey("AppleLanguages");
        if(null == value) {
            return super.locale();
        }
        final List<String> languages = this.toList(Rococoa.cast(value, NSArray.class));
        if(null != languages) {
            return languages.iterator().next();
        }
        return super.locale();
    }

    @Override
    public List<String> applicationLocales() {
//        return this.toList(NSBundle.mainBundle().preferredLocalizations());
        return this.toList(NSBundle.mainBundle().localizations());
    }

    @Override
    public List<String> systemLocales() {
        // Language ordering in system preferences. Can be overridden
        // using the "AppleLanguages" user default
        return this.toList(NSLocale.preferredLanguages());
    }

    /**
     * Convert collection
     *
     * @param list List of properties
     * @return Collection
     */
    private List<String> toList(final NSArray list) {
        if(null == list) {
            return Collections.emptyList();
        }
        final List<String> localizations = new ArrayList<String>();
        NSEnumerator ordered = list.objectEnumerator();
        NSObject next;
        while(((next = ordered.nextObject()) != null)) {
            localizations.add(next.toString());
        }
        return localizations;
    }
}

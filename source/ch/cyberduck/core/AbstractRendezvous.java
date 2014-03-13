package ch.cyberduck.core;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.TimedSemaphore;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRendezvous implements Rendezvous {
    private static final Logger log = Logger.getLogger(AbstractRendezvous.class);

    /**
     * sftp-ssh
     * Secure File Transfer Protocol over SSH
     * Bryan Cole <bryan.cole at teraview.com>
     * Protocol description: draft-ietf-secsh-filexfer-13.txt
     * Defined TXT keys: u=<username> p=<password> path=<path>
     */
    private static final String SERVICE_TYPE_SFTP = "_sftp-ssh._tcp.";

    /**
     * ftp
     * File Transfer
     * Service name originally allocated for Jon Postel <postel at isi.edu>
     * Now advertised and browsed-for by numerous independent
     * server and client implementations.
     * Protocol description: RFC 959
     * Defined TXT keys: u=<username> p=<password> path=<path>
     */
    private static final String SERVICE_TYPE_FTP = "_ftp._tcp.";

    /**
     * webdav
     * World Wide Web Distributed Authoring and Versioning (WebDAV)
     * Y. Y. Goland <yarong at microsoft.com>
     * Protocol description: RFC 2518
     * Defined TXT keys: u=<username> p=<password> path=<path>
     */
    private static final String SERVICE_TYPE_WEBDAV = "_webdav._tcp";

    /**
     * webdavs
     * WebDAV over SSL/TLS
     * Y. Y. Goland <yarong at microsoft.com>
     * Protocol description: RFC 2518
     * Defined TXT keys: u=<username> p=<password> path=<path>
     */
    private static final String SERVICE_TYPE_WEBDAV_TLS = "_webdavs._tcp";

    public String[] getServiceTypes() {
        return new String[]{
                SERVICE_TYPE_SFTP, SERVICE_TYPE_FTP, SERVICE_TYPE_WEBDAV, SERVICE_TYPE_WEBDAV_TLS
        };
    }

    private Map<String, Host> services = new ConcurrentHashMap<String, Host>();

    private Set<RendezvousListener> listeners =
            Collections.synchronizedSet(new HashSet<RendezvousListener>());

    private LimitedRendezvousListener notifier;

    @Override
    public void init() {
        notifier = new LimitedRendezvousListener();
    }

    @Override
    public void quit() {
        notifier.quit();
    }

    /**
     * Register a listener to be notified
     *
     * @param listener Callback
     */
    @Override
    public void addListener(RendezvousListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the listener from the notification queue
     *
     * @param listener Callback
     */
    @Override
    public void removeListener(RendezvousListener listener) {
        listeners.remove(listener);
    }

    /**
     * @return The number of services found; 0 <= services < n
     */
    @Override
    public int numberOfServices() {
        return services.size();
    }

    @Override
    public Host getService(int index) {
        return services.values().toArray(new Host[services.size()])[index];
    }

    @Override
    public Iterator<Host> iterator() {
        return services.values().iterator();
    }

    /**
     * @param index Index in collection
     * @return A nicely formatted informative string
     */
    @Override
    public String getDisplayedName(final int index) {
        if(index < this.numberOfServices()) {
            Host host = services.values().toArray(new Host[services.size()])[index];
            return host.getNickname();
        }
        return LocaleFactory.localizedString("Unknown");
    }

    /**
     * @param identifier The full service domain name
     * @return A nicely formatted informative string
     */
    @Override
    public String getDisplayedName(final String identifier) {
        Host host = services.get(identifier);
        if(null == host) {
            return LocaleFactory.localizedString("Unknown");
        }
        return host.getNickname();
    }

    protected void add(final String fullname, final String hostname, final int port,
                       final String user, final String password, final String path) {
        final Protocol protocol = this.getProtocol(fullname);
        if(null == protocol) {
            log.warn(String.format("Unknown service type for %s", fullname));
            return;
        }
        final Host host = new Host(protocol, hostname, port);
        host.getCredentials().setUsername(user);
        host.getCredentials().setPassword(password);
        if(StringUtils.isNotBlank(path)) {
            host.setDefaultPath(PathNormalizer.normalize(path));
        }
        host.setComment(UserDateFormatterFactory.get().getLongFormat(new Date().getTime()));
        this.add(fullname, host);
    }


    /**
     * @param fullname Service name
     * @return Null if no protocol can be found for the given Rendezvous service type.
     * @see "http://developer.apple.com/qa/qa2001/qa1312.html"
     */
    protected Protocol getProtocol(final String fullname) {
        if(fullname.contains(SERVICE_TYPE_SFTP)) {
            return ProtocolFactory.SFTP;
        }
        if(fullname.contains(SERVICE_TYPE_FTP)) {
            return ProtocolFactory.FTP;
        }
        if(fullname.contains(SERVICE_TYPE_WEBDAV)) {
            return ProtocolFactory.WEBDAV;
        }
        if(fullname.contains(SERVICE_TYPE_WEBDAV_TLS)) {
            return ProtocolFactory.WEBDAV_SSL;
        }
        log.warn(String.format("Cannot find service type in %s", fullname));
        return null;
    }

    /**
     * @param fullname Service name
     * @param host     Bookmark
     */
    protected void add(final String fullname, final Host host) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Add resolved host %s for full name %s", host, fullname));
        }
        if(null == services.put(fullname, host)) {
            notifier.serviceResolved(fullname, host);
        }
    }

    protected void remove(final String identifier) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Remove host with identifier %s", identifier));
        }
        final Host host = services.remove(identifier);
        if(null == host) {
            return;
        }
        notifier.serviceLost(host);
    }

    private final class LimitedRendezvousListener implements RendezvousListener {
        /**
         * Rate limit for notifications
         */
        private TimedSemaphore limit = new TimedSemaphore(
                1L, TimeUnit.MINUTES, Preferences.instance().getInteger("rendezvous.notification.limit"));

        public void quit() {
            limit.shutdown();
        }

        @Override
        public void serviceResolved(final String identifier, final Host host) {
            if(log.isInfoEnabled()) {
                log.info(String.format("Service resolved with identifier %s with %s", identifier, host));
            }
            if(Preferences.instance().getBoolean("rendezvous.loopback.supress")) {
                try {
                    if(InetAddress.getByName(host.getHostname()).equals(InetAddress.getLocalHost())) {
                        if(log.isInfoEnabled()) {
                            log.info(String.format("Supressed Rendezvous notification for %s", host));
                        }
                        return;
                    }
                }
                catch(UnknownHostException e) {
                    //Ignore
                }
            }
            if(this.acquire()) {
                for(RendezvousListener listener : listeners) {
                    listener.serviceResolved(identifier, host);
                }
            }
        }

        @Override
        public void serviceLost(final Host servicename) {
            if(log.isInfoEnabled()) {
                log.info(String.format("Service with name %s lost", servicename));
            }
            if(this.acquire()) {
                for(RendezvousListener listener : listeners) {
                    listener.serviceLost(servicename);
                }
            }
        }

        private boolean acquire() {
            if(limit.getAvailablePermits() > 0) {
                try {
                    limit.acquire();
                    return true;
                }
                catch(InterruptedException e) {
                    log.warn(String.format("Failure acquiring lock %s", e.getMessage()));
                }
            }
            return false;
        }
    }
}
package ch.cyberduck.core.dav;

import ch.cyberduck.core.*;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.ConnectionRefusedException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.features.Copy;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Headers;
import ch.cyberduck.core.features.Timestamp;
import ch.cyberduck.core.features.Touch;
import ch.cyberduck.core.features.UnixPermission;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.proxy.Proxy;
import ch.cyberduck.core.proxy.ProxyFinder;
import ch.cyberduck.core.proxy.ProxySocketFactory;
import ch.cyberduck.core.shared.DefaultHomeFinderService;
import ch.cyberduck.core.socket.DefaultSocketConfigurator;
import ch.cyberduck.core.ssl.CertificateStoreX509KeyManager;
import ch.cyberduck.core.ssl.CertificateStoreX509TrustManager;
import ch.cyberduck.core.ssl.CustomTrustSSLProtocolSocketFactory;
import ch.cyberduck.core.ssl.DefaultTrustManagerHostnameCallback;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DefaultX509TrustManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;
import ch.cyberduck.core.ssl.KeychainX509KeyManager;
import ch.cyberduck.core.ssl.TrustManagerHostnameCallback;

import org.junit.Ignore;
import org.junit.Test;

import javax.security.auth.x500.X500Principal;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class DAVSessionTest {

    @Test
    public void testConnect() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "svn.cyberduck.io", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host,
                new CertificateStoreX509TrustManager(new DefaultTrustManagerHostnameCallback(host), new DefaultCertificateStore()),
                new CertificateStoreX509KeyManager(new DefaultCertificateStore()));
        assertNotNull(session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener()));
        assertTrue(session.isConnected());
        assertNotNull(session.getClient());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        final AttributedList<Path> list = session.list(new Path("/", EnumSet.of(Path.Type.directory, Path.Type.volume)), new DisabledListProgressListener());
        assertNotNull(list.get(new Path("/trunk", EnumSet.of(Path.Type.directory))));
        assertNotNull(list.get(new Path("/branches", EnumSet.of(Path.Type.directory))));
        assertNotNull(list.get(new Path("/tags", EnumSet.of(Path.Type.directory))));
        assertTrue(session.isConnected());
        session.close();
        assertFalse(session.isConnected());
    }

    @Test(expected = ConnectionRefusedException.class)
    public void testConnectRefused() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "localhost", 2121);
        final DAVSession session = new DAVSession(host,
                new CertificateStoreX509TrustManager(new DefaultTrustManagerHostnameCallback(host), new DefaultCertificateStore()),
                new CertificateStoreX509KeyManager(new DefaultCertificateStore()));
        try {
            session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
            session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        }
        catch(ConnectionRefusedException e) {
            assertEquals("Connection failed", e.getMessage());
            throw e;
        }
    }

    @Test(expected = ConnectionRefusedException.class)
    public void testConnectHttpProxyFailure() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "svn.cyberduck.io", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host,
                new CertificateStoreX509TrustManager(new DefaultTrustManagerHostnameCallback(host), new DefaultCertificateStore()),
                new CertificateStoreX509KeyManager(new DefaultCertificateStore()),
                new ProxySocketFactory(host.getProtocol(), new DefaultTrustManagerHostnameCallback(host),
                        new DefaultSocketConfigurator(), new ProxyFinder() {
                    @Override
                    public Proxy find(final Host target) {
                        return new Proxy(Proxy.Type.HTTP, "localhost", 1111);
                    }
                })
        );
        final LoginConnectionService c = new LoginConnectionService(
                new DisabledLoginCallback(),
                new DisabledHostKeyCallback(),
                new DisabledPasswordStore(),
                new DisabledProgressListener(),
                new DisabledTranscriptListener());
        c.connect(session, PathCache.empty());
    }

    @Test
    @Ignore
    public void testConnectHttpProxy() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "svn.cyberduck.io", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host,
                new CertificateStoreX509TrustManager(new DefaultTrustManagerHostnameCallback(host), new DefaultCertificateStore()),
                new CertificateStoreX509KeyManager(new DefaultCertificateStore()),
                new ProxyFinder() {
                    @Override
                    public Proxy find(final Host target) {
                        return new Proxy(Proxy.Type.HTTP, "localhost", 3128);
                    }
                }
        );
        final AtomicBoolean proxied = new AtomicBoolean();
        final LoginConnectionService c = new LoginConnectionService(
                new DisabledLoginCallback(),
                new DisabledHostKeyCallback(),
                new DisabledPasswordStore(),
                new DisabledProgressListener(), new DisabledTranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                if(request) {
                    if(message.contains("CONNECT")) {
                        proxied.set(true);
                    }
                }
            }
        });
        c.connect(session, PathCache.empty());
        assertTrue(proxied.get());
        assertTrue(session.isConnected());
        session.close();
    }

    @Test(expected = ConnectionRefusedException.class)
    public void testConnectHttpProxyConnectionFailure() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "svn.cyberduck.io", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host,
                new CertificateStoreX509TrustManager(new DefaultTrustManagerHostnameCallback(host), new DefaultCertificateStore()),
                new CertificateStoreX509KeyManager(new DefaultCertificateStore()),
                new ProxyFinder() {
                    @Override
                    public Proxy find(final Host target) {
                        return new Proxy(Proxy.Type.HTTP, "localhost", 5555);
                    }
                }
        );
        final AtomicBoolean proxied = new AtomicBoolean();
        final LoginConnectionService c = new LoginConnectionService(
                new DisabledLoginCallback(),
                new DisabledHostKeyCallback(),
                new DisabledPasswordStore(),
                new DisabledProgressListener(), new DisabledTranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                if(request) {
                    if(message.contains("CONNECT")) {
                        proxied.set(true);
                    }
                }
            }
        });
        c.connect(session, PathCache.empty());
        assertFalse(proxied.get());
        assertFalse(session.isConnected());
        session.close();
    }

    @Test(expected = InteroperabilityException.class)
    public void testSsl() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "test.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host);
        assertFalse(session.alert(new DisabledConnectionCallback()));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        assertTrue(session.isSecured());
        try {
            session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        }
        catch(BackgroundException e) {
            assertEquals("Method Not Allowed. Please contact your web hosting service provider for assistance.", e.getDetail());
            throw e;
        }
    }

    @Test(expected = InteroperabilityException.class)
    public void testHtmlResponse() throws Exception {
        final Host host = new Host(new DAVProtocol(), "media.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        try {
            session.list(session.workdir(), new DisabledListProgressListener());
        }
        catch(InteroperabilityException e) {
            assertEquals("Not a valid DAV response.", e.getDetail());
            throw e;
        }
    }

    @Test(expected = LoginFailureException.class)
    public void testRedirect301() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/redir-perm");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        session.close();
    }

    @Test(expected = LoginFailureException.class)
    public void testRedirect302() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/redir-tmp");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
    }

    @Test(expected = LoginFailureException.class)
    public void testRedirect303() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/redir-other");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        session.close();
    }

    @Test(expected = BackgroundException.class)
    public void testRedirectGone() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/redir-gone");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
    }

    @Test
    public void testLoginBasicAuth() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                System.getProperties().getProperty("webdav.user"), System.getProperties().getProperty("webdav.password")
        ));
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        session.close();
    }

    @Test
    public void testTouch() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                System.getProperties().getProperty("webdav.user"), System.getProperties().getProperty("webdav.password")
        ));
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        session.getFeature(Touch.class).touch(test);
        assertTrue(session.getFeature(Find.class).find(test));
        new DAVDeleteFeature(session).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
            }
        });
        assertFalse(session.getFeature(Find.class).find(test));
        session.close();
    }

    @Test
    public void testListAnonymous() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/dav/anon");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        assertNotNull(session.list(new DefaultHomeFinderService(session).find(), new DisabledListProgressListener()));
        session.close();
    }

    @Test
    public void testAlert() throws Exception {
        PreferencesFactory.get().setProperty("webdav.basic.preemptive", true);
        assertTrue(new DAVSession(new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials("u", "p"))).alert(new DisabledConnectionCallback()));
        assertFalse(new DAVSession(new Host(new DAVSSLProtocol(), "test.cyberduck.ch", new Credentials("u", "p"))).alert(new DisabledConnectionCallback()));
        PreferencesFactory.get().setProperty("webdav.basic.preemptive", false);
        assertFalse(new DAVSession(new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials("u", "p"))).alert(new DisabledConnectionCallback()));
        assertFalse(new DAVSession(new Host(new DAVSSLProtocol(), "test.cyberduck.ch", new Credentials("u", "p"))).alert(new DisabledConnectionCallback()));
    }

    @Test(expected = LoginFailureException.class)
    public void testLoginFailureBasicAuth() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                "u", "p"
        ));
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new TranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                if(request) {
                    if(message.contains("Authorization: Digest")) {
                        fail(message);
                    }
                }
            }
        });
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        session.close();
    }

    @Test(expected = LoginFailureException.class)
    public void testLoginFailureDigestAuth() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                "u", "p"
        ));
        host.setDefaultPath("/dav/digest");
        final DAVSession session = new DAVSession(host);
        PreferencesFactory.get().setProperty("webdav.basic.preemptive", false);
        session.open(new DisabledHostKeyCallback(), new TranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                if(request) {
                    if(message.contains("Authorization: Basic")) {
                        fail(message);
                    }
                }
            }
        });
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback() {
            @Override
            public void prompt(final Host bookmark, final Credentials credentials, final String title, final String reason,
                               final LoginOptions options) throws LoginCanceledException {
                assertEquals(host.getCredentials(), credentials);
                assertEquals("Login failed", title);
                assertEquals("Authorization Required.", reason);
                assertFalse(options.publickey);
                throw new LoginCanceledException();
            }
        }, null);
    }

    @Test(expected = LoginFailureException.class)
    @Ignore
    public void testLoginErrorBasicFallback() throws Exception {
        final Host host = new Host(new DAVProtocol(), "prod.lattusdemo.com", new Credentials(
                "u", "p"
        ));
        host.setDefaultPath("/namespace");
        final DAVSession session = new DAVSession(host);
        PreferencesFactory.get().setProperty("webdav.basic.preemptive", true);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        try {
            session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        }
        catch(LoginFailureException e) {
            assertEquals("Unauthorized. Please contact your web hosting service provider for assistance.", e.getDetail());
            throw e;
        }
    }

    @Test
    @Ignore
    public void testFeatures() throws Exception {
        final Session session = new DAVSession(new Host(new DAVProtocol(), "h"));
        assertNull(session.getFeature(UnixPermission.class));
        assertNull(session.getFeature(Timestamp.class));
        assertNotNull(session.getFeature(Copy.class));
        assertNotNull(session.getFeature(Headers.class));
        assertNotNull(session.getFeature(DistributionConfiguration.class));
    }

    @Test
    @Ignore
    public void testdavpiximegallery() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "g2.pixi.me", new Credentials(
                "webdav", "webdav"
        ));
        host.setDefaultPath("/w/webdav/");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test(expected = LoginFailureException.class)
    public void testInteroperabilityDocumentsEpflTLSv1() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "documents.epfl.ch");
        final DAVSession session = new DAVSession(host, new DisabledX509TrustManager(), new DefaultX509KeyManager(),
                new CustomTrustSSLProtocolSocketFactory(new DisabledX509TrustManager(), new DefaultX509KeyManager(), "TLSv1"));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test(expected = InteroperabilityException.class)
    public void testInteroperabilityDocumentsEpflFailure() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "documents.epfl.ch");
        final DAVSession session = new DAVSession(host, new DisabledX509TrustManager(), new DefaultX509KeyManager(),
                new CustomTrustSSLProtocolSocketFactory(new DisabledX509TrustManager(), new DefaultX509KeyManager(), "TLSv12"));
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test
    public void testInteroperabilitydavpixime() throws Exception {
        // no SSLv3, supports TLSv1-1.2 - Apache/mod_dav framework)
        final Host host = new Host(new DAVSSLProtocol(), "dav.pixi.me", new Credentials(
                "webdav", "webdav"
        ));
        final DAVSession session = new DAVSession(host, new DefaultX509TrustManager() {
            @Override
            public void verify(final String hostname, final X509Certificate[] certs, final String cipher) throws CertificateException {
                assertEquals("ECDHE_RSA", cipher);
                super.verify(hostname, certs, cipher);
            }
        }, new DefaultX509KeyManager());
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test
    public void testInteroperabilitytlsv11pixime() throws Exception {
        // no SSLv3, supports TLSv1.1 only -Apache/mod_dav framework
        final Host host = new Host(new DAVSSLProtocol(), "tlsv11.pixi.me", new Credentials(
                "webdav", "webdav"
        ));
        final DAVSession session = new DAVSession(host, new DefaultX509TrustManager() {
            @Override
            public void verify(final String hostname, final X509Certificate[] certs, final String cipher) throws CertificateException {
                assertEquals("ECDHE_RSA", cipher);
                super.verify(hostname, certs, cipher);
            }
        }, new DefaultX509KeyManager());
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test
    public void testInteroperabilitytlsv12pixime() throws Exception {
        // no SSLv3, supports TLSv1.2 only - Apache/mod_dav framework
        final Host host = new Host(new DAVSSLProtocol(), "tlsv12.pixi.me", new Credentials(
                "webdav", "webdav"
        ));
        final DAVSession session = new DAVSession(host, new DefaultX509TrustManager() {
            @Override
            public void verify(final String hostname, final X509Certificate[] certs, final String cipher) throws CertificateException {
                assertEquals("ECDHE_RSA", cipher);
                super.verify(hostname, certs, cipher);
            }
        }, new DefaultX509KeyManager());
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test
    public void testLoginChangeUsername() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"),
                PreferencesFactory.get().getProperty("connection.login.anon.pass"))
        );
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        final AtomicBoolean prompt = new AtomicBoolean();
        final LoginConnectionService c = new LoginConnectionService(new DisabledLoginCallback() {
            @Override
            public void prompt(Host bookmark, Credentials credentials,
                               String title, String reason, LoginOptions options) throws LoginCanceledException {
                if(prompt.get()) {
                    fail();
                }
                credentials.setUsername(System.getProperties().getProperty("webdav.user"));
                credentials.setPassword(System.getProperties().getProperty("webdav.password"));
                prompt.set(true);
            }

            @Override
            public void warn(Protocol protocol, String title, String message,
                             String continueButton, String disconnectButton, String preference) throws LoginCanceledException {
                //
            }
        }, new DisabledHostKeyCallback(),
                new DisabledPasswordStore(), new DisabledProgressListener(), new DisabledTranscriptListener());
        c.connect(session, PathCache.empty());
        assertTrue(prompt.get());
        assertTrue(session.isConnected());
        assertFalse(session.isSecured());
        assertNotNull(session.workdir());
        session.close();
    }

    @Test(expected = InteroperabilityException.class)
    @Ignore
    public void testMutualTlsUnknownCA() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "auth.startssl.com");
        final DAVSession session = new DAVSession(host, new DefaultX509TrustManager(),
                new KeychainX509KeyManager(new DisabledCertificateStore() {
                    @Override
                    public X509Certificate choose(String[] keyTypes, Principal[] issuers, String hostname, String prompt)
                            throws ConnectionCanceledException {
                        assertEquals("auth.startssl.com", hostname);
                        assertEquals("The server requires a certificate to validate your identity. Select the certificate to authenticate yourself to auth.startssl.com.",
                                prompt);
                        assertTrue(Arrays.asList(issuers).contains(new X500Principal("" +
                                "CN=StartCom Certification Authority, OU=Secure Digital Certificate Signing, O=StartCom Ltd., C=IL")));
                        assertTrue(Arrays.asList(issuers).contains(new X500Principal("" +
                                "CN=StartCom Class 1 Primary Intermediate Client CA, OU=Secure Digital Certificate Signing, O=StartCom Ltd., C=IL")));
                        assertTrue(Arrays.asList(issuers).contains(new X500Principal("" +
                                "CN=StartCom Class 2 Primary Intermediate Client CA, OU=Secure Digital Certificate Signing, O=StartCom Ltd., C=IL")));
                        assertTrue(Arrays.asList(issuers).contains(new X500Principal("" +
                                "CN=StartCom Class 3 Primary Intermediate Client CA, OU=Secure Digital Certificate Signing, O=StartCom Ltd., C=IL")));
                        throw new ConnectionCanceledException(prompt);
                    }
                }));
        final LoginConnectionService c = new LoginConnectionService(
                new DisabledLoginCallback() {
                    @Override
                    public void prompt(Host bookmark, Credentials credentials,
                                       String title, String reason, LoginOptions options) throws LoginCanceledException {
                        //
                    }
                },
                new DisabledHostKeyCallback(),
                new DisabledPasswordStore(),
                new DisabledProgressListener(), new DisabledTranscriptListener());
        try {
            c.connect(session, PathCache.empty());
        }
        catch(InteroperabilityException e) {
            throw e;
        }
    }

    @Test(expected = InteroperabilityException.class)
    public void testConnectMutualTlsNoCertificate() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "test.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"),
                PreferencesFactory.get().getProperty("connection.login.anon.pass"))
        );
        host.setDefaultPath("/dav");
        final DAVSession session = new DAVSession(host, new DefaultX509TrustManager(),
                new KeychainX509KeyManager(new DisabledCertificateStore() {
                    @Override
                    public X509Certificate choose(String[] keyTypes, Principal[] issuers, String hostname, String prompt)
                            throws ConnectionCanceledException {
                        assertEquals("test.cyberduck.ch", hostname);
                        assertEquals("The server requires a certificate to validate your identity. Select the certificate to authenticate yourself to test.cyberduck.ch.",
                                prompt);
                        throw new ConnectionCanceledException(prompt);
                    }
                }));
        final LoginConnectionService c = new LoginConnectionService(
                new DisabledLoginCallback(),
                new DisabledHostKeyCallback(),
                new DisabledPasswordStore(),
                new DisabledProgressListener(),
                new DisabledTranscriptListener());
        try {
            c.connect(session, PathCache.empty());
        }
        catch(InteroperabilityException e) {
            assertEquals("Handshake failure. Unable to negotiate an acceptable set of security parameters. Please contact your web hosting service provider for assistance.", e.getDetail());
            throw e;
        }
    }

    @Test(expected = InteroperabilityException.class)
    public void testConnectMutualTls() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "test.cyberduck.ch", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"),
                PreferencesFactory.get().getProperty("connection.login.anon.pass"))
        );
        host.setDefaultPath("/dav");
        final DAVSession session = new DAVSession(host, new DefaultX509TrustManager() {
            @Override
            public void verify(final String hostname, final X509Certificate[] certs, final String cipher) throws CertificateException {
                assertEquals(2, certs.length);
                super.verify(hostname, certs, cipher);
            }
        },
                new KeychainX509KeyManager(new DisabledCertificateStore() {
                    @Override
                    public X509Certificate choose(String[] keyTypes, Principal[] issuers, String hostname, String prompt)
                            throws ConnectionCanceledException {
                        assertEquals("test.cyberduck.ch", hostname);
                        assertEquals("The server requires a certificate to validate your identity. Select the certificate to authenticate yourself to test.cyberduck.ch.",
                                prompt);
                        throw new ConnectionCanceledException(prompt);
                    }
                }));
        final LoginConnectionService c = new LoginConnectionService(
                new DisabledLoginCallback(),
                new DisabledHostKeyCallback(),
                new DisabledPasswordStore(),
                new DisabledProgressListener(),
                new DisabledTranscriptListener());
        try {
            c.connect(session, PathCache.empty());
        }
        catch(InteroperabilityException e) {
            assertEquals("Handshake failure. Unable to negotiate an acceptable set of security parameters. Please contact your web hosting service provider for assistance.", e.getDetail());
            throw e;
        }
    }

    @Test(expected = LoginCanceledException.class)
    public void testTrustChain1() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "dav.pixi.me", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"),
                PreferencesFactory.get().getProperty("connection.login.anon.pass"))
        );
        final AtomicBoolean verified = new AtomicBoolean();
        final DAVSession session = new DAVSession(host, new DefaultX509TrustManager() {
            @Override
            public void verify(final String hostname, final X509Certificate[] certs, final String cipher) throws CertificateException {
                assertEquals(2, certs.length);
                assertEquals("CN=RapidSSL SHA256 CA - G3,O=GeoTrust Inc.,C=US",
                        certs[certs.length - 1].getSubjectX500Principal().getName());
                assertEquals("OU=GT79990730,OU=See www.rapidssl.com/resources/cps (c)15,OU=Domain Control Validated - RapidSSL(R),CN=*.pixi.me",
                        certs[0].getSubjectDN().getName());
                verified.set(true);
                super.verify(hostname, certs, cipher);
            }
        },
                new KeychainX509KeyManager(new DisabledCertificateStore()));
        final LoginConnectionService c = new LoginConnectionService(
                new DisabledLoginCallback(),
                new DisabledHostKeyCallback(),
                new DisabledPasswordStore(),
                new DisabledProgressListener(),
                new DisabledTranscriptListener());
        c.connect(session, PathCache.empty());
        assertTrue(verified.get());
        session.close();
    }

    @Test
    public void testTrustChain2() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "svn.cyberduck.io", new Credentials(
                PreferencesFactory.get().getProperty("connection.login.anon.name"),
                PreferencesFactory.get().getProperty("connection.login.anon.pass"))
        );
        final AtomicBoolean verified = new AtomicBoolean();
        final DAVSession session = new DAVSession(host, new DefaultX509TrustManager() {
            @Override
            public void verify(final String hostname, final X509Certificate[] certs, final String cipher) throws CertificateException {
                assertEquals(3, certs.length);
                assertEquals("CN=StartCom Class 2 Primary Intermediate Server CA,OU=Secure Digital Certificate Signing,O=StartCom Ltd.,C=IL",
                        certs[1].getSubjectX500Principal().getName());
                assertEquals("C=CH,ST=Bern,L=Bern,O=iterate GmbH,CN=*.cyberduck.io,E=hostmaster@cyberduck.io",
                        certs[0].getSubjectDN().getName());
                verified.set(true);
                super.verify(hostname, certs, cipher);
            }
        },
                new KeychainX509KeyManager(new DisabledCertificateStore()));
        final LoginConnectionService c = new LoginConnectionService(
                new DisabledLoginCallback(),
                new DisabledHostKeyCallback(),
                new DisabledPasswordStore(),
                new DisabledProgressListener(),
                new DisabledTranscriptListener());
        c.connect(session, PathCache.empty());
        assertTrue(verified.get());
        session.close();
    }

    @Test(expected = ConnectionCanceledException.class)
    public void testHandshakeFailure() throws Exception {
        final Session session = new DAVSession(new Host(new DAVSSLProtocol(), "54.228.253.92", new Credentials("user", "p")),
                new CertificateStoreX509TrustManager(new TrustManagerHostnameCallback() {
                    @Override
                    public String getTarget() {
                        return "54.228.253.92";
                    }
                }, new DisabledCertificateStore() {
                    @Override
                    public boolean isTrusted(final String hostname, final List<X509Certificate> certificates) {
                        return false;
                    }
                }
                ), new DefaultX509KeyManager()
        );
        final LoginConnectionService s = new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(), new DisabledPasswordStore(),
                new DisabledProgressListener(), new DisabledTranscriptListener());
        s.check(session, PathCache.empty());
    }
}

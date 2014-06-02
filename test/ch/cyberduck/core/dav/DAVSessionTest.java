package ch.cyberduck.core.dav;

import ch.cyberduck.core.*;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.features.Copy;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Headers;
import ch.cyberduck.core.features.Timestamp;
import ch.cyberduck.core.features.Touch;
import ch.cyberduck.core.features.UnixPermission;
import ch.cyberduck.core.shared.DefaultHomeFinderService;
import ch.cyberduck.core.ssl.KeychainX509KeyManager;
import ch.cyberduck.core.ssl.KeychainX509TrustManager;
import ch.cyberduck.core.ssl.TrustManagerHostnameCallback;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class DAVSessionTest extends AbstractTestCase {

    @Test
    public void testConnect() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "svn.cyberduck.ch", new Credentials(
                Preferences.instance().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host);
        assertNotNull(session.open(new DisabledHostKeyCallback()));
        assertTrue(session.isConnected());
        assertNotNull(session.getClient());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        final AttributedList<Path> list = session.list(new Path("/", EnumSet.of(Path.Type.directory, Path.Type.volume)), new DisabledListProgressListener());
        assertNotNull(list.get(new Path("/trunk", EnumSet.of(Path.Type.directory)).getReference()));
        assertNotNull(list.get(new Path("/branches", EnumSet.of(Path.Type.directory)).getReference()));
        assertNotNull(list.get(new Path("/tags", EnumSet.of(Path.Type.directory)).getReference()));
        assertTrue(session.isConnected());
        session.close();
        assertFalse(session.isConnected());
    }

    @Test(expected = InteroperabilityException.class)
    public void testSsl() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "test.cyberduck.ch", new Credentials(
                Preferences.instance().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host);
        assertFalse(session.alert());
        session.open(new DisabledHostKeyCallback());
        assertTrue(session.isSecured());
        try {
            session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        }
        catch(BackgroundException e) {
            assertEquals("Method Not Allowed.", e.getDetail());
            throw e;
        }
    }

    @Test(expected = InteroperabilityException.class)
    public void testHtmlResponse() throws Exception {
        final Host host = new Host(new DAVProtocol(), "media.cyberduck.ch", new Credentials(
                Preferences.instance().getProperty("connection.login.anon.name"), null
        ));
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
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
                Preferences.instance().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/redir-perm");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        session.close();
    }

    @Test(expected = LoginFailureException.class)
    public void testRedirect302() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                Preferences.instance().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/redir-tmp");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
    }

    @Test(expected = LoginFailureException.class)
    public void testRedirect303() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                Preferences.instance().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/redir-other");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        session.close();
    }

    @Test(expected = BackgroundException.class)
    public void testRedirectGone() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                Preferences.instance().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/redir-gone");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
    }

    @Test
    public void testLoginBasicAuth() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("webdav.user"), properties.getProperty("webdav.password")
        ));
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        session.close();
    }

    @Test
    public void testTouch() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                properties.getProperty("webdav.user"), properties.getProperty("webdav.password")
        ));
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        session.getFeature(Touch.class).touch(test);
        assertTrue(session.getFeature(Find.class).find(test));
        new DAVDeleteFeature(session).delete(Collections.<Path>singletonList(test), new DisabledLoginController());
        assertFalse(session.getFeature(Find.class).find(test));
        session.close();
    }

    @Test
    public void testListAnonymous() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                Preferences.instance().getProperty("connection.login.anon.name"), null
        ));
        host.setDefaultPath("/dav/anon");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        assertNotNull(session.list(new DefaultHomeFinderService(session).find(), new DisabledListProgressListener()));
        session.close();
    }

    @Test
    public void testAlert() throws Exception {
        Preferences.instance().setProperty("webdav.basic.preemptive", true);
        assertTrue(new DAVSession(new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials("u", "p"))).alert());
        assertFalse(new DAVSession(new Host(new DAVSSLProtocol(), "test.cyberduck.ch", new Credentials("u", "p"))).alert());
        Preferences.instance().setProperty("webdav.basic.preemptive", false);
        assertFalse(new DAVSession(new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials("u", "p"))).alert());
        assertFalse(new DAVSession(new Host(new DAVSSLProtocol(), "test.cyberduck.ch", new Credentials("u", "p"))).alert());
    }

    @Test(expected = LoginFailureException.class)
    public void testLoginFailureBasicAuth() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                "u", "p"
        ));
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        session.addTranscriptListener(new TranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                if(request) {
                    if(message.contains("Authorization: Digest")) {
                        fail(message);
                    }
                }
            }
        });
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        session.close();
    }

    @Test(expected = LoginFailureException.class)
    public void testLoginFailureDigestAuth() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                "u", "p"
        ));
        host.setDefaultPath("/dav/digest");
        final DAVSession session = new DAVSession(host);
        session.addTranscriptListener(new TranscriptListener() {
            @Override
            public void log(final boolean request, final String message) {
                if(request) {
                    if(message.contains("Authorization: Basic")) {
                        fail(message);
                    }
                }
            }
        });
        Preferences.instance().setProperty("webdav.basic.preemptive", false);
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController() {
            @Override
            public void prompt(final Protocol protocol, final Credentials credentials, final String title, final String reason,
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
        Preferences.instance().setProperty("webdav.basic.preemptive", true);
        session.open(new DisabledHostKeyCallback());
        try {
            session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        }
        catch(LoginFailureException e) {
            assertEquals("Unauthorized. Please contact your web hosting service provider for assistance.", e.getDetail());
            throw e;
        }
    }

    @Test
    public void testFeatures() throws Exception {
        final Session session = new DAVSession(new Host("h"));
        assertNull(session.getFeature(UnixPermission.class));
        assertNull(session.getFeature(Timestamp.class));
        assertNotNull(session.getFeature(Copy.class));
        assertNotNull(session.getFeature(Headers.class));
        assertNotNull(session.getFeature(DistributionConfiguration.class));
    }

    @Test
    public void testdavpiximegallery() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "g2.pixi.me", new Credentials(
                "webdav", "webdav"
        ));
        host.setDefaultPath("/w/webdav/");
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test
    public void testdavpixime() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "dav.pixi.me", new Credentials(
                "webdav", "webdav"
        ));
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test
    public void testtlsv11pixime() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "tlsv11.pixi.me", new Credentials(
                "webdav", "webdav"
        ));
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test
    public void testtlsv12pixime() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "tlsv12.pixi.me", new Credentials(
                "webdav", "webdav"
        ));
        final DAVSession session = new DAVSession(host);
        session.open(new DisabledHostKeyCallback());
        assertTrue(session.isConnected());
        assertTrue(session.isSecured());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
        assertNotNull(session.workdir());
        assertFalse(session.getAcceptedIssuers().isEmpty());
        session.close();
    }

    @Test(expected = InteroperabilityException.class)
    public void testUnrecognizedName() throws Exception {
        final DAVSession session = new DAVSession(new Host(new DAVSSLProtocol(), "sds-security.selfhost.eu", 8000));
        session.open(new DisabledHostKeyCallback());
        session.login(new DisabledPasswordStore(), new DisabledLoginController(), new DisabledCancelCallback());
    }

    @Test
    public void testLoginChangeUsername() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                Preferences.instance().getProperty("connection.login.anon.name"),
                Preferences.instance().getProperty("connection.login.anon.pass"))
        );
        host.setDefaultPath("/dav/basic");
        final DAVSession session = new DAVSession(host);
        final AtomicBoolean prompt = new AtomicBoolean();
        final LoginConnectionService c = new LoginConnectionService(new DisabledLoginController() {
            @Override
            public void prompt(Protocol protocol, Credentials credentials, String title, String reason, LoginOptions options) throws LoginCanceledException {
                if(prompt.get()) {
                    fail();
                }
                credentials.setUsername(properties.getProperty("webdav.user"));
                credentials.setPassword(properties.getProperty("webdav.password"));
                prompt.set(true);
            }

            @Override
            public void warn(Protocol protocol, String title, String message, String continueButton, String disconnectButton, String preference) throws LoginCanceledException {
                //
            }
        }, new DisabledHostKeyCallback(),
                new DisabledPasswordStore(), new DisabledProgressListener());
        c.connect(session, Cache.empty());
        assertTrue(prompt.get());
        assertTrue(session.isConnected());
        assertFalse(session.isSecured());
        assertNotNull(session.workdir());
        session.close();
    }

    @Test(expected = InteroperabilityException.class)
    public void testClientSSLNoCertificate() throws Exception {
        final Host host = new Host(new DAVSSLProtocol(), "auth.startssl.com");
        final TrustManagerHostnameCallback callback = new TrustManagerHostnameCallback() {
            @Override
            public String getTarget() {
                return "auth.startssl.com";
            }
        };
        final DAVSession session = new DAVSession(host, new KeychainX509TrustManager(callback),
                new KeychainX509KeyManager());
        final LoginConnectionService c = new LoginConnectionService(
                new DisabledLoginController() {
                    @Override
                    public void prompt(Protocol protocol, Credentials credentials, String title, String reason, LoginOptions options) throws LoginCanceledException {
                        //
                    }
                },
                new DisabledHostKeyCallback(),
                new DisabledPasswordStore(),
                new DisabledProgressListener());
        c.connect(session, Cache.empty());
    }
}

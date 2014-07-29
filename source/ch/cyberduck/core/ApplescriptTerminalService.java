package ch.cyberduck.core;

import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.local.Application;
import ch.cyberduck.core.local.ApplicationFinder;
import ch.cyberduck.core.local.ApplicationFinderFactory;
import ch.cyberduck.ui.cocoa.foundation.NSAppleScript;
import ch.cyberduck.ui.cocoa.foundation.NSDictionary;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.ObjCObjectByReference;

import java.text.MessageFormat;

/**
 * @version $Id$
 */
public class ApplescriptTerminalService implements TerminalService {
    private static final Logger log = Logger.getLogger(ApplescriptTerminalService.class);

    public static void register() {
        TerminalServiceFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private static class Factory extends TerminalServiceFactory {
        @Override
        protected TerminalService create() {
            return new ApplescriptTerminalService();
        }
    }

    private ApplicationFinder finder
            = ApplicationFinderFactory.get();

    @Override
    public void open(final Host host, final Path workdir) throws AccessDeniedException {
        final boolean identity = host.getCredentials().isPublicKeyAuthentication();
        final Application application
                = finder.getDescription(Preferences.instance().getProperty("terminal.bundle.identifier"));
        if(!finder.isInstalled(application)) {
            log.error(String.format("Application with bundle identifier %s is not installed",
                    Preferences.instance().getProperty("terminal.bundle.identifier")));
        }
        String ssh = MessageFormat.format(Preferences.instance().getProperty("terminal.command.ssh"),
                identity ? "-i " + host.getCredentials().getIdentity().getAbsolute() : StringUtils.EMPTY,
                host.getCredentials().getUsername(),
                host.getHostname(),
                String.valueOf(host.getPort()), this.escape(workdir.getAbsolute()));
        if(log.isInfoEnabled()) {
            log.info(String.format("Excecute SSH command %s", ssh));
        }
        // Escape
        ssh = StringUtils.replace(ssh, "\\", "\\\\");
        // Escape all " for do script command
        ssh = StringUtils.replace(ssh, "\"", "\\\"");
        if(log.isInfoEnabled()) {
            log.info("Escaped SSH Command for Applescript:" + ssh);
        }
        String command
                = "tell application \"" + application.getName() + "\""
                + "\n"
                + "activate"
                + "\n"
                + MessageFormat.format(Preferences.instance().getProperty("terminal.command"), ssh)
                + "\n"
                + "end tell";
        if(log.isInfoEnabled()) {
            log.info(String.format("Execute AppleScript %s", command));
        }
        final NSAppleScript as = NSAppleScript.createWithSource(command);
        final ObjCObjectByReference error = new ObjCObjectByReference();
        if(null == as.executeAndReturnError(error)) {
            final NSDictionary d = error.getValueAs(NSDictionary.class);
            throw new AccessDeniedException(String.format("Failure running script in %s. %s",
                    application.getName(), d.objectForKey("NSAppleScriptErrorBriefMessage")));
        }
    }

    protected String escape(final String path) {
        final StringBuilder escaped = new StringBuilder();
        for(char c : path.toCharArray()) {
            if(StringUtils.isAlphanumeric(String.valueOf(c))) {
                escaped.append(c);
            }
            else {
                escaped.append("\\").append(c);
            }
        }
        return escaped.toString();
    }
}

package ch.cyberduck.core.s3;

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

import ch.cyberduck.core.*;
import ch.cyberduck.core.analytics.AnalyticsProvider;
import ch.cyberduck.core.analytics.QloudstatAnalyticsProvider;
import ch.cyberduck.core.cdn.DistributionConfiguration;
import ch.cyberduck.core.cloudfront.WebsiteCloudFrontDistributionConfiguration;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.*;
import ch.cyberduck.core.http.HttpSession;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.identity.AWSIdentityConfiguration;
import ch.cyberduck.core.identity.IdentityConfiguration;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageBucketLoggingStatus;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.model.WebsiteConfig;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.OAuth2Credentials;
import org.jets3t.service.security.OAuth2Tokens;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.RestUtils;
import org.jets3t.service.utils.ServiceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @version $Id$
 */
public class S3Session extends HttpSession<S3Session.RequestEntityRestStorageService> {
    private static final Logger log = Logger.getLogger(S3Session.class);

    private PathContainerService containerService = new PathContainerService();

    public S3Session(Host h) {
        super(h);
    }

    /**
     * Exposing protected methods
     */
    public class RequestEntityRestStorageService extends RestS3Service {
        public RequestEntityRestStorageService(final Jets3tProperties configuration) {
            super(host.getCredentials().isAnonymousLogin() ? null :
                    new AWSCredentials(host.getCredentials().getUsername(), host.getCredentials().getPassword()),
                    new PreferencesUseragentProvider().get(), null, configuration);
        }

        @Override
        protected HttpClient initHttpConnection() {
            final AbstractHttpClient client = connect();
            if(Preferences.instance().getBoolean("s3.expect-continue")) {
                // Activates 'Expect: 100-Continue' handshake for the entity enclosing methods
                HttpProtocolParams.setUseExpectContinue(client.getParams(), true);
            }
            client.setHttpRequestRetryHandler(new RestUtils.JetS3tRetryHandler(5, this));
            return client;
        }

        @Override
        protected boolean isTargettingGoogleStorageService() {
            return getHost().getHostname().equals(Constants.GS_DEFAULT_HOSTNAME);
        }

        @Override
        protected void initializeProxy() {
            // Client already configured
        }

        @Override
        protected void putObjectWithRequestEntityImpl(String bucketName, StorageObject object,
                                                      HttpEntity requestEntity, Map<String, String> requestParams) throws ServiceException {
            super.putObjectWithRequestEntityImpl(bucketName, object, requestEntity, requestParams);
        }

        @Override
        public void verifyExpectedAndActualETagValues(String expectedETag, StorageObject uploadedObject) throws ServiceException {
            if(StringUtils.isBlank(uploadedObject.getETag())) {
                log.warn("No ETag to verify");
                return;
            }
            super.verifyExpectedAndActualETagValues(expectedETag, uploadedObject);
        }

        /**
         * @return the identifier for the signature algorithm.
         */
        @Override
        protected String getSignatureIdentifier() {
            return S3Session.this.getSignatureIdentifier();
        }

        /**
         * @return header prefix for general Google Storage headers: x-goog-.
         */
        @Override
        public String getRestHeaderPrefix() {
            return S3Session.this.getRestHeaderPrefix();
        }

        /**
         * @return header prefix for Google Storage metadata headers: x-goog-meta-.
         */
        @Override
        public String getRestMetadataPrefix() {
            return S3Session.this.getRestMetadataPrefix();
        }

        @Override
        protected XmlResponsesSaxParser getXmlResponseSaxParser() throws ServiceException {
            return S3Session.this.getXmlResponseSaxParser();
        }

        @Override
        public void setBucketLoggingStatusImpl(String bucketName, StorageBucketLoggingStatus status) throws ServiceException {
            super.setBucketLoggingStatusImpl(bucketName, status);
        }

        @Override
        public StorageBucketLoggingStatus getBucketLoggingStatusImpl(String bucketName) throws ServiceException {
            return super.getBucketLoggingStatusImpl(bucketName);
        }

        @Override
        public WebsiteConfig getWebsiteConfigImpl(String bucketName) throws ServiceException {
            return super.getWebsiteConfigImpl(bucketName);
        }

        @Override
        public void setWebsiteConfigImpl(String bucketName, WebsiteConfig config) throws ServiceException {
            super.setWebsiteConfigImpl(bucketName, config);
        }

        @Override
        public void deleteWebsiteConfigImpl(String bucketName) throws ServiceException {
            super.deleteWebsiteConfigImpl(bucketName);
        }

        @Override
        public void authorizeHttpRequest(HttpUriRequest httpMethod, HttpContext context)
                throws ServiceException {
            if(authorize(httpMethod, getProviderCredentials())) {
                return;
            }
            super.authorizeHttpRequest(httpMethod, context);
        }

        @Override
        protected boolean isRecoverable403(HttpUriRequest httpRequest, Exception exception) {
            if(getProviderCredentials() instanceof OAuth2Credentials) {
                OAuth2Tokens tokens;
                try {
                    tokens = ((OAuth2Credentials) getProviderCredentials()).getOAuth2Tokens();
                }
                catch(IOException e) {
                    return false;
                }
                if(tokens != null) {
                    tokens.expireAccessToken();
                    return true;
                }
            }
            return super.isRecoverable403(httpRequest, exception);
        }

        @Override
        protected StorageBucket createBucketImpl(String bucketName, String location,
                                                 AccessControlList acl) throws ServiceException {
            if(StringUtils.isNotBlank(getProjectId())) {
                return super.createBucketImpl(bucketName, location, acl,
                        Collections.<String, Object>singletonMap("x-goog-project-id", getProjectId()));
            }
            return super.createBucketImpl(bucketName, location, acl);
        }

        @Override
        protected StorageBucket[] listAllBucketsImpl() throws ServiceException {
            if(StringUtils.isNotBlank(getProjectId())) {
                return super.listAllBucketsImpl(
                        Collections.<String, Object>singletonMap("x-goog-project-id", getProjectId()));
            }
            return super.listAllBucketsImpl();
        }
    }

    protected boolean authorize(HttpUriRequest httpMethod, ProviderCredentials credentials)
            throws ServiceException {
        return false;
    }

    protected XmlResponsesSaxParser getXmlResponseSaxParser() throws ServiceException {
        return new XmlResponsesSaxParser(client.getJetS3tProperties(), false);
    }

    /**
     * @return the identifier for the signature algorithm.
     */
    protected String getSignatureIdentifier() {
        return "AWS";
    }

    /**
     * @return header prefix for general Google Storage headers: x-goog-.
     */
    protected String getRestHeaderPrefix() {
        return "x-amz-";
    }

    /**
     * @return header prefix for Google Storage metadata headers: x-goog-meta-.
     */
    protected String getRestMetadataPrefix() {
        return "x-amz-meta-";
    }

    protected String getProjectId() {
        return null;
    }

    protected Jets3tProperties configure() {
        final Jets3tProperties configuration = new Jets3tProperties();
        if(log.isDebugEnabled()) {
            log.debug(String.format("Configure for endpoint %s", host.getHostname(true)));
        }
        if(StringUtils.isNotBlank(host.getProtocol().getDefaultHostname())
                && host.getHostname(true).endsWith(host.getProtocol().getDefaultHostname())) {
            // The user specified a DNS bucket endpoint. Connect to the default hostname instead.
            configuration.setProperty("s3service.s3-endpoint", host.getProtocol().getDefaultHostname());
            configuration.setProperty("s3service.enable-storage-classes", String.valueOf(true));
        }
        else {
            // Standard configuration
            configuration.setProperty("s3service.s3-endpoint", host.getHostname(true));
            configuration.setProperty("s3service.disable-dns-buckets", String.valueOf(true));
            configuration.setProperty("s3service.enable-storage-classes", String.valueOf(false));
        }
        if(StringUtils.isNotBlank(host.getProtocol().getContext())) {
            configuration.setProperty("s3service.s3-endpoint-virtual-path", PathNormalizer.normalize(host.getProtocol().getContext()));
        }
        configuration.setProperty("s3service.https-only", String.valueOf(host.getProtocol().isSecure()));
        if(host.getProtocol().isSecure()) {
            configuration.setProperty("s3service.s3-endpoint-https-port", String.valueOf(host.getPort()));
        }
        else {
            configuration.setProperty("s3service.s3-endpoint-http-port", String.valueOf(host.getPort()));
        }
        // The maximum number of retries that will be attempted when an S3 connection fails
        // with an InternalServer error. To disable retries of InternalError failures, set this to 0.
        configuration.setProperty("s3service.internal-error-retry-max", String.valueOf(0));
        // The maximum number of concurrent communication threads that will be started by
        // the multi-threaded service for upload and download operations.
        configuration.setProperty("s3service.max-thread-count", String.valueOf(1));
        configuration.setProperty("httpclient.proxy-autodetect", String.valueOf(false));
        return configuration;
    }

    private String getHostnameForContainer(final Path bucket) {
        if(this.configure().getBoolProperty("s3service.disable-dns-buckets", false)) {
            return this.getHost().getHostname(true);
        }
        if(!ServiceUtils.isBucketNameValidDNSName(containerService.getContainer(bucket).getName())) {
            return this.getHost().getHostname(true);
        }
        if(this.getHost().getHostname().equals(this.getHost().getProtocol().getDefaultHostname())) {
            return String.format("%s.%s", bucket.getName(), this.getHost().getHostname(true));
        }
        return this.getHost().getHostname(true);
    }

    @Override
    public RequestEntityRestStorageService connect(final HostKeyController key) throws BackgroundException {
        return new RequestEntityRestStorageService(this.configure());
    }

    @Override
    public void login(final PasswordStore keychain, final LoginController prompt) throws BackgroundException {
        client.setProviderCredentials(host.getCredentials().isAnonymousLogin() ? null :
                new AWSCredentials(host.getCredentials().getUsername(), host.getCredentials().getPassword()));
        // List all buckets and cache
        this.cache().put(new Path(String.valueOf(Path.DELIMITER), Path.DIRECTORY_TYPE | Path.VOLUME_TYPE).getReference(),
                new AttributedList<Path>(new S3BucketListService().list(this)));
    }

    @Override
    public boolean isRenameSupported(final Path file) {
        return !file.attributes().isVolume();
    }

    /**
     * Overwritten to provide publicly accessible URL of given object
     *
     * @return Using scheme from protocol
     */
    @Override
    public String toURL(final Path path) {
        return this.toURL(path, this.getHost().getProtocol().getScheme().toString());
    }

    /**
     * Overwritten to provide publicy accessible URL of given object
     *
     * @return Plain HTTP link
     */
    @Override
    public String toHttpURL(final Path path) {
        return this.toURL(path, Scheme.http.name());
    }

    /**
     * Properly URI encode and prepend the bucket name.
     *
     * @param scheme Protocol
     * @return URL to be displayed in browser
     */
    private String toURL(final Path file, final String scheme) {
        final StringBuilder url = new StringBuilder(scheme);
        url.append("://");
        if(file.isRoot()) {
            url.append(this.getHost().getHostname());
        }
        else {
            final String hostname = this.getHostnameForContainer(containerService.getContainer(file));
            if(hostname.startsWith(containerService.getContainer(file).getName())) {
                url.append(hostname);
                if(!containerService.isContainer(file)) {
                    url.append(URIEncoder.encode(containerService.getKey(file)));
                }
            }
            else {
                url.append(this.getHost().getHostname());
                url.append(URIEncoder.encode(file.getAbsolute()));
            }
        }
        return url.toString();
    }

    /**
     * Query string authentication. Query string authentication is useful for giving HTTP or browser access to
     * resources that would normally require authentication. The signature in the query string secures the request.
     *
     * @return A signed URL with a limited validity over time.
     */
    public DescriptiveUrl toSignedUrl(final Path path) {
        return toSignedUrl(path, Preferences.instance().getInteger("s3.url.expire.seconds"));
    }

    /**
     * Query String Authentication generates a signed URL string that will grant
     * access to an S3 resource (bucket or object)
     * to whoever uses the URL up until the time specified.
     *
     * @param seconds Expire after seconds elapsed
     * @return Temporary URL to be displayed in browser
     */
    public DescriptiveUrl toSignedUrl(final Path file, final int seconds) {
        return this.toSignedUrl(file, seconds, PasswordStoreFactory.get());
    }

    protected DescriptiveUrl toSignedUrl(final Path file, final int seconds, final HostPasswordStore store) {
        if(this.getHost().getCredentials().isAnonymousLogin()) {
            return DescriptiveUrl.EMPTY;
        }
        if(file.attributes().isFile()) {
            // Determine expiry time for URL
            final Calendar expiry = Calendar.getInstance();
            expiry.add(Calendar.SECOND, seconds);
            // Generate URL
            final RequestEntityRestStorageService client = new RequestEntityRestStorageService(this.configure());
            final String secret = store.find(host);
            if(StringUtils.isBlank(secret)) {
                return DescriptiveUrl.EMPTY;
            }
            client.setProviderCredentials(
                    new AWSCredentials(host.getCredentials().getUsername(), secret));
            return new DescriptiveUrl(client.createSignedUrl("GET",
                    containerService.getContainer(file).getName(), containerService.getKey(file), null,
                    null, expiry.getTimeInMillis() / 1000, false, this.getHost().getProtocol().isSecure(), false),
                    MessageFormat.format(Locale.localizedString("{0} URL"), Locale.localizedString("Signed", "S3"))
                            + " (" + MessageFormat.format(Locale.localizedString("Expires on {0}", "S3") + ")",
                            UserDateFormatterFactory.get().getShortFormat(expiry.getTimeInMillis()))
            );
        }
        return DescriptiveUrl.EMPTY;
    }

    /**
     * Generates a URL string that will return a Torrent file for an object in S3,
     * which file can be downloaded and run in a BitTorrent client.
     *
     * @return Torrent URL
     */
    public DescriptiveUrl toTorrentUrl(final Path path) {
        if(path.attributes().isFile()) {
            return new DescriptiveUrl(new RequestEntityRestStorageService(this.configure()).createTorrentUrl(
                    containerService.getContainer(path).getName(),
                    containerService.getKey(path)));
        }
        return DescriptiveUrl.EMPTY;
    }

    @Override
    public Set<DescriptiveUrl> getHttpURLs(final Path path) {
        final Set<DescriptiveUrl> urls = super.getHttpURLs(path);
        // Always include HTTP URL
        urls.add(new DescriptiveUrl(this.toURL(path, Scheme.http.name()),
                MessageFormat.format(Locale.localizedString("{0} URL"), Scheme.http.name().toUpperCase(java.util.Locale.ENGLISH))));
        final DescriptiveUrl hour = this.toSignedUrl(path, 60 * 60);
        if(StringUtils.isNotBlank(hour.getUrl())) {
            urls.add(hour);
        }
        // Default signed URL expiring in 24 hours.
        final DescriptiveUrl day = this.toSignedUrl(path, Preferences.instance().getInteger("s3.url.expire.seconds"));
        if(StringUtils.isNotBlank(day.getUrl())) {
            urls.add(day);
        }
        final DescriptiveUrl week = this.toSignedUrl(path, 7 * 24 * 60 * 60);
        if(StringUtils.isNotBlank(week.getUrl())) {
            urls.add(week);
        }
        final DescriptiveUrl torrent = this.toTorrentUrl(path);
        if(StringUtils.isNotBlank(torrent.getUrl())) {
            urls.add(new DescriptiveUrl(torrent.getUrl(),
                    MessageFormat.format(Locale.localizedString("{0} URL"), Locale.localizedString("Torrent"))));
        }
        return urls;
    }

    @Override
    public InputStream read(final Path file, final TransferStatus status) throws BackgroundException {
        try {
            if(file.attributes().isDuplicate()) {
                return this.getClient().getVersionedObject(file.attributes().getVersionId(),
                        containerService.getContainer(file).getName(), containerService.getKey(file),
                        null, // ifModifiedSince
                        null, // ifUnmodifiedSince
                        null, // ifMatch
                        null, // ifNoneMatch
                        status.isResume() ? status.getCurrent() : null, null).getDataInputStream();
            }
            return this.getClient().getObject(containerService.getContainer(file).getName(), containerService.getKey(file),
                    null, // ifModifiedSince
                    null, // ifUnmodifiedSince
                    null, // ifMatch
                    null, // ifNoneMatch
                    status.isResume() ? status.getCurrent() : null, null).getDataInputStream();
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map("Download failed", e, file);
        }
    }

    @Override
    public OutputStream write(final Path file, final TransferStatus status) throws BackgroundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AttributedList<Path> list(final Path file, final ListProgressListener listener) throws BackgroundException {
        if(file.isRoot()) {
            // List all buckets
            return new AttributedList<Path>(new S3BucketListService().list(this));
        }
        else {
            return new S3ObjectListService(this).list(file, listener);
        }
    }

    @Override
    public void mkdir(final Path file, final String region) throws BackgroundException {
        try {
            if(containerService.isContainer(file)) {
                final S3BucketCreateService service = new S3BucketCreateService(this);
                if(StringUtils.isBlank(region)) {
                    service.create(file, Preferences.instance().getProperty("s3.location"));
                }
                else {
                    service.create(file, region);
                }
            }
            else {
                // Add placeholder object
                final StorageObject object = new StorageObject(containerService.getKey(file) + Path.DELIMITER);
                object.setBucketName(containerService.getContainer(file).getName());
                object.setContentLength(0);
                object.setContentType("application/x-directory");
                this.getClient().putObject(containerService.getContainer(file).getName(), object);
            }
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map("Cannot create folder {0}", e, file);
        }
    }

    @Override
    public void rename(final Path file, final Path renamed) throws BackgroundException {
        try {
            if(file.attributes().isFile() || file.attributes().isPlaceholder()) {
                final StorageObject destination = new StorageObject(containerService.getKey(renamed));
                // Keep same storage class
                destination.setStorageClass(file.attributes().getStorageClass());
                // Keep encryption setting
                destination.setServerSideEncryptionAlgorithm(file.attributes().getEncryption());
                // Apply non standard ACL
                final S3AccessControlListFeature acl = new S3AccessControlListFeature(this);
                destination.setAcl(acl.convert(acl.getPermission(file)));
                // Moving the object retaining the metadata of the original.
                this.getClient().moveObject(containerService.getContainer(file).getName(), containerService.getKey(file),
                        containerService.getContainer(renamed).getName(),
                        destination, false);
            }
            else if(file.attributes().isDirectory()) {
                for(Path i : this.list(file, new DisabledListProgressListener())) {
                    this.rename(i, new Path(renamed, i.getName(), i.attributes().getType()));
                }
            }
        }
        catch(ServiceException e) {
            throw new ServiceExceptionMappingService().map("Cannot rename {0}", e, file);
        }
    }

    @Override
    public <T> T getFeature(final Class<T> type, final LoginController prompt) {
        if(type == Upload.class) {
            return (T) new S3ThresholdUploadService(this);
        }
        if(type == Delete.class) {
            if(this.getHost().getHostname().equals(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new S3MultipleDeleteFeature(this, prompt);
            }
            return (T) new S3DefaultDeleteFeature(this);
        }
        if(type == AclPermission.class) {
            return (T) new S3AccessControlListFeature(this);
        }
        if(type == Headers.class) {
            return (T) new S3MetadataFeature(this);
        }
        if(type == Touch.class) {
            return (T) new S3TouchFeature(this);
        }
        if(type == Location.class) {
            // Only for AWS
            if(this.getHost().getHostname().equals(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new S3LocationFeature(this);
            }
        }
        if(type == AnalyticsProvider.class) {
            // Only for AWS
            if(this.getHost().getHostname().equals(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new QloudstatAnalyticsProvider();
            }
            return null;
        }
        if(type == Versioning.class) {
            // Only for AWS
            if(this.getHost().getHostname().equals(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new S3VersioningFeature(this);
            }
            return null;
        }
        if(type == Logging.class) {
            // Only for AWS
            if(this.getHost().getHostname().equals(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new S3LoggingFeature(this);
            }
            return null;
        }
        if(type == Lifecycle.class) {
            // Only for AWS
            if(this.getHost().getHostname().equals(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new S3LifecycleConfiguration(this);
            }
        }
        if(type == Encryption.class) {
            // Only for AWS
            if(this.getHost().getHostname().equals(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new S3EncryptionFeature(this);
            }
            return null;
        }
        if(type == Redundancy.class) {
            // Only for AWS
            if(this.getHost().getHostname().equals(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new S3StorageClassFeature(this);
            }
            return null;
        }
        if(type == IdentityConfiguration.class) {
            // Only for AWS
            if(this.getHost().getHostname().equals(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new AWSIdentityConfiguration(host, prompt);
            }
        }
        if(type == DistributionConfiguration.class) {
            if(host.getHostname().endsWith(Constants.S3_DEFAULT_HOSTNAME)) {
                return (T) new WebsiteCloudFrontDistributionConfiguration(this, prompt);
            }
            else {
                // Amazon CloudFront custom origin
                return super.getFeature(type, prompt);
            }
        }
        return super.getFeature(type, prompt);
    }
}
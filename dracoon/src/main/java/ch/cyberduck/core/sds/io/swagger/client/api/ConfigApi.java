package ch.cyberduck.core.sds.io.swagger.client.api;

import ch.cyberduck.core.sds.io.swagger.client.ApiException;
import ch.cyberduck.core.sds.io.swagger.client.ApiClient;
import ch.cyberduck.core.sds.io.swagger.client.Configuration;
import ch.cyberduck.core.sds.io.swagger.client.Pair;

import javax.ws.rs.core.GenericType;

import ch.cyberduck.core.sds.io.swagger.client.model.ConfigOptionList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-05-03T10:55:56.129+02:00")
public class ConfigApi {
  private ApiClient apiClient;

  public ConfigApi() {
    this(Configuration.getDefaultApiClient());
  }

  public ConfigApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Get authentication settings
   * &lt;div class&#x3D;\&quot;sds\&quot;&gt;&lt;p&gt;&lt;strong&gt;Functional Description:&lt;/strong&gt; &lt;br/&gt;Retrieve the settings of authentication configuration.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Precondition:&lt;/strong&gt; Right \&quot;Config Read\&quot; required.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Effects:&lt;/strong&gt; None.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Further Information:&lt;/strong&gt; None.&lt;/p&gt;&lt;/div&gt;&lt;div class&#x3D;\&quot;sds\&quot;&gt;&lt;b&gt;Configuration settings for various authentication methods&lt;/b&gt;&lt;h4&gt;Authentication Methods&lt;/h4&gt;&lt;dl&gt;&lt;dt&gt;sql&lt;/dt&gt;&lt;dd&gt;&lt;b&gt;Basic Authentication globally allowed.&lt;/b&gt;&lt;br/&gt;This option must be activated to allow users to log in with their credentials stored in the database.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;active_directory&lt;/dt&gt;&lt;dd&gt;&lt;b&gt;Active Directory Authentication globally allowed&lt;/b&gt;&lt;br/&gt;This option must be activated to allow users to log in with their Active Directory credentials.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;radius&lt;/dt&gt;&lt;dd&gt;&lt;b&gt;RADIUS Authentication globally allowed&lt;/b&gt;&lt;br/&gt;This option must be activated to allow users to log in with their RADIUS username, their PIN and a token password.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;openid&lt;/dt&gt;&lt;dd&gt;&lt;b&gt;OpenID Connect Authentication globally allowed&lt;/b&gt;&lt;br/&gt;This option must be activated to allow users to log in with their OpenID Connect identity.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_auth_method&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;&lt;b&gt;Default authentication method without user context&lt;/b&gt;&lt;br/&gt;If this option is set, the chosen method will be provided as default authentication method if no user context is available.&lt;br/&gt;Only one authentication method can be set and it must be allowed (see above).&lt;br/&gt;If no value is set, there is no guarantee about the order of the returned methods.&lt;br/&gt;Only activated authentication methods may be set as default authentication method.&lt;br/&gt;VALUE: &lt;code&gt;sql|active_directory|radius|openid&lt;/code&gt;&lt;/dd&gt;&lt;/dl&gt;&lt;h4&gt;Configurable settings for RADIUS authentication &lt;/h4&gt;&lt;p&gt; &lt;b style&#x3D;&#39;color: red;&#39;&gt;&amp;nbsp;DEPRECATED&lt;/b&gt; These settings will be ignored. Please use /system/config/auth/radius API &lt;/p&gt;&lt;dl&gt;&lt;dt&gt;radius-ip&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;IP address of the RADIUS server&lt;br/&gt;VALUE: &lt;code&gt;IPv4 address&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;radius-port&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Port of the RADIUS server (usually 1812)&lt;br/&gt;VALUE: &lt;code&gt;Port&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;radius-sharedsec&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Shared Secret to access the RADIUS server&lt;br/&gt;VALUE: &lt;code&gt;Shared Secret&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;radius-otpPinFirst&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Sequence order of concatenated PIN and One-Time token&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;/dl&gt;&lt;/div&gt;
   * @param xSdsAuthToken Authentication token (required)
   * @return ConfigOptionList
   * @throws ApiException if fails to make API call
   */
  public ConfigOptionList getAuthSettings(String xSdsAuthToken) throws ApiException {
    Object localVarPostBody = null;
    
    // verify the required parameter 'xSdsAuthToken' is set
    if (xSdsAuthToken == null) {
      throw new ApiException(400, "Missing the required parameter 'xSdsAuthToken' when calling getAuthSettings");
    }
    
    // create path and map variables
    String localVarPath = "/config/authSettings";

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();


    if (xSdsAuthToken != null)
      localVarHeaderParams.put("X-Sds-Auth-Token", apiClient.parameterToString(xSdsAuthToken));

    
    final String[] localVarAccepts = {
      "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {
      
    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {  };

    GenericType<ConfigOptionList> localVarReturnType = new GenericType<ConfigOptionList>() {};
    return apiClient.invokeAPI(localVarPath, "GET", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
      }
  /**
   * Get system settings
   * &lt;div class&#x3D;\&quot;sds\&quot;&gt;&lt;p&gt;&lt;strong&gt;Functional Description:&lt;/strong&gt; &lt;br/&gt;DataSpace configuration entry point. Returns a list of configurable system settings.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Precondition:&lt;/strong&gt; Right \&quot;Config Read\&quot; required.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Effects:&lt;/strong&gt; None.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Further Information:&lt;/strong&gt;&lt;/p&gt;&lt;/div&gt;&lt;div class&#x3D;\&quot;sds\&quot;&gt;&lt;b&gt;Attention!&lt;/b&gt; If eula_active is true, but not accepted yet, or password must be changed, only the following two values are returned:&lt;ul&gt;&lt;li&gt;&lt;b&gt;allow_system_global_weak_password&lt;/b&gt;&lt;/li&gt;&lt;li&gt;&lt;b&gt;eula_active&lt;/b&gt;&lt;/li&gt;&lt;/ul&gt;&lt;h4&gt;Settings&lt;/h4&gt;&lt;h5&gt;Configurable settings&lt;/h5&gt;&lt;dl&gt;&lt;dt&gt;allow_system_global_weak_password &lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Allow weak password&lt;ul&gt;&lt;li&gt;A weak password has to fulfill the following criteria:&lt;br/&gt;- is at least 8 characters long&lt;br/&gt;- contains letters and numbers&lt;/li&gt;&lt;li&gt;A strong password has to fulfill the following criteria in addition:&lt;br/&gt;- contains at least one special character&lt;br/&gt;- contains upper and lower case characters&lt;/li&gt;&lt;/ul&gt;&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;branding_server_branding_id &lt;b style&#x3D;&#39;color: green;&#39;&gt;&amp;nbsp;NEW&lt;/b&gt;&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;The branding UUID, which corresponds to &lt;span style&#x3D;&#39;font-style: italic;&#39;&gt;BRANDING-QUALIFIER&lt;/span&gt; in the new branding server.&lt;br/&gt;VALUE: &lt;code&gt;String&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;branding_portal_url &lt;b style&#x3D;&#39;color: green;&#39;&gt;&amp;nbsp;NEW&lt;/b&gt;&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Access URL to to the Branding Portal.&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE &lt;code&gt;String&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;branding_server_customer&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;The UUID of the branding server customer, which corresponds to customer key in the branding server.&lt;br/&gt;VALUE: &lt;code&gt;String&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;branding_server_url&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Access URL to to the Branding Server.&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE &lt;code&gt;String&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;connect_as_drive&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Data Rooms can be mounted by WebDAV.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;dblog&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Write logs to local database&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;TRUE | FALSE&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_downloadshare_expiration_period&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Default expiration period for download shares in days.&lt;br/&gt;VALUE: &lt;code&gt;Integer between 0 and 9999&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_file_upload_expiration_date&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Default expiration period for all uploaded files in days. Set 0 to disable&lt;br/&gt;VALUE: &lt;code&gt;Integer between 0 and 9999&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_language&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Define which language should be default.&lt;br/&gt;VALUE: &lt;code&gt;cf. GET /auth/resources Model \&quot;Language\&quot;&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_uploadshare_expiration_period&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Default expiration period for upload shares in days.&lt;br/&gt;VALUE: &lt;code&gt;Integer between 0 and 9999&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;email_from&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Sender of system-generated emails&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE &lt;code&gt;Valid email address&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;email_to_sales&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Contact email address for customers to request more user licenses or data volume.&lt;br/&gt;VALUE &lt;code&gt;Valid email address&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;email_to_support&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Support email address for users.&lt;br/&gt;VALUE &lt;code&gt;Valid email address&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;enable_client_side_crypto&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Activation status of &lt;b&gt;Triple-Crypt&amp;trade; Technology&lt;/b&gt;. False by default.&lt;br/&gt; Can only be enabled once; disabling is not possible.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;eula_active&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Each user has to confirm the EULA at first login.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;eventlog_retention_period&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Retention period (in days) of event log entries. After that period, all entries are deleted.&lt;br/&gt;Recommended value: 7&lt;br/&gt;VALUE: &lt;code&gt;Integer between 0 and 9999&lt;/code&gt;&lt;br/&gt;VALUE: &lt;code&gt;0 no logs are deleted&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;file_size_js&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Maximum file size (in bytes) for downloads of encrypted files with JavaScript. Bigger files will require a JavaApplet.&lt;br/&gt; Recommended value: 10485760 (&#x3D;10MB)&lt;br/&gt;VALUE: &lt;code&gt;Integer&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;ip_address_logging&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Determines whether a user&#39;s IP address is logged on login.&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Email server to send emails&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;DNS name or IPv4 of an email server&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_authentication_necessary&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Set to true if the email server requires authentication.&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_password &lt;b style&#x3D;&#39;color: red;&#39;&gt;&amp;nbsp;Password is no longer returned.&lt;/b&gt;&lt;/dt&gt;&lt;dd&gt;&amp;nbsp;&lt;/dd&gt;&lt;dt&gt;mailserver_password_set&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Indicates if a password is set for the mailserver (because mailserver_password is always returned empty)&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_port&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Email server port&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;Positive number&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_username&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;User name for email server&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;User name for authentication&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_use_ssl&lt;dd&gt;&lt;br/&gt;Email server requires SSL connection?&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer.&lt;br/&gt;Requires mailserver_use_starttls to be false&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_use_starttls&lt;dd&gt;&lt;br/&gt;Email server requires StartTLS connection?&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;Requires mailserver_use_ssl to be false&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;syslog&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Write logs to a syslog interface&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;TRUE | FALSE&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;syslog_host&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Syslog Server (IP or FQDN)&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;DNS name or IPv4 of a syslog server&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;syslog_port&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Syslog server port&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;Positive Number&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;syslog_protocol&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Protocol to connect to syslog server&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE: &lt;code&gt;TCP | UDP&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;system_name&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;System name&lt;br/&gt;VALUE: &lt;code&gt;Display name of the Data Space&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;enable_email_notification_button&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Enable Mail Notification Button&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;allow_share_password_sms&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Allow sending of Share passwords via SMS&lt;br/&gt;VALUE: &lt;code&gt;TRUE | FALSE&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;globally_allow_share_password_sms&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Allow sending of Share passwords via SMS&lt;br/&gt;Read only&lt;br/&gt;VALUE: &lt;code&gt;TRUE | FALSE&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;use_s3_storage&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Defines if s3 is used as storage backend&lt;br/&gt;Read only&lt;br/&gt;VALUE: &lt;code&gt;TRUE | FALSE&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;s3_default_region&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Suggested S3 Region&lt;br/&gt;Read only&lt;br/&gt;VALUE: &lt;code&gt; Region name&lt;/code&gt;&lt;/dd&gt;&lt;/dl&gt;&lt;/div&gt;
   * @param xSdsAuthToken Authentication token (required)
   * @return ConfigOptionList
   * @throws ApiException if fails to make API call
   */
  public ConfigOptionList getSystemSettings(String xSdsAuthToken) throws ApiException {
    Object localVarPostBody = null;
    
    // verify the required parameter 'xSdsAuthToken' is set
    if (xSdsAuthToken == null) {
      throw new ApiException(400, "Missing the required parameter 'xSdsAuthToken' when calling getSystemSettings");
    }
    
    // create path and map variables
    String localVarPath = "/config/settings";

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();


    if (xSdsAuthToken != null)
      localVarHeaderParams.put("X-Sds-Auth-Token", apiClient.parameterToString(xSdsAuthToken));

    
    final String[] localVarAccepts = {
      "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {
      
    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {  };

    GenericType<ConfigOptionList> localVarReturnType = new GenericType<ConfigOptionList>() {};
    return apiClient.invokeAPI(localVarPath, "GET", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
      }
  /**
   * Change authentication setting
   * &lt;div class&#x3D;\&quot;sds\&quot;&gt;&lt;p&gt;&lt;strong&gt;Functional Description:&lt;/strong&gt; &lt;br/&gt;Change one or more settings of authentication configuration.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Precondition:&lt;/strong&gt; Right \&quot;Config Change\&quot; required.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Effects:&lt;/strong&gt; One or more global authentication setting gets changed.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Further Information:&lt;/strong&gt; None.&lt;/p&gt;&lt;/div&gt;&lt;div class&#x3D;\&quot;sds\&quot;&gt;&lt;b&gt;Configuration settings for various authentication methods&lt;/b&gt;&lt;h4&gt;Authentication Methods&lt;/h4&gt;&lt;dl&gt;&lt;dt&gt;sql&lt;/dt&gt;&lt;dd&gt;&lt;b&gt;Basic Authentication globally allowed.&lt;/b&gt;&lt;br/&gt;This option must be activated to allow users to log in with their credentials stored in the database.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;active_directory&lt;/dt&gt;&lt;dd&gt;&lt;b&gt;Active Directory Authentication globally allowed&lt;/b&gt;&lt;br/&gt;This option must be activated to allow users to log in with their Active Directory credentials.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;radius&lt;/dt&gt;&lt;dd&gt;&lt;b&gt;RADIUS Authentication globally allowed&lt;/b&gt;&lt;br/&gt;This option must be activated to allow users to log in with their RADIUS username, their PIN and a token password.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;openid&lt;/dt&gt;&lt;dd&gt;&lt;b&gt;OpenID Connect Authentication globally allowed&lt;/b&gt;&lt;br/&gt;This option must be activated to allow users to log in with their OpenID Connect identity.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_auth_method&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;&lt;b&gt;Default authentication method without user context&lt;/b&gt;&lt;br/&gt;If this option is set, the chosen method will be provided as default authentication method if no user context is available.&lt;br/&gt;Only one authentication method can be set and it must be allowed (see above).&lt;br/&gt;If no value is set, there is no guarantee about the order of the returned methods.&lt;br/&gt;Only activated authentication methods may be set as default authentication method.&lt;br/&gt;VALUE: &lt;code&gt;sql|active_directory|radius|openid&lt;/code&gt;&lt;/dd&gt;&lt;/dl&gt;&lt;h4&gt;Configurable settings for RADIUS authentication &lt;/h4&gt;&lt;p&gt; &lt;b style&#x3D;&#39;color: red;&#39;&gt;&amp;nbsp;DEPRECATED&lt;/b&gt; These settings will be ignored. Please use /system/config/auth/radius API &lt;/p&gt;&lt;dl&gt;&lt;dt&gt;radius-ip&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;IP address of the RADIUS server&lt;br/&gt;VALUE: &lt;code&gt;IPv4 address&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;radius-port&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Port of the RADIUS server (usually 1812)&lt;br/&gt;VALUE: &lt;code&gt;Port&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;radius-sharedsec&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Shared Secret to access the RADIUS server&lt;br/&gt;VALUE: &lt;code&gt;Shared Secret&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;radius-otpPinFirst&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Sequence order of concatenated PIN and One-Time token&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;/dl&gt;&lt;/div&gt;
   * @param xSdsAuthToken Authentication token (required)
   * @param body  (required)
   * @throws ApiException if fails to make API call
   */
  public void setAuthSettings(String xSdsAuthToken, ConfigOptionList body) throws ApiException {
    Object localVarPostBody = body;
    
    // verify the required parameter 'xSdsAuthToken' is set
    if (xSdsAuthToken == null) {
      throw new ApiException(400, "Missing the required parameter 'xSdsAuthToken' when calling setAuthSettings");
    }
    
    // verify the required parameter 'body' is set
    if (body == null) {
      throw new ApiException(400, "Missing the required parameter 'body' when calling setAuthSettings");
    }
    
    // create path and map variables
    String localVarPath = "/config/authSettings";

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();


    if (xSdsAuthToken != null)
      localVarHeaderParams.put("X-Sds-Auth-Token", apiClient.parameterToString(xSdsAuthToken));

    
    final String[] localVarAccepts = {
      "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {
      
    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {  };


    apiClient.invokeAPI(localVarPath, "PUT", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, null);
  }
  /**
   * Change system setting
   * &lt;div class&#x3D;\&quot;sds\&quot;&gt;&lt;p&gt;&lt;strong&gt;Functional Description:&lt;/strong&gt; &lt;br/&gt;DataSpace configuration entry point. Returns a list of configurable settings.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Precondition:&lt;/strong&gt; Right \&quot;Config Change\&quot; required.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Effects:&lt;/strong&gt; One or more global authentication setting gets changed.&lt;/p&gt;&lt;p&gt;&lt;strong&gt;Further Information:&lt;/strong&gt;&lt;/p&gt;&lt;/div&gt;&lt;div class&#x3D;\&quot;sds\&quot;&gt;&lt;b&gt;Attention!&lt;/b&gt; Only visible for Data Space Admin of Provider-Customer&lt;h4&gt;Settings&lt;/h4&gt;&lt;h5&gt;Configurable settings&lt;/h5&gt;&lt;dl&gt;&lt;dt&gt;allow_system_global_weak_password&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Allow weak password&lt;ul&gt;&lt;li&gt;A weak password has to fulfil the following criteria:&lt;br/&gt;- is at least 8 characters long&lt;br/&gt;- contains letters and numbers&lt;/li&gt;&lt;li&gt;A strong password has to fulfil the following criteria in addition:&lt;br/&gt;- contains at least one special character&lt;br/&gt;- contains upper and lower case characters&lt;/li&gt;&lt;/ul&gt;&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;branding_server_branding_id &lt;b style&#x3D;&#39;color: green;&#39;&gt;&amp;nbsp;NEW&lt;/b&gt;\&quot;&lt;dd&gt;&lt;br/&gt;The branding UUID, which corresponds to BRANDING-QUALIFIER in the new branding server.&lt;br/&gt;&lt;strong&gt;Effects:&lt;/strong&gt;&lt;br/&gt; If &lt;span style&#x3D;&#39;font-style: italic;&#39;&gt;branding_id&lt;/span&gt; is set, &lt;span style&#x3D;&#39;font-style: italic;&#39;&gt;branding_server_customer&lt;/span&gt; is not used.&lt;br/&gt;If it needs to be reset, send an empty string&lt;br/&gt;&lt;br/&gt;VALUE: &lt;code&gt;String&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;branding_portal_url &lt;b style&#x3D;&#39;color: green;&#39;&gt;&amp;nbsp;NEW&lt;/b&gt;&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Access URL to to the Branding Portal.&lt;br/&gt;Only visible for Data Space Admin of Provider-Customer&lt;br/&gt;VALUE &lt;code&gt;String&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;branding_server_customer&lt;dd&gt;&lt;br/&gt;The UUID of the branding server customer, which corresponds to customer key in the branding server.&lt;br/&gt;VALUE: &lt;code&gt;String&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;branding_server_url&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Access URL to to the Branding Server.&lt;br/&gt;VALUE &lt;code&gt;String&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;connect_as_drive&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Data Rooms can be mounted by WebDAV.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;dblog&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Write logs to local database&lt;br/&gt;VALUE: &lt;code&gt;TRUE | FALSE&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_downloadshare_expiration_period&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Default expiration period for download shares in days.&lt;br/&gt;VALUE: &lt;code&gt;Integer between 0 and 9999&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_file_upload_expiration_date&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Default expiration period for all uploaded files in days. Set 0 to disable&lt;br/&gt;VALUE: &lt;code&gt;Integer between 0 and 9999&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_language&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Define which language should be default.&lt;br/&gt;VALUE: &lt;code&gt;cf. GET /auth/resources Model \&quot;Language\&quot;&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;default_uploadshare_expiration_period&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Default expiration period for upload shares in days.&lt;br/&gt;VALUE: &lt;code&gt;Integer between 0 and 9999&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;email_from&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Sender of system-generated emails&lt;br/&gt;VALUE &lt;code&gt;Valid email address&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;email_to_sales&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Contact email address for customers to request more user licenses or data volume.&lt;br/&gt;VALUE &lt;code&gt;Valid email address&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;email_to_support&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Support email address for users.&lt;br/&gt;VALUE &lt;code&gt;Valid email address&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;enable_client_side_crypto&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Activation status of &lt;b&gt;Triple-Crypt&amp;trade; Technology&lt;/b&gt;. False by default.&lt;br/&gt; Can only be enabled once; disabling is not possible.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;eula_active&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Each user has to confirm the EULA at first login.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;eventlog_retention_period&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Retention period (in days) of event log entries. After that period, all entries are deleted.&lt;br/&gt;Recommended value: 7&lt;br/&gt;VALUE: &lt;code&gt;Integer between 0 and 9999&lt;/code&gt;&lt;br/&gt;VALUE: &lt;code&gt;0 no logs are deleted&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;file_size_js&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Maximum file size (in bytes) for downloads of encrypted files with JavaScript. Bigger files will require a JavaApplet.&lt;br/&gt; Recommended value: 10485760 (&#x3D;10MB)&lt;br/&gt;VALUE: &lt;code&gt;Integer&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;ip_address_logging&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Determines whether a user&#39;s IP address is logged on login.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Email server to send emails&lt;br/&gt;VALUE: &lt;code&gt;DNS name or IPv4 of an email server&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_authentication_necessary&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Set to true if the email server requires authentication.&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_password&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Password for email server&lt;br/&gt;VALUE: &lt;code&gt;Password for authentication&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_port&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Email server port&lt;br/&gt;VALUE: &lt;code&gt;Positive number&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_username&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;User name for email server&lt;br/&gt;VALUE: &lt;code&gt;User name for authentication&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;mailserver_use_ssl&lt;dd&gt;&lt;br/&gt;Email server requires ssl connection?&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;syslog&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Write logs to a syslog interface&lt;br/&gt;VALUE: &lt;code&gt;TRUE | FALSE&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;syslog_host&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Syslog Server (IP or FQDN)&lt;br/&gt;VALUE: &lt;code&gt;DNS name or IPv4 of a syslog server&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;syslog_port&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Syslog server port&lt;br/&gt;VALUE: &lt;code&gt;Positive Number&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;syslog_protocol&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Protocol to connect to syslog server&lt;br/&gt;VALUE: &lt;code&gt;TCP | UDP&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;system_name&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;System name&lt;br/&gt;VALUE: &lt;code&gt;Display name of the Data Space&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;enable_email_notification_button&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Enable Mail Notification Button&lt;br/&gt;VALUE: &lt;code&gt;true|false&lt;/code&gt;&lt;/dd&gt;&lt;dt&gt;allow_share_password_sms&lt;/dt&gt;&lt;dd&gt;&lt;br/&gt;Allow sending of Share passwords via SMS&lt;br/&gt;VALUE: &lt;code&gt;TRUE | FALSE&lt;/code&gt;&lt;/dd&gt;&lt;/dl&gt;&lt;/div&gt;
   * @param xSdsAuthToken Authentication token (required)
   * @param body  (required)
   * @throws ApiException if fails to make API call
   */
  public void setSystemSetting(String xSdsAuthToken, ConfigOptionList body) throws ApiException {
    Object localVarPostBody = body;
    
    // verify the required parameter 'xSdsAuthToken' is set
    if (xSdsAuthToken == null) {
      throw new ApiException(400, "Missing the required parameter 'xSdsAuthToken' when calling setSystemSetting");
    }
    
    // verify the required parameter 'body' is set
    if (body == null) {
      throw new ApiException(400, "Missing the required parameter 'body' when calling setSystemSetting");
    }
    
    // create path and map variables
    String localVarPath = "/config/settings";

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();


    if (xSdsAuthToken != null)
      localVarHeaderParams.put("X-Sds-Auth-Token", apiClient.parameterToString(xSdsAuthToken));

    
    final String[] localVarAccepts = {
      "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {
      
    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {  };


    apiClient.invokeAPI(localVarPath, "PUT", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, null);
  }
}

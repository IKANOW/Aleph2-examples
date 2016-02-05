#Aleph2_Web_SSO 

For further detailed configuration please refer to  `buji-pac4j-demo` project [buji-pac4j](https://github.com/pac4j/buji-pac4j-demo) .
 

## Configuration

Build the project and launch the web app within tomcat 7.

### SAML configuration

Place a copy of the shiro.ini file inside the folder

    /opt/aleph2-home/etc/aleph2_web_sso

####SAML host:port configuration in shiro.ini

Configure the service provider entity ID:

    saml2Config.serviceProviderEntityId = http://localhost:8080/aleph2_web_sso/callback?client_name=SAML2Client

Configure the SAML callback URL - replace 'localhost:8080' with your host:port .
    
    saml2Client.callbackUrl=http://localhost:8080/aleph2_web_sso/callback?client_name=SAML2Client

Configure the pac4j-client/shiro callback URL - replace 'localhost:8080' with your host:port .
 
     clients.callbackUrl = http://localhost:8080/aleph2_web_sso/callback


####SAML keystore configuration in shiro.ini
Currently the samlKeystore.jks and the dp-metadata.xml files are compiled and placed inside inside the war.
However they could be placed in a folder on the classpath, e.g. inside 
     
     opt/aleph2-home/etc/aleph2_web_sso
      
The keystore and the metadata files would have to be renamed to avoid conflicts with files within the war.

This is the saml keystore config section for the service provider (aleph2_web_sso) containing the private key for the SP.

    saml2Config.keystorePath = resource:samlKeystore.jks
    saml2Config.keystorePassword=welcome1
    saml2Config.privateKeyPassword=welcome1_key

The idp-metadata.xml contains the Shibboleth IDP metadata configuration, pointing to shibboleth IDP.

    saml2Config.identityProviderMetadataPath = resource:idp-metadata.xml


The service provider metadata file (sp-metadata.xml) is created on startup of the application, default is  inside the tomcat home folder.
It can then be supplied to the the IDP server for configuration. 

    saml2Config.serviceProviderMetadataPath = sp-metadata.xml


### Aleph2 services configuration

Place a copy of the aleph2_web_sso.properties file inside /opt/aleph2-home/etc/aleph2_web_sso




###LDAP configuration in shiro.ini
This is a sample configuration binding directly to an entry using the Format resolver. For further configuration please refer to the 
[ldaptive library documentation](http://www.ldaptive.org/docs/guide/authentication.html)
 
   
    dnResolver=org.ldaptive.auth.FormatDnResolver
    dnResolver.format=cn=%s,ou=users,ou=aleph2,dc=ikanow,dc=com

    ldapConnectionConfig.ldapUrl=ldap://localhost:10389


Change the host and port of the url pointing to the form used by ldap to login: 
    
    formClient.loginUrl = http://localhost:8080/aleph2_web_sso/loginForm.jsp
    
    
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


####IDP-metadata configuration in shiro.ini
The  IDP's idp-metadata.xml file needs to be copied in a path on the classpath (configured in config.xml) currently here:
     
     /opt/aleph2-home/etc/aleph2_web_sso/idp-metadata.xml
      
_For a Shibboleth based Identity provider the idp-metadata.xml file can be found in /opt/shibboleth-idp/metadata/idp-metadata.xml on the shibboleth IDP host.
 
####SAML keystore configuration in shiro.ini

The keystore file file needs to be created and placed in a path on the classpath (configured in config.xml) currently here:

     /opt/aleph2-home/etc/aleph2_web_sso/samlKeystore.jks

In order to fill the java keystore file one needs to create a  a keypair and a certificate in the PKCS12 format.

_HOWTO TBD_

The following command uses the java command keytool to import the certificate into the keystore

    keytool -importkeystore -deststorepass welcome1 -destkeystore samlKeystore.jks -destkeypass welcome1_key -srckeystore ikanow-sp.p12 -srcstoretype PKCS12

This is the saml keystore config section for the service provider (aleph2_web_sso) containing the private key for the SP.

    saml2Config.keystorePath = resource:samlKeystore.jks
    saml2Config.keystorePassword=welcome1
    saml2Config.privateKeyPassword=welcome1_key

The idp-metadata.xml contains the Shibboleth IDP metadata configuration, pointing to shibboleth IDP.

    saml2Config.identityProviderMetadataPath = resource:idp-metadata.xml


####SP metadata file
The service provider metadata file (sp-metadata.xml) is created on deployment of the application (even if  is not fully configured), the default is  inside the tomcat home folder,e.g.

	cd ~tomcat
	ls

example location:

	/usr/share/tomcat7/sp-metadata.xml



   
It can then be supplied to the the IDP server for configuration. 

    saml2Config.serviceProviderMetadataPath = sp-metadata.xml

####SSO urls and OIDs in shiro.ini
Configure the Single Sign ON specific URLs and attribute OIDs:

    aleph2WebSsoConfig = com.ikanow.aleph2_web_sso.utils.Aleph2WebSsoConfig

If a user does not exist in V1 he can be created by enabling setting the _aleph2WebSsoConfig.createUser_ setting to true. 
In this case either the saml2 profile needs to contain the data (attributes) to create the user or the attributes need to be configured separately (see below.)
  
    aleph2WebSsoConfig.createUser=true
    aleph2WebSsoConfig.logoutUrl=http://idp001.dev.ikanow.com:8080/idp/profile/Logout
    aleph2WebSsoConfig.apiRootUrl=http://api001.dev.ikanow.com:8080/api/

Set useProfile setting to true if the Saml profile received from the IDP contains username, email, first,last-name attributes.
  
     aleph2WebSsoConfig.useProfile=false


If attributes are received from the IDP, (vs using the Profile only) then configure the Attribute OIDs to match the IDP setting

    aleph2WebSsoConfig.useAttributes=true
    aleph2WebSsoConfig.emailOid=urn:oid:0.9.2342.19200300.100.1.3
    aleph2WebSsoConfig.uidOid=urn:oid:0.9.2342.19200300.100.1.1
    aleph2WebSsoConfig.firstNameOid=urn:oid:2.5.4.42
    aleph2WebSsoConfig.lastnameOid=urn:oid:2.5.4.4
    aleph2WebSsoConfig.fullNameOid=urn:oid:2.16.840.1.113730.3.1.241
    aleph2WebSsoConfig.phoneOid=urn:oid:2.5.4.20 


### Aleph2 services configuration

Place a copy of the aleph2_web_sso.properties file inside /opt/aleph2-home/etc/aleph2_web_sso .<br/>
Edit the mongodb connection, e.g.
     
    MongoDbManagementDbService.mongodb_connection=db001.dev.ikanow.com:27017

<br>

----

<br>

#LDAP configuration in shiro.ini (Informative only, not used for saml based SSO) 
This is a sample configuration binding directly to an entry using the Format resolver. For further configuration please refer to the 
[ldaptive library documentation](http://www.ldaptive.org/docs/guide/authentication.html)
 
   
    dnResolver=org.ldaptive.auth.FormatDnResolver
    dnResolver.format=cn=%s,ou=users,ou=aleph2,dc=ikanow,dc=com

    ldapConnectionConfig.ldapUrl=ldap://localhost:10389


Change the host and port of the url pointing to the form used by ldap to login: 
    
    formClient.loginUrl = http://localhost:8080/aleph2_web_sso/loginForm.jsp
    
    
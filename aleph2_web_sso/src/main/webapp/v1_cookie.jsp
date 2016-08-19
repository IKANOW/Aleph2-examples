<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="include.jsp"%>
<%@ page import="com.google.inject.Injector"%>
<%-- page import="org.apache.shiro.subject.support.DefaultSubjectContext"--%>
<%-- page import="org.apache.shiro.subject.SimplePrincipalCollection"--%>
<%@ page import="org.apache.logging.log4j.Logger"%>
<%@ page import="org.apache.logging.log4j.LogManager"%>
<%@ page import="com.ikanow.aleph2.security.web.*"%>
<%@ page import="org.pac4j.core.profile.CommonProfile" %>
<%@ page import="org.pac4j.saml.profile.SAML2Profile" %>
<%@ page import="org.pac4j.ldap.profile.LdapProfile" %>
<%@page import="org.apache.shiro.subject.Subject"%>
<%@page import="org.apache.shiro.SecurityUtils"%>
<%@page import="java.util.ArrayList"%>
<%@ page import="com.ikanow.aleph2.security.web.CSCACookieAuthentication"%>
<%@ page import="com.ikanow.aleph2_web_sso.utils.*"%>

<shiro:authenticated>

<%

String toReturn = "{\"response\":{\"action\":\"LDAP Login\",\"success\":true,\"message\":\"LDAP Login Success. Default Response\"}}";

ServletContext sc = session.getServletContext();
Injector injector = (Injector)sc.getAttribute("com.google.inject.Injector");
CSCACookieAuthentication cookieAuth = CSCACookieAuthentication.getInstance(injector);
final Logger logger = LogManager.getLogger(this.getClass());

Subject subject = SecurityUtils.getSubject();
String email = "";
String uid = null;
LdapProfile lp = null;
SAML2Profile sp = null;
String firstName = null;
String lastName = null;
String phone = null;
String adminAttribute = null;
String adminAttributeContainsValue = null;
String userType	= "user";

int nClientPort = request.getServerPort();
if(subject.getPrincipals()!=null && subject.getPrincipals().asList().size()>1){
	Object pP = subject.getPrincipals().asList().get(1);
	if(pP instanceof SAML2Profile){
		logger.debug("SAML Profile Received");
		sp = (SAML2Profile)pP;
	}
	else if (pP instanceof LdapProfile)
	{
		logger.debug("LDAP Profile Received");
		lp = (LdapProfile)pP;
	}
	else
	{
		logger.error("Unrecognized Principal Type: " + pP.getClass());
	}
}
else
{
	logger.error("User had no Principals");
}

if(Aleph2WebSsoConfig.getInstance().isUseProfile()){	
	logger.debug("isUseProfile enabled");
	uid = lp.getUsername();
	email = lp.getEmail();
	firstName = lp.getFirstName();
	lastName = lp.getFamilyName();
}

if(Aleph2WebSsoConfig.getInstance().isUseAttributes()){
	logger.debug("isUseAttributes enabled");
	if (null != lp)
	{
		uid = Aleph2WebSsoUtils.extractAttribute(lp, Aleph2WebSsoConfig.getInstance().getUidOid());
		logger.debug("Result from " + Aleph2WebSsoConfig.getInstance().getUidOid() + " was: " + uid);
		email = Aleph2WebSsoUtils.extractAttribute(lp, Aleph2WebSsoConfig.getInstance().getEmailOid());
		logger.debug("Result from " + Aleph2WebSsoConfig.getInstance().getEmailOid() + " was: " + email);
		firstName = Aleph2WebSsoUtils.extractAttribute(lp, Aleph2WebSsoConfig.getInstance().getFirstNameOid());
		logger.debug("Result from " + Aleph2WebSsoConfig.getInstance().getFirstNameOid() + " was: " + firstName);
		lastName = Aleph2WebSsoUtils.extractAttribute(lp, Aleph2WebSsoConfig.getInstance().getLastnameOid());
		logger.debug("Result from " + Aleph2WebSsoConfig.getInstance().getLastnameOid() + " was: " + lastName);
		//try {
		//	phone = Aleph2WebSsoUtils.extractAttribute(cp, Aleph2WebSsoConfig.getInstance().getPhoneOid());
		//} catch (NullPointerException npe) {}
		
		try {
			if (null != Aleph2WebSsoConfig.getInstance().getAdminAttribute() && null != Aleph2WebSsoConfig.getInstance().getAdminAttributeContainsValue())
			{
				adminAttribute = Aleph2WebSsoUtils.extractAttribute(lp, Aleph2WebSsoConfig.getInstance().getAdminAttribute());
				logger.debug("Result from " + Aleph2WebSsoConfig.getInstance().getAdminAttribute() + " was: " + adminAttribute);
				adminAttributeContainsValue = Aleph2WebSsoConfig.getInstance().getAdminAttributeContainsValue();
				logger.debug("AdminAttributeContainsValue = " + adminAttributeContainsValue);
			}
			else
			{
				logger.debug("Missing Admin Attribute information. No Admin election will be performed.");
			}
		} catch (NullPointerException npe) {
			logger.debug("NPE: Missing Admin Attribute information. No Admin election will be performed.");
		}
			
		if (null != adminAttribute && null != adminAttributeContainsValue)
		{
			if (adminAttribute.contains(adminAttributeContainsValue))
			{
				userType = "admin";
				logger.debug("User matches Admin Attribute");
			}
			else
			{
				logger.debug("User does not match Admin Attribute");
			}
		}
	}
	else
	{
		logger.warn("Profile was NULL");
	}	
}

CookieBean cb = cookieAuth.createCookieByEmail(email);
if(cb==null){
	// check of we want to create the user
	if(Aleph2WebSsoConfig.getInstance().isCreateUser()){
		// second attempt, creating user
		cookieAuth.setApiRootUrl(Aleph2WebSsoConfig.getInstance().getApiRootUrl());
		cb = cookieAuth.createUser(uid, email, firstName, lastName, phone, userType);
		if(cb!=null){
			logger.debug("New User Created");
		}
		else{
			logger.error("Unable to create user. uid=" + uid + " email=" + email + " firstName=" + firstName + " lastName=" + lastName + " userType=" + userType);	
			toReturn = "{\"response\":{\"action\":\"LDAP Login\",\"success\":false,\"message\":\"LDAP Login Failed to Create User\"}}";		
		}
	}
}
else
{
	if(Aleph2WebSsoConfig.getInstance().isCreateUser()){
		cookieAuth.setApiRootUrl(Aleph2WebSsoConfig.getInstance().getApiRootUrl());
		// update cookie in case admin status changed
		cb = cookieAuth.updateUser(uid, email, firstName, lastName, phone, userType);
		if(cb!=null){
			logger.debug("Existing User Updated");
		}
		else{
			logger.error("User not created");	
			toReturn = "{\"response\":{\"action\":\"LDAP Login\",\"success\":false,\"message\":\"LDAP Login Failed to Update User\"}}";		
		}
	}
}

if(cb!=null){
	// Create cookies for first and last names.      
	Cookie infiniteCookie = new Cookie("infinitecookie",  cb.getCookieId());
	infiniteCookie.setPath("/");
	//infiniteCookie.setAccessRestricted(true);
	//Indicates whether to restrict cookie access to untrusted parties. Currently this toggles the non-standard but widely supported HttpOnly cookie parameter. 
	infiniteCookie.setHttpOnly(true);

	if ((443 == nClientPort) || (8443 == nClientPort)) {
		infiniteCookie.setSecure(true);
	}
	// Add both the cookies in the response header.
	response.addCookie( infiniteCookie );
	logger.debug("User Cookie Created");
	toReturn = "{\"response\":{\"action\":\"LDAP Login\",\"success\":true,\"message\":\"LDAP Login Success. Cookie Created Successfully.\"}}";

}// if cb
else{
	logger.error("User Cookie Not Created");
	toReturn = "{\"response\":{\"action\":\"LDAP Login\",\"success\":false,\"message\":\"LDAP Login Error: Unable to create session\"}}";
}
 
out.print(toReturn);
String url = null;
Cookie[] cookies  = request.getCookies();
if(cookies!=null){
for (Cookie c: cookies) {
    if (c.getName().equals("return_url")) {
        if (c.getValue().startsWith("return_url")) {
            url = java.net.URLDecoder.decode(c.getValue().substring(11)); //(11=="return_url="
        }
    }
}
}
if (null != url) {
response.setStatus(response.SC_MOVED_TEMPORARILY);
response.setHeader("Location", url);
return;
}
%>


</shiro:authenticated>

<shiro:notAuthenticated>
{"response":{"action":"LDAP Login","success":false,"message":"LDAP Login Issue"}}
</shiro:notAuthenticated>

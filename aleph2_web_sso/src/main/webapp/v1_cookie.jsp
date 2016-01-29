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
<%@ include file="include.jsp"%>
<%@ page import="com.google.inject.Injector"%>
<%-- page import="org.apache.shiro.subject.support.DefaultSubjectContext"--%>
<%-- page import="org.apache.shiro.subject.SimplePrincipalCollection"--%>
<%@ page import="com.ikanow.aleph2.security.web.*"%>
<%@ page import="org.pac4j.saml.profile.SAML2Profile" %>
<%@page import="org.apache.shiro.subject.Subject"%>
<%@page import="org.apache.shiro.SecurityUtils"%>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="style.css"/>
    <title>Cookie</title>
</head>
<body>

<shiro:authenticated>
<h2>You are authenticated!</h2>
<%
     ServletContext sc = session.getServletContext();
Injector injector = (Injector)sc.getAttribute("com.google.inject.Injector");
IkanowV1CookieAuthentication cookieAuth = IkanowV1CookieAuthentication.getInstance(injector);

Subject subject = SecurityUtils.getSubject();
String email = "";
int nClientPort = request.getServerPort();
if(subject.getPrincipals()!=null && subject.getPrincipals().asList().size()>1){
	Object pP = 	subject.getPrincipals().asList().get(1);
	if(pP instanceof SAML2Profile){
		SAML2Profile sp = (SAML2Profile)pP;
		email = sp.getEmail();
		out.print("Saml2Profile: "+sp);
		out.print("<br/>");
		out.print("Email: "+email);		
	}

CookieBean cb = cookieAuth.createCookieByEmail(email);

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
} 

 %>
 <%=email %>
 <%=nClientPort %>
</shiro:authenticated>

<shiro:notAuthenticated>
<h2>Not authenticated!</h2>
</shiro:notAuthenticated>

</body>
</html>

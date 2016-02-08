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
<%@page import="java.util.ArrayList"%>

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
		// todo test
		if(email == null){
			Object emailAttributes = sp.getAttribute("urn:oid:0.9.2342.19200300.100.1.3");
			if(emailAttributes instanceof ArrayList){
				ArrayList emails = (ArrayList)emailAttributes;
				if(emails.size()>0){
					email = ""+emails.get(0);		
					out.print("<br/>Using Email Attribute (urn:oid:0.9.2342.19200300.100.1.3) instead of email :"+email+"<br/>");
				}
			}			
		}
	}

CookieBean cb = cookieAuth.createCookieByEmail(email);
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
out.print("<h3>V1 cookie created!</h3>");
}else{
	out.print("<h3>Sorry, V1 cookie not created, check the log!</h3>");
}

}// if cb
 out.print("profile :"+subject.getPrincipals());
 %>
 <br/>
Email:<%=email %>
<br/>
ClientPort: <%=nClientPort %>

<%
String url = null;
for (Cookie c: request.getCookies()) {
    if (c.getName().equals("return_url")) {
        if (c.getValue().startsWith("return_url")) {
            url = java.net.URLDecoder.decode(c.getValue().substring(11)); //(11=="return_url="
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
<h2>Not authenticated!</h2>
</shiro:notAuthenticated>

<br />
<a href="index.jsp">home</a>
<br />
<a href="logout">logout</a>
<br/>
<a href="http://idp001.dev.ikanow.com:8080/idp/profile/Logout">IDP logout (hardcoded for testing)</a>
<br/>


</body>
</html>

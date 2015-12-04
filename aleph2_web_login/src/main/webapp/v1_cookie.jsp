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
<%@ page import="com.google.inject.Injector,com.ikanow.aleph2.security.service.IkanowV1CookieAuthentication" %>
<%@ include file="include.jsp"%>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="<c:url value="/style.css"/>"/>
    <title>Cookie</title>
</head>
<body>

<%     ServletContext sc = session.getServletContext();
	   Injector injector = (Injector)sc.getAttribute("com.google.inject.Injector");
	   IkanowV1CookieAuthentication cookieAuth = IkanowV1CookieAuthentication.getInstance(injector);

     %>
<shiro:authenticated>
<h2>You are authenticated!</h2>
<%
// Create cookies for first and last names.      
Cookie infiniteCookie = new Cookie("infinite.cookie",  "CookieValue");
infiniteCookie.setPath("/");

//


     


// Add both the cookies in the response header.
response.addCookie( infiniteCookie );
   %>


</shiro:authenticated>
<shiro:notAuthenticated>
<h2>Not authenticated!</h2>
</shiro:notAuthenticated>

<h2>Injector<%=""+injector %></h2>
</body>
</html>

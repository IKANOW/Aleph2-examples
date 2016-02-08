<%@ page import="com.google.inject.Injector"%>
<%@ page import="com.ikanow.aleph2.security.web.*"%>

<%
    // Get and delete v1 cookie

    for (Cookie c: request.getCookies()) {
        if (c.getName().equals("infinitecookie")) {
            ServletContext sc = session.getServletContext();
            Injector injector = (Injector)sc.getAttribute("com.google.inject.Injector");
            IkanowV1CookieAuthentication cookieAuth = IkanowV1CookieAuthentication.getInstance(injector);
            cookieAuth.deleteSessionCookieInDbById(c.getValue());
        }
    }
%>

<%
    // Logout from IDP

    //TODO: get this from the config
    final String idp_logout = "http://idp001.dev.ikanow.com:8080/idp/profile/Logout";

    response.setStatus(response.SC_MOVED_TEMPORARILY);
    response.setHeader("Location", idp_logout);
%>
~

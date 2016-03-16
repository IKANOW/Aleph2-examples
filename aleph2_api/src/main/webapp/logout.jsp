<%@ page import="com.google.inject.Injector"%>
<%@ page import="com.ikanow.aleph2.security.web.*"%>
<%-- @ page import="com.ikanow.aleph2_api.utils.*"--%>

<%
// Get and delete v1 cookie
Cookie[] cookies = request.getCookies();
if(cookies!=null){
    for (Cookie c: cookies) {
        if (c.getName().equals("infinitecookie")) {
            ServletContext sc = session.getServletContext();
            Injector injector = (Injector)sc.getAttribute("com.google.inject.Injector");
            IkanowV1CookieAuthentication cookieAuth = IkanowV1CookieAuthentication.getInstance(injector);
            cookieAuth.deleteSessionCookieInDbById(c.getValue());
        }
    }
}
%>

<%
    // Logout from IDP

    //TODO: get this from the config
 //final String idp_logout = Aleph2WebSsoConfig.getInstance().getLogoutUrl();//"http://idp001.dev.ikanow.com:8080/idp/profile/Logout";

 //   response.setStatus(response.SC_MOVED_TEMPORARILY);
    //response.setHeader("Location", idp_logout);
%>
~

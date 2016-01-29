<%@page import="org.pac4j.core.exception.RequiresHttpAction"%>
<%@page import="org.apache.shiro.subject.Subject"%>
<%@page import="org.apache.shiro.SecurityUtils"%>
<%@page import="org.pac4j.demo.shiro.util.WebUtils"%>
<%@page import="io.buji.pac4j.context.session.ShiroSessionStore"%>
<%@page import="org.pac4j.http.client.indirect.*"%>
<%@page import="org.pac4j.oauth.client.*"%>
<%@page import="org.pac4j.cas.client.*"%>
<%@page import="org.pac4j.saml.client.*"%>
<%@ page import="org.pac4j.core.client.Clients" %>
<%@ page import="org.pac4j.core.context.J2EContext" %>
<%@ page import="org.pac4j.saml.profile.SAML2Profile" %>

<%
	J2EContext context = new J2EContext(request, response, new ShiroSessionStore());
	Clients clients = WebUtils.getObject(pageContext, Clients.class, "clients");
	Subject subject = SecurityUtils.getSubject();
	FacebookClient fbClient = (FacebookClient) clients.findClient("FacebookClient");
	TwitterClient twClient = (TwitterClient) clients.findClient("TwitterClient");
	FormClient formClient = (FormClient) clients.findClient("FormClient");
	IndirectBasicAuthClient baClient = (IndirectBasicAuthClient) clients.findClient("IndirectBasicAuthClient");
	CasClient casClient = (CasClient) clients.findClient("CasClient");
	VkClient vkClient = (VkClient) clients.findClient("VkClient");
	SAML2Client saml2Client = (SAML2Client) clients.findClient("SAML2Client");
%>
<h1>index</h1>
<a href="saml/index.jsp">Protected url by SAML : saml/index.jsp</a> (use testpac4j at gmail.com / Pac4jtest)<br />
<a href="ldap/index.jsp">Protected url by LDAP : ldap/index.jsp</a> (use testpac4j at gmail.com / Pac4jtest)<br />

<br />
<a href="logout">logout</a>
<br /><br />
profile : <%=subject.getPrincipals()%>
<%--
	if(subject.getPrincipals()!=null && subject.getPrincipals().asList().size()>1){
		Object pP = 	subject.getPrincipals().asList().get(1);
		String email = "";
		if(pP instanceof SAML2Profile){
			SAML2Profile sp = (SAML2Profile)pP;
			email = sp.getEmail();
			out.print("Saml2Profile: "+sp);
			out.print("<br/>");
			out.print("Email: "+email);
			
		}
			
		
	}
--%>
<br /><br />
<hr />
<%
try {
%>
<a href="<%=formClient.getRedirectAction(context, false).getLocation()%>">Authenticate with form/ldap</a><br />
<a href="<%=saml2Client.getRedirectAction(context, false).getLocation()%>">Authenticate with SAML</a><br />
<%
} catch (RequiresHttpAction e) {
	// should not happen
}
%>

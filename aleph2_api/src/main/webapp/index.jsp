<%@page import="org.pac4j.core.exception.RequiresHttpAction"%>
<%@page import="org.apache.shiro.subject.Subject"%>
<%@page import="org.apache.shiro.SecurityUtils"%>
<%@page import="org.pac4j.demo.shiro.util.WebUtils"%>
<%@page import="io.buji.pac4j.context.session.ShiroSessionStore"%>
<%@page import="org.pac4j.http.client.direct.*"%>
<%@page import="org.pac4j.http.client.indirect.*"%>
<%@page import="org.pac4j.oauth.client.*"%>
<%@page import="org.pac4j.cas.client.*"%>
<%@page import="org.pac4j.saml.client.*"%>
<%@ page import="org.pac4j.core.client.Clients" %>
<%@ page import="org.pac4j.core.context.J2EContext" %>
<%@ page import="org.pac4j.saml.profile.SAML2Profile" %>
<%--@ page import="com.ikanow.aleph2_api.utils.*"--%>

<%
	J2EContext context = new J2EContext(request, response, new ShiroSessionStore());
	Clients clients = WebUtils.getObject(pageContext, Clients.class, "clients");
	Subject subject = SecurityUtils.getSubject();
	FormClient formClient = (FormClient) clients.findClient("FormClient");
	IndirectBasicAuthClient baClient = (IndirectBasicAuthClient) clients.findClient("IndirectBasicAuthClient");
	SAML2Client saml2Client = (SAML2Client) clients.findClient("SAML2Client");
	ParameterClient parameterClient = (ParameterClient) clients.findClient("ParameterClient");
%>
<h1>index</h1>
<a href="form/index.jsp">Protected url by form authentication : form/index.jsp</a> (use login = pwd)<br />
<a href="basicauth/index.jsp">Protected url by basic auth : basicauth/index.jsp</a> (use login = pwd)<br />
<a href="saml/index.jsp">Protected url by SAML : saml/index.jsp</a> (use testpac4j at gmail.com / Pac4jtest)<br />
<a href="ldap/index.jsp">Protected url by LDAP : ldap/index.jsp</a> (use testpac4j at gmail.com / Pac4jtest)<br />

<br />
<a href="jwt.jsp">Generate a JWT token</a> (after being authenticated)<br />
<a href="dba/index.jsp">Protected url by DirectBasicAuthClient: dba/index.jsp</a> (POST the <em>Authorization</em> header with value: <em>Basic amxlbGV1OmpsZWxldQ==</em>) then by <a href="/dba/index.jsp">ParameterClient: /dba/index.jsp</a> (with request parameter: token=<em>jwt_generated_token</em>)<br />
<a href="rest-jwt/index.jsp">Protected url by ParameterClient: rest-jwt/index.jsp</a> (with request parameter: token=<em>jwt_generated_token</em>)<br />
<br />

<a href="logout">logout</a>
<br/>
<a href="<!--  %=Aleph2WebSsoConfig.getInstance().getLogoutUrl()%>">IDP logout</a>
<br /><br />
profile : <%=subject.getPrincipals()%>

<br /><br />
<hr />
<%
try {
%>
<a href="<%=formClient.getRedirectAction(context, false).getLocation()%>">Authenticate with form/ldap</a><br />
<a href="<%=saml2Client.getRedirectAction(context, false).getLocation()%>">Authenticate with SAML</a><br />
<a href="<%=formClient.getRedirectAction(context, false).getLocation()%>">Authenticate with form</a><br />
<a href="<%=baClient.getRedirectAction(context, false).getLocation()%>">Authenticate with basic auth</a><br />

<%
} catch (RequiresHttpAction e) {
	// should not happen
}
%>

<%@page import="org.pac4j.demo.shiro.util.WebUtils"%>
<%@page import="org.pac4j.http.client.indirect.FormClient"%>
<shiro:authenticated>
	<jsp:forward page="v1_cookie.jsp"/>
</shiro:authenticated>
<shiro:notAuthenticated>
<%
	FormClient formClient = WebUtils.getObject(pageContext, FormClient.class, "formClient");
%>
<form action="<%=formClient.getCallbackUrl()%>" method="POST">
	<input type="text" name="username" value="" />
	<p />
	<input type="password" name="password" value="" />
	<p />
	<input type="submit" name="submit" value="Submit" />
</form>
</shiro:notAuthenticated>
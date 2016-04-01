<%@ page import="org.apache.shiro.subject.support.DefaultSubjectContext" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Rest Test</title>
</head>
<p>Hi <shiro:guest>Guest</shiro:guest><shiro:user><shiro:principal/></shiro:user>!
    ( <shiro:user><a href="../logout">Log out</a></shiro:user>
    <shiro:guest><a href="../login.jsp">Log in</a></shiro:guest> )
</p>

<body>
    Grab the principal from session: <%=request.getSession(false).getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY)%>
</body>
</html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:include page="header.jsp" />

<div class="text-center">
    <h1>GDG Workshop Test App</h1>

<%
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null) {
      pageContext.setAttribute("user", user);
%>
	<a href="/crawls" class="btn btn-primary btn-large">
		<i class="icon-white icon-ok"></i>
			Manage your crawls ${fn:escapeXml(user.nickname)}
	</a>
	or
	<a href="<%= userService.createLogoutURL(request.getRequestURI()) %>" class="btn btn-danger btn-small">
		<i class="icon-white icon-remove"></i>
			Log out
	</a>
<%
    } else {
%>
	<a href="<%= userService.createLoginURL(request.getRequestURI()) %>" class="btn btn-primary btn-large">
		<i class="icon-white icon-ok"></i>
			Please sign in
	</a>
<%
    }
%>

</div>

<jsp:include page="footer.jsp" />
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="ro.gdgs.crawler.web.entities.Site" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:include page="header.jsp" />

<%
	UserService userService = UserServiceFactory.getUserService();
%>
<div class="pull-right"><%= request.getAttribute("user") %> |
	<a href="<%= userService.createLogoutURL(request.getRequestURI()) %>">Sign Out</a></div>

<table class="table table-bordered table-hover">
	<caption>Existing Root URIs</caption>
	<thead>
		<tr>
			<th>URL</th>
			<th>Links pending crawl</th>
			<th>Internal links</th>
			<th>External links</th>
			<th><!-- Operations --></th>
		</tr>
	</thead>
	<tbody>
<%
		List<Site> sites = (List<Site>)request.getAttribute("userSites");
		for (Site site: sites) {
%>
		<tr>
			<td><%= site.getUri() %></td>
			<td><%= site.getLinksToCrawl() %></td>
			<td><%= site.getCrawledInternalLinks() %></td>
			<td><%= site.getExternalLinks() %></td>
			<td>
				<form role="form" class="formPosition" action="" method="post">
					<input name="action" type="hidden" value="delete">
					<input name="key" type="hidden" value="<%= site.getKey().getId() %>">
					<button type="submit" class="btn btn-default"><i class="icon-white icon-remove"></i>Delete</button>
				</form>
			</td>
		</tr>
<%
		}
%>
		<form role="form" class="formPosition" action="" method="post">
		<input name="action" type="hidden" value="add">
		<tr>
			<td colspan="4">
				<div class="form-group">
					<label for="uri">Root URI</label>
					<input id="uri" name="uri" type="text" class="form-control" placeholder="" required="">
				</div>
			</td>
			<td>
				<button type="submit" class="btn btn-primary"><i class="icon-white icon-plus"></i>Add</button>
				<button type="reset" class="btn btn-default"><i class="icon-white icon-remove"></i>Reset</button>
			</td>
		</tr>
		</form>
	</tbody>
</table>

<jsp:include page="footer.jsp" />
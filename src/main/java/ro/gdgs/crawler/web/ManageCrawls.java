package ro.gdgs.crawler.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.gdgs.crawler.web.entities.Site;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * @author Octavian
 * @since 1.0
 */
public class ManageCrawls extends HttpServlet {
	private static final long serialVersionUID = -2583198947920354445L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		UserService userService = UserServiceFactory.getUserService();
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		User user = userService.getCurrentUser();
		List<Site> sites = Site.getForUser(datastore, user);

		request.setAttribute("user", user);
		request.setAttribute("userSites", sites);
		renderResponse("crawls.jsp", request, response);
	}

	private void renderResponse(String jspFileName, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		CaptureHttpResponse responseWrapper = new CaptureHttpResponse(response);
		request.getRequestDispatcher(jspFileName).include(request,
				responseWrapper);

		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		try (PrintWriter out = response.getWriter()) {
			out.print(responseWrapper.getStringWriter().toString());
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		switch (request.getParameter("action")) {
		case "add":
			Key siteKey = new Site(user,
					URI.create(request.getParameter("uri"))).save(datastore);
			Queue queue = QueueFactory.getDefaultQueue();
			queue.add(TaskOptions.Builder.withUrl("/crawler")
					.param("uri", request.getParameter("uri"))
					.param("siteKey", KeyFactory.keyToString(siteKey)));
			break;
		case "delete":
			Key k = KeyFactory.createKey("Site",
					Long.parseLong(request.getParameter("key")));
			datastore.delete(k);
			break;
		default:
			throw new IllegalArgumentException(request.getParameter("action"));
		}

		response.sendRedirect(request.getRequestURI());
	}
}

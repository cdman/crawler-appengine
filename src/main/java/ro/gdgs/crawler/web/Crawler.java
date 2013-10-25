package ro.gdgs.crawler.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ro.gdgs.crawler.web.entities.Page;
import ro.gdgs.crawler.web.entities.Site;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

public final class Crawler extends HttpServlet {
	private static final long serialVersionUID = -7810954531612606749L;
	private static final Logger LOG = Logger.getLogger(Crawler.class.getName());

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		MemcacheService cache = MemcacheServiceFactory.getMemcacheService();
		Queue queue = QueueFactory.getDefaultQueue();
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		URI uri;
		try {
			String uriStr = request.getParameter("uri");
			LOG.info("Crawling " + uriStr);
			uri = normalize(URI.create(uriStr));
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Exception during normalization of URI", e);
			return;
		}

		if (checkUriInMemcache(cache, uri)) {
			LOG.info("URI already crawled");
			return;
		}
		putUriInMemcache(cache, uri);

		String pageContents;
		try {
			pageContents = downloadPage(uri);
		} catch (Exception ex) {
			LOG.log(Level.WARNING,
					"Exception during the downloading of the page", ex);
			return;
		}

		List<String> internalUris = new ArrayList<>(), externalUris = new ArrayList<>();
		extractLinks(uri, pageContents, internalUris, externalUris);

		List<String> toCrawl;
		Key siteKey = KeyFactory.stringToKey(request.getParameter("siteKey"));
		while (true) {
			try {
				toCrawl = calculateToCrawlAndUpdateSite(datastore, cache, uri,
						pageContents, internalUris, externalUris, siteKey);
				break;
			} catch (ConcurrentModificationException ex) {
				LOG.info("Contention on " + siteKey
						+ ". Backing of and retrying.");
				Thread.yield();
				continue;
			} catch (Exception ex) {
				LOG.log(Level.WARNING, "Exception during datastore write", ex);
			}
		}

		for (String u : toCrawl) {
			queue.add(TaskOptions.Builder.withUrl("/crawler").param("uri", u)
					.param("siteKey", KeyFactory.keyToString(siteKey)));
		}
	}

	private List<String> calculateToCrawlAndUpdateSite(
			DatastoreService datastore, MemcacheService cache, URI uri,
			String pageContents, List<String> internalUris,
			List<String> externalUris, Key siteKey)
			throws EntityNotFoundException {
		List<String> toCrawl = new ArrayList<>();

		Transaction txn = datastore.beginTransaction();

		Site site = Site.get(siteKey, datastore);

		if (Page.get(datastore, site, uri.toString()) != null) {
			txn.rollback();
			return Collections.emptyList();
		}
		new Page(site, uri, pageContents).save(datastore);

		site = Site.get(siteKey, datastore);
		int newToCrawl = 0;
		for (String u : internalUris) {
			if (checkUriInMemcache(cache, u)) {
				continue;
			}
			if (Page.get(datastore, site, u) != null) {
				continue;
			}
			toCrawl.add(u);
			newToCrawl += 1;
		}

		int newExternalUri = 0;
		for (String u : externalUris) {
			if (Page.get(datastore, site, u) != null) {
				continue;
			}
			new Page(site, URI.create(u), "").save(datastore);
			newExternalUri += 1;
		}

		site = site.decLinksToCrawl().incLinksToCrawl(toCrawl.size())
				.incCrawledInternalLinks(newToCrawl)
				.incExternalLinks(newExternalUri);
		site.save(datastore);
		txn.commit();

		return toCrawl;
	}

	private void extractLinks(URI uri, String page, List<String> internalUris,
			List<String> externalUris) {
		Document doc = Jsoup.parse(page, uri.toString());
		Elements links = doc.select("a[href]");
		for (Element link : links) {
			String href = link.absUrl("href");
			URI hrefURI;
			try {
				hrefURI = normalize(URI.create(href));
			} catch (URISyntaxException e) {
				LOG.log(Level.WARNING, "Exception during uri normalization: "
						+ href, e);
				continue;
			}
			if (hrefURI.getAuthority().equals(uri.getAuthority())) {
				internalUris.add(hrefURI.toString());
			} else {
				externalUris.add(hrefURI.toString());
			}
		}
	}

	private URI normalize(URI uri) throws URISyntaxException {
		URI result = uri.normalize();
		switch (uri.getScheme()) {
		case "http":
		case "https":
			break;
		default:
			throw new IllegalArgumentException("Illegal scheme: "
					+ uri.getScheme());
		}
		return new URI(result.getScheme(), result.getAuthority(),
				result.getPath(), null, null);
	}

	private boolean checkUriInMemcache(MemcacheService cache, String uri) {
		return cache.contains(uri);
	}

	private boolean checkUriInMemcache(MemcacheService cache, URI uri) {
		return cache.contains(uri.toString());
	}

	private void putUriInMemcache(MemcacheService cache, URI uri) {
		cache.put(uri.toString(), true);
	}

	private String downloadPage(URI uri) throws IOException {
		StringBuilder page = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				uri.toURL().openStream(), Charset.forName("UTF-8")))) {
			String line;
			while ((line = reader.readLine()) != null) {
				page.append(line);
			}
		}
		return page.toString();
	}
}

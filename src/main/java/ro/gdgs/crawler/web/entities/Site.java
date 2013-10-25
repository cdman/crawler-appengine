package ro.gdgs.crawler.web.entities;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Link;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.users.User;

public final class Site {
	private final static String KIND = "Site";

	private final Key key;
	private final User user;
	private final URI uri;
	private final long linksToCrawl, crawledInternalLinks, externalLinks;

	public Site(User user, URI uri) {
		this(null, user, uri, 1, 0, 0);
	}

	private Site(Key key, User user, URI uri, long linksToCrawl,
			long crawledInternalLinks, long externalLinks) {
		this.key = key;
		this.user = user;
		this.uri = uri;
		this.linksToCrawl = linksToCrawl;
		this.crawledInternalLinks = crawledInternalLinks;
		this.externalLinks = externalLinks;
	}

	private Site(Entity entity) {
		this.key = entity.getKey();
		this.user = (User) entity.getProperty("user");
		this.uri = URI.create(((Link) entity.getProperty("link")).getValue());
		this.linksToCrawl = (Long) entity.getProperty("linksToCrawl");
		this.crawledInternalLinks = (Long) entity
				.getProperty("crawledInternalLinks");
		this.externalLinks = (Long) entity.getProperty("externalLinks");
	}

	public User getUser() {
		return user;
	}

	public URI getUri() {
		return uri;
	}

	public Key getKey() {
		return key;
	}

	public long getLinksToCrawl() {
		return linksToCrawl;
	}

	public long getCrawledInternalLinks() {
		return crawledInternalLinks;
	}

	public long getExternalLinks() {
		return externalLinks;
	}

	public Site decLinksToCrawl() {
		return new Site(key, user, uri, linksToCrawl - 1, crawledInternalLinks,
				externalLinks);
	}

	public Site incLinksToCrawl(int by) {
		return new Site(key, user, uri, linksToCrawl + by,
				crawledInternalLinks, externalLinks);
	}

	public Site incCrawledInternalLinks(int by) {
		return new Site(key, user, uri, linksToCrawl,
				crawledInternalLinks + by, externalLinks);
	}

	public Site incExternalLinks(int by) {
		return new Site(key, user, uri, linksToCrawl, crawledInternalLinks,
				externalLinks + by);

	}

	public Key save(DatastoreService datastore) {
		Entity entity = key == null ? new Entity(KIND) : new Entity(KIND,
				key.getId());
		entity.setProperty("user", user);
		entity.setProperty("link", new Link(uri.toString()));
		entity.setProperty("linksToCrawl", linksToCrawl);
		entity.setProperty("crawledInternalLinks", crawledInternalLinks);
		entity.setProperty("externalLinks", externalLinks);
		return datastore.put(entity);
	}

	public static List<Site> getForUser(DatastoreService datastore, User user) {
		Query q = new Query(KIND).setFilter(new Query.FilterPredicate("user",
				FilterOperator.EQUAL, user));
		List<Entity> entities = datastore.prepare(q).asList(
				FetchOptions.Builder.withDefaults());
		List<Site> result = new ArrayList<>(entities.size());
		for (Entity e : entities) {
			result.add(new Site(e));
		}
		return result;
	}

	public static Site get(Key key, DatastoreService datastore)
			throws EntityNotFoundException {
		assert key.getKind().equals(KIND);
		return new Site(datastore.get(key));
	}
}

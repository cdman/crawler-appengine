package ro.gdgs.crawler.web.entities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Link;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

public final class Page {
	private static final int DECOMPRESS_BUFFER_SIZE = 1024;
	private static final String KIND = "Page";
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private final Key key;
	private final Site site;
	private final URI uri;
	private final byte[] content;

	public Page(Site site, URI uri, String content) {
		this.key = null;
		this.site = site;
		this.uri = uri;
		this.content = compress(content);
	}

	private Page(Entity entity) {
		this.key = entity.getKey();
		this.site = null;
		this.uri = URI.create(((Link) entity.getProperty("link")).getValue());
		this.content = ((Blob) entity.getProperty("content")).getBytes();
	}

	public Key save(DatastoreService datastore) {
		Entity entity = new Entity(KIND, site.getKey());
		entity.setProperty("link", new Link(uri.toString()));
		entity.setProperty("content", new Blob(content));
		return datastore.put(entity);
	}

	public Key getKey() {
		return key;
	}

	public Site getSite() {
		return site;
	}

	public URI getUri() {
		return uri;
	}

	public String getContent() {
		return decompress(content);
	}

	public static Page get(DatastoreService datastore, Site site, String link) {
		Query q = new Query(KIND).setAncestor(site.getKey()).setFilter(
				new Query.FilterPredicate("link", FilterOperator.EQUAL, link));
		List<Entity> entities = datastore.prepare(q).asList(
				FetchOptions.Builder.withDefaults());
		if (entities.isEmpty()) {
			return null;
		} else if (entities.size() == 1) {
			return new Page(entities.get(0));
		} else {
			throw new IllegalArgumentException();
		}
	}

	private static byte[] compress(String str) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(str.length());
		try (GZIPOutputStream gzOut = new GZIPOutputStream(out)) {
			gzOut.write(str.getBytes(UTF8));
			gzOut.flush();
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return out.toByteArray();
	}

	private static String decompress(byte[] buffer) {
		ByteArrayInputStream in = new ByteArrayInputStream(buffer);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPInputStream gzIn = new GZIPInputStream(in)) {
			byte[] readBuffer = new byte[DECOMPRESS_BUFFER_SIZE];
			int read;
			while ((read = gzIn.read(readBuffer)) > 0) {
				out.write(readBuffer, 0, read);
				if (read < readBuffer.length) {
					break;
				}
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return new String(out.toByteArray(), UTF8);
	}
}

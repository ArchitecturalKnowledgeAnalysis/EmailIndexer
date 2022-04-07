package nl.andrewl.email_indexer.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple cache that loads classpath resources as strings.
 */
public class QueryCache {
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

	/**
	 * Loads a string from a classpath resource.
	 * @param resourceName The name of the resource to load.
	 * @return The resource that was loaded.
	 */
	public static String load(String resourceName) {
		String sql = CACHE.get(resourceName);
		if (sql == null) {
			InputStream is = QueryCache.class.getResourceAsStream(resourceName);
			if (is == null) throw new RuntimeException("Could not load " + resourceName);
			try {
				sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			CACHE.put(resourceName, sql);
		}
		return sql;
	}
}

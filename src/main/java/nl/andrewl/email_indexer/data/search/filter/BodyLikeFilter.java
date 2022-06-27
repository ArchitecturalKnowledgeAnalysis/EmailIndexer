package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

/**
 * Filter that limits to emails whose body matches a given string, using the
 * `LIKE` operator.
 * @param bodyString The substring to search for.
 */
public record BodyLikeFilter(String bodyString) implements SearchFilter {
	@Override
	public String getWhereClause() {
		return "LOWER(EMAIL.BODY) LIKE '%" + bodyString.toLowerCase() + "%'";
	}
}

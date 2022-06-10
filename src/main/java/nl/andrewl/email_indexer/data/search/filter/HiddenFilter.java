package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

/**
 * Filter to limit searches to only hidden or shown emails.
 * @param hidden Whether to show hidden emails (true) or shown emails (false).
 */
public record HiddenFilter(
		boolean hidden
) implements SearchFilter {
	@Override
	public String getWhereClause() {
		return "EMAIL.HIDDEN = " + Boolean.toString(hidden).toUpperCase();
	}
}

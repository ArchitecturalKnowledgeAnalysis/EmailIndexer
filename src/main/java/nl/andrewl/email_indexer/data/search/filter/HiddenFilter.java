package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

public record HiddenFilter(
		boolean hidden
) implements SearchFilter {
	@Override
	public String getWhereClause() {
		return "EMAIL.HIDDEN = " + Boolean.toString(hidden).toUpperCase();
	}
}

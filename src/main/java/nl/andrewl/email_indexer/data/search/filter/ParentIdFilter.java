package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

/**
 * Filter that limits search results to only those emails whose PARENT_ID
 * matches the given non-null value.
 * @param parentId The id of the parent email.
 */
public record ParentIdFilter(long parentId) implements SearchFilter {
	@Override
	public String getWhereClause() {
		return "EMAIL.PARENT_ID = " + parentId;
	}
}

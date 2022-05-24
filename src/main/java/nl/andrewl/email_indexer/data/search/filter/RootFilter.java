package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

/**
 * A filter that can be used to filter an email search to only those emails
 * that are the root of a conversation, or those which are not.
 * @param isRoot Whether to include only emails that are a root, or only
 *               those that are not.
 */
public record RootFilter (
		boolean isRoot
) implements SearchFilter {
	@Override
	public String getWhereClause() {
		return isRoot ?
				"EMAIL.PARENT_ID IS NULL" :
				"EMAIL.PARENT_ID IS NOT NULL";
	}
}

package nl.andrewl.email_indexer.data.search.filter;


import nl.andrewl.email_indexer.data.search.SearchFilter;

/**
 * A filter that can be used to filter to only emails whose subject matches
 * a given string, using the `LIKE` operator.
 * @param subjectString The substring to search for.
 */
public record SubjectLikeFilter(String subjectString) implements SearchFilter {
	@Override
	public String getWhereClause() {
		return "LOWER(EMAIL.SUBJECT) LIKE '%" + subjectString + "%'";
	}
}

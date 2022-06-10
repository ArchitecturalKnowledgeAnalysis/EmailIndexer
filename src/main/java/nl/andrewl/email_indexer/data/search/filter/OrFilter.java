package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A search filter that combines a set of nested filters into an "OR" clause.
 * @param filters The list of filters to include in the OR clause.
 */
public record OrFilter(
		List<SearchFilter> filters
) implements SearchFilter {

	@Override
	public String getWhereClause() {
		return filters.stream()
				.map(SearchFilter::getWhereClause)
				.filter(Objects::nonNull)
				.filter(String::isBlank)
				.map(s -> '(' + s + ')')
				.collect(Collectors.joining(" OR "));
	}

	public static OrFilter of(SearchFilter... filters) {
		return new OrFilter(List.of(filters));
	}
}

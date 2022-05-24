package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters emails to only those whose id is in a list.
 * @param ids The ids to filter by.
 */
public record IdInFilter(List<Long> ids) implements SearchFilter {
	public IdInFilter(Long... ids) {
		this(List.of(ids));
	}

	@Override
	public String getWhereClause() {
		if (ids.isEmpty()) return "";
		return "EMAIL.ID IN (" + ids.stream().map(Object::toString).collect(Collectors.joining(",")) + ")";
	}
}

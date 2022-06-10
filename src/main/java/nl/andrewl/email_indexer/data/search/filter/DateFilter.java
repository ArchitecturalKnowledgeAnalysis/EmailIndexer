package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A date filter that filters emails to only those whose date is between the
 * two inclusive bounds.
 * @param start The start of the interval (inclusive).
 * @param end The end of the interval (inclusive).
 */
public record DateFilter(
		ZonedDateTime start,
		ZonedDateTime end
) implements SearchFilter {
	@Override
	public String getWhereClause() {
		return String.format(
				"EMAIL.DATE >= %s AND EMAIL.DATE <= %s",
				start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
				end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		);
	}
}

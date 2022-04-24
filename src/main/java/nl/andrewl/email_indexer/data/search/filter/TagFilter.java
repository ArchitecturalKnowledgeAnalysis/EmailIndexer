package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A filter that can be used to filter an email search according to certain tags.
 * @param tags The list of tags to filter by.
 * @param type The type of filtering.
 */
public record TagFilter(
		List<String> tags,
		Type type
) implements SearchFilter {
	public enum Type {
		INCLUDE_ANY,
		EXCLUDE_ANY
	}

	public String getWhereClause() {
		if (tags.isEmpty()) {
			// If include any specified tags, but none given, then only show emails without tags.
			if (type == TagFilter.Type.INCLUDE_ANY) {
				return "(SELECT COUNT(EMAIL_TAG.TAG) FROM EMAIL_TAG WHERE EMAIL_TAG.MESSAGE_ID = EMAIL.MESSAGE_ID) = 0";
				//havingCb.with("COUNT(EMAIL_TAG.TAG) = 0");
			} else {
				// Exclude none was specified, so just don't add anything.
				return "";
			}
		} else {
			String tagString = tags.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", "));
			return switch (type) {
				case INCLUDE_ANY -> "EMAIL.MESSAGE_ID IN (SELECT EMAIL_TAG.MESSAGE_ID FROM EMAIL_TAG WHERE TAG IN (" + tagString + "))";
				case EXCLUDE_ANY -> "EMAIL.MESSAGE_ID NOT IN (SELECT EMAIL_TAG.MESSAGE_ID FROM EMAIL_TAG WHERE TAG IN (" + tagString + "))";
			};
		}
	}

	public static TagFilter includeNone() {
		return new TagFilter(List.of(), Type.INCLUDE_ANY);
	}

	public static TagFilter excludeNone() {
		return new TagFilter(List.of(), Type.EXCLUDE_ANY);
	}

	public static TagFilter including(String... tags) {
		return new TagFilter(List.of(tags), Type.INCLUDE_ANY);
	}

	public static TagFilter excluding(String... tags) {
		return new TagFilter(List.of(tags), Type.EXCLUDE_ANY);
	}
}

package nl.andrewl.email_indexer.data.search.filter;

import nl.andrewl.email_indexer.data.search.SearchFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * A filter that can be used to filter an email search according to certain tags.
 * @param tagIds The list of tags to filter by.
 * @param type The type of filtering.
 */
public record TagFilter(
		Collection<Integer> tagIds,
		Type type
) implements SearchFilter {
	public enum Type {
		INCLUDE_ANY,
		EXCLUDE_ANY
	}

	public String getWhereClause() {
		if (tagIds.isEmpty()) {
			// If include any specified tags, but none given, then only show emails without tags.
			if (type == TagFilter.Type.INCLUDE_ANY) {
				return "(SELECT COUNT(EMAIL_TAG.EMAIL_ID) FROM EMAIL_TAG WHERE EMAIL_TAG.EMAIL_ID = EMAIL.ID) = 0";
			} else {
				// Exclude none was specified, so just don't add anything.
				return "";
			}
		} else {
			String tagString = tagIds.stream().map(Object::toString).collect(Collectors.joining(","));
			return switch (type) {
				case INCLUDE_ANY -> "EMAIL.ID IN (SELECT ET.EMAIL_ID FROM EMAIL_TAG ET WHERE ET.TAG_ID IN (" + tagString + "))";
				case EXCLUDE_ANY -> "EMAIL.ID NOT IN (SELECT ET.EMAIL_ID FROM EMAIL_TAG ET WHERE ET.TAG_ID IN (" + tagString + "))";
			};
		}
	}

	public static TagFilter includeNone() {
		return new TagFilter(Collections.emptyList(), Type.INCLUDE_ANY);
	}

	public static TagFilter excludeNone() {
		return new TagFilter(Collections.emptyList(), Type.EXCLUDE_ANY);
	}

	public static TagFilter including(Collection<Integer> tagIds) {
		return new TagFilter(tagIds, Type.INCLUDE_ANY);
	}

	public static TagFilter excluding(Collection<Integer> tagIds) {
		return new TagFilter(tagIds, Type.EXCLUDE_ANY);
	}
}

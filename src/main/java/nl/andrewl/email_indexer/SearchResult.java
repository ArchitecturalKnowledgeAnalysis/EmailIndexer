package nl.andrewl.email_indexer;

import java.util.List;

public record SearchResult(
		List<String> resultIds,
		long totalResultsCount,
		int page,
		int size
) {}

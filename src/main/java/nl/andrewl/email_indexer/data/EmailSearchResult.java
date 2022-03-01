package nl.andrewl.email_indexer.data;

import java.util.List;

public record EmailSearchResult(
		List<EmailEntryPreview> emails,
		int page,
		int size,
		long totalResultCount
) {}

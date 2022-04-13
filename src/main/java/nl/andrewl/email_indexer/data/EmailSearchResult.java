package nl.andrewl.email_indexer.data;

import java.util.List;

public record EmailSearchResult(
		List<EmailEntryPreview> emails,
		int page,
		int pageCount,
		boolean hasPreviousPage,
		boolean hasNextPage,
		int size,
		long totalResultCount
) {
	public static EmailSearchResult of(List<EmailEntryPreview> emails, int page, int size, long totalResultCount) {
		int pageCount = (int) Math.ceil(totalResultCount / (double) size);
		boolean hasPreviousPage = page > 1;
		boolean hasNextPage = page < pageCount;
		return new EmailSearchResult(emails, page, pageCount, hasPreviousPage, hasNextPage, size, totalResultCount);
	}
}

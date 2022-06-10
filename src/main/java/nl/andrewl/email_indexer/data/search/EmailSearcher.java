package nl.andrewl.email_indexer.data.search;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.QueryCache;
import nl.andrewl.email_indexer.util.Async;
import nl.andrewl.email_indexer.util.ConditionBuilder;
import nl.andrewl.email_indexer.util.DbUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A special repository that contains methods dedicated to providing search
 * functionality over the dataset with many filters and options, and
 * pagination.
 */
public class EmailSearcher {
	private final Connection conn;

	public EmailSearcher(Connection conn) {
		this.conn = conn;
	}

	public EmailSearcher(EmailDataset dataset) {
		this(dataset.getConnection());
	}

	/**
	 * Searches over the collection of all emails.
	 * @param page The page of results to get. Starts at 1.
	 * @param size The size of each page.
	 * @param filters The filters to apply.
	 * @return A search result.
	 */
	public CompletableFuture<EmailSearchResult> findAll(int page, int size, Collection<SearchFilter> filters) {
		return findAll(page, size, filters, false);
	}

	/**
	 * Searches over the collection of all emails.
	 * @param page The page of results to get. Starts at 1.
	 * @param size The size of each page.
	 * @param filters The filters to apply.
	 * @param debug Whether to enable debug printing of the queries.
	 * @return A search result.
	 */
	public CompletableFuture<EmailSearchResult> findAll(int page, int size, Collection<SearchFilter> filters, boolean debug) {
		return Async.supply(() -> {
			List<EmailEntryPreview> entries = new ArrayList<>(size);
			String searchQuery = getSearchQuery(page, size, filters);
			String countQuery = getSearchCountQuery(filters);
			if (debug) {
				System.out.printf(
						"Searching for page %d of %d emails:%nUsing query:%n%s%nAnd count query:%n%s%n",
						page,
						size,
						searchQuery,
						countQuery
				);
			}
			try (
				var queryStmt = conn.prepareStatement(searchQuery);
				var countStmt = conn.prepareStatement(countQuery)
			) {
				var queryRs = queryStmt.executeQuery();
				while (queryRs.next()) entries.add(new EmailEntryPreview(queryRs));
				var countRs = countStmt.executeQuery();
				countRs.next();
				return EmailSearchResult.of(entries, page, size, countRs.getLong(1));
			} catch (SQLException e) {
				e.printStackTrace();
				return EmailSearchResult.of(new ArrayList<>(), 0, size, 0);
			}
		});
	}

	/**
	 * Gets a count for emails matching the criteria.
	 * @param filters The filters to apply.
	 * @return The number of emails.
	 */
	public CompletableFuture<Long> countAll(Collection<SearchFilter> filters) {
		return Async.supply(() -> DbUtils.count(conn, getSearchCountQuery(filters)));
	}

	private String getSearchQuery(int page, int size, Collection<SearchFilter> filters) {
		ConditionBuilder whereCb = ConditionBuilder.whereAnd();
		for (var filter : filters) whereCb.with(filter.getWhereClause());
		String whereClause = whereCb.build();
		return String.format(
				"""
				%s
				%s
				ORDER BY %s
				LIMIT %d OFFSET %d""",
				QueryCache.load("/sql/preview/search_query.sql"),
				whereClause,
				"EMAIL.DATE DESC, EMAIL.MESSAGE_ID ASC",
				size, (page - 1) * size
		);
	}

	private String getSearchCountQuery(Collection<SearchFilter> filters) {
		String countQuery = """
			SELECT COUNT(EMAIL.ID)
			FROM EMAIL
			%s
			""";
		ConditionBuilder whereCb = ConditionBuilder.whereAnd();
		for (var filter : filters) whereCb.with(filter.getWhereClause());
		return String.format(countQuery, whereCb.build());
	}
}

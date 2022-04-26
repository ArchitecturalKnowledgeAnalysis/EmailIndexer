package nl.andrewl.email_indexer.data.search;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
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
	 * @param page The page of results to get.
	 * @param size The size of each page.
	 * @param filters The filters to apply.
	 * @return A search result.
	 */
	public CompletableFuture<EmailSearchResult> findAll(int page, int size, Collection<SearchFilter> filters) {
		return Async.supply(() -> {
			List<EmailEntryPreview> entries = new ArrayList<>(size);
			try (
				var queryStmt = conn.prepareStatement(getSearchQuery(page, size, filters));
				var countStmt = conn.prepareStatement(getSearchCountQuery(filters))
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
		String queryFormat = """
			SELECT EMAIL.MESSAGE_ID, SUBJECT, SENT_FROM, DATE, LISTAGG(TAG, ','), HIDDEN
			FROM EMAIL
			LEFT JOIN EMAIL_TAG ON EMAIL.MESSAGE_ID = EMAIL_TAG.MESSAGE_ID
			%s
			GROUP BY EMAIL.MESSAGE_ID
			ORDER BY EMAIL.DATE DESC
			LIMIT %d OFFSET %d""";
		ConditionBuilder whereCb = ConditionBuilder.whereAnd();
		for (var filter : filters) whereCb.with(filter.getWhereClause());
		return String.format(queryFormat, whereCb.build(), size, (page - 1) * size);
	}

	private String getSearchCountQuery(Collection<SearchFilter> filters) {
		String countQuery = """
			SELECT COUNT(DISTINCT EMAIL.MESSAGE_ID)
			FROM EMAIL
			%s
			""";
		ConditionBuilder whereCb = ConditionBuilder.whereAnd();
		for (var filter : filters) whereCb.with(filter.getWhereClause());
		return String.format(countQuery, whereCb.build());
	}
}

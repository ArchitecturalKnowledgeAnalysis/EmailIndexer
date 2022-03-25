package nl.andrewl.email_indexer.data;

import nl.andrewl.email_indexer.EmailIndexSearcher;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;

public class EmailRepository {
	private final Connection conn;
	private final Path indexDir;

	public EmailRepository(Connection conn, Path indexDir) {
		this.conn = conn;
		this.indexDir = indexDir;
	}

	public EmailRepository(EmailDataset ds) {
		this(ds.getConnection(), ds.getIndexDir());
	}

	public long countEmails() {
		return DbUtils.count(conn, "SELECT COUNT(MESSAGE_ID) FROM EMAIL");
	}

	public long countTags() {
		return DbUtils.count(conn, "SELECT COUNT(TAG) FROM EMAIL_TAG");
	}

	public long countTaggedEmails() {
		return DbUtils.count(conn, "SELECT COUNT(DISTINCT MESSAGE_ID) FROM EMAIL_TAG");
	}

	public Optional<EmailEntry> findEmailById(String messageId) {
		try (var stmt = conn.prepareStatement(QueryCache.load("/sql/fetch_email_by_id.sql"))) {
			stmt.setString(1, messageId);
			var rs = stmt.executeQuery();
			if (!rs.next()) return Optional.empty();
			var entry = new EmailEntry(
					messageId,
					rs.getString(1),
					rs.getString(2),
					rs.getString(3),
					rs.getObject(4, ZonedDateTime.class),
					rs.getString(5),
					new ArrayList<>()
			);
			String tags = rs.getString(6);
			if (tags != null && !tags.isEmpty()) {
				for (var tag : tags.split(",")) {
					entry.tags().add(tag);
				}
				Collections.sort(entry.tags());
			}
			return Optional.of(entry);
		} catch (SQLException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	public List<EmailEntryPreview> findAllReplies(String messageId) {
		List<EmailEntryPreview> entries = new ArrayList<>();
		try (var stmt = conn.prepareStatement(QueryCache.load("/sql/fetch_email_preview_by_in_reply_to.sql"))) {
			stmt.setString(1, messageId);
			var rs = stmt.executeQuery();
			while (rs.next()) entries.add(new EmailEntryPreview(rs));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return entries;
	}

	public Optional<EmailEntryPreview> findRootEmailByChildId(String messageId) {
		try (var stmt = conn.prepareStatement("SELECT IN_REPLY_TO FROM EMAIL WHERE EMAIL.MESSAGE_ID = ?")) {
			String nextId = messageId;
			String lastId = null;
			while (nextId != null) {
				stmt.setString(1, nextId);
				var rs = stmt.executeQuery();
				if (!rs.next()) return Optional.empty();
				String inReplyTo = rs.getString(1);
				lastId = nextId;
				nextId = inReplyTo;
			}
			try (var stmt2 = conn.prepareStatement(QueryCache.load("/sql/fetch_email_preview_by_id.sql"))) {
				stmt2.setString(1, lastId);
				var rs2 = stmt2.executeQuery();
				if (rs2.next()) {
					return Optional.of(new EmailEntryPreview(rs2));
				} else {
					return Optional.empty();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	public EmailSearchResult search(String query, int page, int size) {
		try {
			var result = new EmailIndexSearcher().search(indexDir, query, page, size);
			List<EmailEntryPreview> entries = new ArrayList<>(result.size());
			for (var messageId : result.resultIds()) {
				findRootEmailByChildId(messageId).ifPresent(preview -> {
					if (!entries.contains(preview)) entries.add(preview);
				});
			}
			return EmailSearchResult.of(entries, result.page(), result.size(), result.totalResultsCount());
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			return EmailSearchResult.of(new ArrayList<>(), 1, size, 0);
		}
	}

	public EmailSearchResult findAll(int page, int size, Boolean hidden, Boolean tagged) {
		String queryFormat = """
			SELECT EMAIL.MESSAGE_ID, SUBJECT, SENT_FROM, DATE, LISTAGG(TAG, ',')
			FROM EMAIL
			LEFT JOIN EMAIL_TAG ON EMAIL.MESSAGE_ID = EMAIL_TAG.MESSAGE_ID
			!!WHERE!!
			GROUP BY EMAIL.MESSAGE_ID
			!!HAVING!!
			ORDER BY EMAIL.DATE DESC
			LIMIT %d OFFSET %d""";
		String countQuery = """
			SELECT COUNT(EMAIL.MESSAGE_ID)
			FROM EMAIL
			!!WHERE!!""";
		String query = String.format(queryFormat, size, (page - 1) * size);
		List<String> whereConditions = new ArrayList<>();
		if (hidden != null) whereConditions.add("EMAIL.HIDDEN = " + Boolean.toString(hidden).toUpperCase());
		String whereClause = whereConditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", whereConditions);
		query = query.replace("!!WHERE!!", whereClause);
		if (tagged != null) {
			whereConditions.add("EMAIL.MESSAGE_ID " + (tagged ? "" : "NOT") + " IN (SELECT MESSAGE_ID FROM EMAIL_TAG)");
		}
		whereClause = whereConditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", whereConditions);
		countQuery = countQuery.replace("!!WHERE!!", whereClause);

		List<String> havingConditions = new ArrayList<>();
		if (tagged != null) {
			havingConditions.add("COUNT(EMAIL_TAG.TAG) " + (tagged ? '>' : '=') + " 0");
		}
		String havingClause = havingConditions.isEmpty() ? "" : "HAVING " + String.join(" AND ", havingConditions);
		query = query.replace("!!HAVING!!", havingClause);

		List<EmailEntryPreview> entries = new ArrayList<>(size);
		try (var stmt = conn.prepareStatement(query)) {
			var rs = stmt.executeQuery();
			while (rs.next()) entries.add(new EmailEntryPreview(rs));
			long totalResultCount = DbUtils.count(conn, countQuery);
			return EmailSearchResult.of(entries, page, size, totalResultCount);
		} catch (SQLException e) {
			e.printStackTrace();
			return EmailSearchResult.of(new ArrayList<>(), 1, size, 0);
		}
	}

	public boolean hasTag(String messageId, String tag) {
		try (var stmt = conn.prepareStatement("SELECT COUNT(TAG) FROM EMAIL_TAG WHERE MESSAGE_ID = ? AND TAG = ?")) {
			stmt.setString(1, messageId);
			stmt.setString(2, tag);
			var rs = stmt.executeQuery();
			if (rs.next()) return rs.getLong(1) > 0;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void addTag(String messageId, String tag) {
		if (!hasTag(messageId, tag)) {
			try (var stmt = conn.prepareStatement("INSERT INTO EMAIL_TAG (MESSAGE_ID, TAG) VALUES (?, ?)")) {
				stmt.setString(1, messageId);
				stmt.setString(2, tag);
				stmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void removeTag(String messageId, String tag) {
		try (var stmt = conn.prepareStatement("DELETE FROM EMAIL_TAG WHERE MESSAGE_ID = ? AND TAG = ?")) {
			stmt.setString(1, messageId);
			stmt.setString(2, tag);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<String> getAllTags() {
		List<String> tags = new ArrayList<>();
		try (var stmt = conn.prepareStatement("SELECT DISTINCT TAG FROM EMAIL_TAG ORDER BY TAG")) {
			var rs = stmt.executeQuery();
			while (rs.next()) tags.add(rs.getString(1));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tags;
	}

	public long hideAllEmailsByBody(String body) {
		try (var stmt = conn.prepareStatement("UPDATE EMAIL SET HIDDEN = TRUE WHERE BODY LIKE ?")) {
			stmt.setString(1, body);
			return stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public long hideAllEmailsBySentFrom(String sentFrom) {
		try (var stmt = conn.prepareStatement("UPDATE EMAIL SET HIDDEN = TRUE WHERE SENT_FROM LIKE ?")) {
			stmt.setString(1, sentFrom);
			return stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}
}

package nl.andrewl.email_indexer.data;

import nl.andrewl.email_indexer.EmailIndexSearcher;
import nl.andrewl.email_indexer.data.util.ConditionBuilder;
import nl.andrewl.email_indexer.data.util.DbUtils;
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
					new ArrayList<>(),
					rs.getBoolean(7)
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

	public EmailSearchResult search(String query) {
		try {
			var result = new EmailIndexSearcher().search(indexDir, query, 1, 100000);
			List<EmailEntryPreview> entries = new ArrayList<>(result.size());
			for (var messageId : result.resultIds()) {
				findRootEmailByChildId(messageId).ifPresent(preview -> {
					if (!entries.contains(preview) && !preview.hidden()) entries.add(preview);
				});
			}
			return EmailSearchResult.of(entries, result.page(), result.size(), result.totalResultsCount());
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			return EmailSearchResult.of(new ArrayList<>(), 1, 100000, 0);
		}
	}

	public EmailSearchResult findAll(int page, int size, Boolean hidden, Boolean tagged) {
		List<EmailEntryPreview> entries = new ArrayList<>(size);
		String q = getSearchQuery(page, size, hidden, tagged);
		try (var stmt = conn.prepareStatement(q)) {
			var rs = stmt.executeQuery();
			while (rs.next()) entries.add(new EmailEntryPreview(rs));
			long totalResultCount = DbUtils.count(conn, getSearchCountQuery(hidden, tagged));
			return EmailSearchResult.of(entries, page, size, totalResultCount);
		} catch (SQLException e) {
			e.printStackTrace();
			return EmailSearchResult.of(new ArrayList<>(), 1, size, 0);
		}
	}

	private String getSearchQuery(int page, int size, Boolean hidden, Boolean tagged) {
		String queryFormat = """
			SELECT EMAIL.MESSAGE_ID, SUBJECT, SENT_FROM, DATE, LISTAGG(TAG, ','), HIDDEN
			FROM EMAIL
			LEFT JOIN EMAIL_TAG ON EMAIL.MESSAGE_ID = EMAIL_TAG.MESSAGE_ID
			%s
			GROUP BY EMAIL.MESSAGE_ID
			%s
			ORDER BY EMAIL.DATE DESC
			LIMIT %d OFFSET %d""";
		ConditionBuilder whereCb = ConditionBuilder.whereAnd();
		if (hidden != null) whereCb.with("EMAIL.HIDDEN = " + Boolean.toString(hidden).toUpperCase());
		ConditionBuilder havingCb = ConditionBuilder.havingAnd();
		if (tagged != null) havingCb.with("COUNT(EMAIL_TAG.TAG) " + (tagged ? '>' : '=') + " 0");
		return String.format(queryFormat, whereCb.build(), havingCb.build(), size, (page - 1) * size);
	}

	private String getSearchCountQuery(Boolean hidden, Boolean tagged) {
		String countQuery = """
			SELECT COUNT(EMAIL.MESSAGE_ID)
			FROM EMAIL
			%s""";
		ConditionBuilder whereCb = ConditionBuilder.whereAnd();
		if (hidden != null) whereCb.with("EMAIL.HIDDEN = " + Boolean.toString(hidden).toUpperCase());
		if (tagged != null) {
			whereCb.with("EMAIL.MESSAGE_ID " + (tagged ? "" : "NOT") + " IN (SELECT MESSAGE_ID FROM EMAIL_TAG)");
		}
		return String.format(countQuery, whereCb.build());
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

	/**
	 * Gets all tags in the dataset.
	 * @return The list of all tags.
	 */
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

	/**
	 * Gets the list of all tags belonging to any parents of the given email.
	 * @param messageId The id of the email.
	 * @return A list of tags.
	 */
	public List<String> getAllParentTags(String messageId) {
		Set<String> tags = new HashSet<>();
		try (
			var parentStmt = conn.prepareStatement("SELECT IN_REPLY_TO FROM EMAIL WHERE MESSAGE_ID = ?");
			var tagStmt = conn.prepareStatement("SELECT TAG FROM EMAIL_TAG WHERE MESSAGE_ID = ?")
		) {
			String nextId = messageId;
			while (nextId != null) {
				parentStmt.setString(1, nextId);
				var rs = parentStmt.executeQuery();
				if (!rs.next()) break;
				String inReplyTo = rs.getString(1);
				nextId = inReplyTo;

				if (inReplyTo != null) {
					tagStmt.setString(1, inReplyTo);
					var tagRs = tagStmt.executeQuery();
					while (tagRs.next()) tags.add(tagRs.getString(1));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		List<String> tagList = new ArrayList<>(tags);
		tagList.sort(String::compareToIgnoreCase);
		return tagList;
	}

	/**
	 * Gets the list of all tags in any emails that are children of the given
	 * email, by using a breadth-first search of all children.
	 * @param messageId The email to check.
	 * @return The list of all tags.
	 */
	public List<String> getAllChildTags(String messageId) {
		Set<String> tags = new HashSet<>();
		Queue<String> messageIdQueue = new LinkedList<>();
		messageIdQueue.add(messageId);
		try (
			var childStmt = conn.prepareStatement("SELECT MESSAGE_ID FROM EMAIL WHERE IN_REPLY_TO = ?");
			var tagStmt = conn.prepareStatement("SELECT TAG FROM EMAIL_TAG WHERE MESSAGE_ID = ?")
		) {
			while (!messageIdQueue.isEmpty()) {
				childStmt.setString(1, messageIdQueue.remove());
				var rs = childStmt.executeQuery();
				while (rs.next()) {
					String childId = rs.getString(1);
					tagStmt.setString(1, childId);
					var tagRs = tagStmt.executeQuery();
					while (tagRs.next()) {
						tags.add(tagRs.getString(1));
					}
					messageIdQueue.add(childId);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		List<String> tagList = new ArrayList<>(tags);
		tagList.sort(String::compareToIgnoreCase);
		return tagList;
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

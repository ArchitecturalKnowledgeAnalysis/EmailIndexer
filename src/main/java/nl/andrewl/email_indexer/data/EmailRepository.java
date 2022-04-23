package nl.andrewl.email_indexer.data;

import nl.andrewl.email_indexer.data.util.Async;
import nl.andrewl.email_indexer.data.util.ConditionBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static nl.andrewl.email_indexer.data.util.DbUtils.*;

/**
 * Repository for accessing emails from a dataset.
 */
public class EmailRepository {
	private final Connection conn;

	public EmailRepository(Connection conn) {
		this.conn = conn;
	}

	public EmailRepository(EmailDataset ds) {
		this(ds.getConnection());
	}

	/**
	 * Gets the total number of emails in the dataset.
	 * @return The total number of emails.
	 */
	public long countEmails() {
		return count(conn, "SELECT COUNT(MESSAGE_ID) FROM EMAIL");
	}

	/**
	 * Gets the total number of unique tags in the dataset.
	 * @return The total number of unique tags.
	 */
	public long countTags() {
		return count(conn, "SELECT COUNT(DISTINCT TAG) FROM EMAIL_TAG");
	}

	/**
	 * Gets the total number of emails that have at least one tag applied.
	 * @return The total number of tagged emails.
	 */
	public long countTaggedEmails() {
		return count(conn, "SELECT COUNT(DISTINCT MESSAGE_ID) FROM EMAIL_TAG");
	}

	/**
	 * Fetches an email by its unique message id.
	 * @param messageId The email's id.
	 * @return An optional that contains the email that was found, if any.
	 */
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

	/**
	 * Finds a preview of an email by its unique message id.
	 * @param messageId The id of the email.
	 * @return An optional that contains a preview of the email that was found,
	 * if any.
	 */
	public Optional<EmailEntryPreview> findPreviewById(String messageId) {
		try (var stmt = conn.prepareStatement(QueryCache.load("/sql/fetch_email_preview_by_id.sql"))) {
			stmt.setString(1, messageId);
			var rs = stmt.executeQuery();
			if (!rs.next()) return Optional.empty();
			return Optional.of(new EmailEntryPreview(rs));
		} catch (SQLException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	/**
	 * Finds all replies for an email identified by the given id.
	 * @param messageId The email's id.
	 * @return A list of previews of emails that are replies to the email
	 * identified by the provided message id.
	 */
	public List<EmailEntryPreview> findAllReplies(String messageId) {
		return fetch(
				conn,
				QueryCache.load("/sql/fetch_email_preview_by_in_reply_to.sql"),
				EmailEntryPreview::new,
				messageId
		);
	}

	/**
	 * Recursively populates an email preview's set of replies by searching
	 * the database and updating the previews set of replies.
	 * @param entry The email preview to fetch replies for.
	 */
	public void loadRepliesRecursive(EmailEntryPreview entry) {
		entry.replies().addAll(findAllReplies(entry.messageId()));
		for (var reply : entry.replies()) {
			loadRepliesRecursive(reply);
		}
	}

	/**
	 * Gets just the body of an email identified by the given id.
	 * @param messageId The id of the email whose body to get.
	 * @return The body of the requested email.
	 */
	public Optional<String> getBody(String messageId) {
		try (var stmt = conn.prepareStatement("SELECT BODY FROM EMAIL WHERE MESSAGE_ID = ?")) {
			stmt.setString(1, messageId);
			var rs = stmt.executeQuery();
			if (rs.next()) {
				return Optional.of(rs.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	/**
	 * Finds a preview of the root email, given the id of an email somewhere
	 * in the thread.
	 * @param messageId The id of the email to get the root of.
	 * @return The root email, if any was found.
	 */
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

	/**
	 * Searches over the collection of all emails.
	 * @param page The page of results to get.
	 * @param size The size of each page.
	 * @param hidden Whether to show hidden emails.
	 * @param tagged Whether to show tagged emails.
	 * @return A search result.
	 */
	public CompletableFuture<EmailSearchResult> findAll(int page, int size, Boolean hidden, Boolean tagged) {
		return Async.supply(() -> {
			List<EmailEntryPreview> entries = new ArrayList<>(size);
			String q = getSearchQuery(page, size, hidden, tagged);
			try (var stmt = conn.prepareStatement(q)) {
				var rs = stmt.executeQuery();
				while (rs.next()) entries.add(new EmailEntryPreview(rs));
				long totalResultCount = count(conn, getSearchCountQuery(hidden, tagged));
				return EmailSearchResult.of(entries, page, size, totalResultCount);
			} catch (SQLException e) {
				e.printStackTrace();
				return EmailSearchResult.of(new ArrayList<>(), 0, size, 0);
			}
		});
	}

	/**
	 * Gets a count for emails matching the criteria.
	 * @param hidden Whether to include hidden emails.
	 * @param tagged Whether to include tagged emails.
	 * @return The number of emails.
	 */
	public long countAll(Boolean hidden, Boolean tagged) {
		return count(conn, getSearchCountQuery(hidden, tagged));
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

	/**
	 * Determines if an email has a certain tag.
	 * @param messageId The id of the email.
	 * @param tag The tag to check.
	 * @return True if the email has the tag, or false otherwise.
	 */
	public boolean hasTag(String messageId, String tag) {
		return count(conn, "SELECT COUNT(TAG) FROM EMAIL_TAG WHERE MESSAGE_ID = ? AND TAG = ?", messageId, tag) > 0;
	}

	/**
	 * Adds a tag to an email. Does nothing if the email already has the tag.
	 * @param messageId The id of the email.
	 * @param tag The tag to add.
	 */
	public void addTag(String messageId, String tag) {
		if (!hasTag(messageId, tag)) {
			update(conn, "INSERT INTO EMAIL_TAG (MESSAGE_ID, TAG) VALUES (?, ?)", messageId, tag);
		}
	}

	/**
	 * Removes a tag from an email. Does nothing if the email doesn't have the
	 * tag.
	 * @param messageId The id of the email.
	 * @param tag The tag to remove.
	 */
	public void removeTag(String messageId, String tag) {
		update(conn, "DELETE FROM EMAIL_TAG WHERE MESSAGE_ID = ? AND TAG = ?", messageId, tag);
	}

	/**
	 * Gets all tags in the dataset.
	 * @return The list of all tags.
	 */
	public List<String> getAllTags() {
		return fetch(conn, "SELECT DISTINCT TAG FROM EMAIL_TAG ORDER BY TAG", rs -> rs.getString(1));
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

	/**
	 * Sets an email as hidden.
	 * @param messageId The id of the email.
	 */
	public void hideEmail(String messageId) {
		update(conn, "UPDATE EMAIL SET HIDDEN = TRUE WHERE MESSAGE_ID = ?", messageId);
	}

	/**
	 * Sets an email as not hidden.
	 * @param messageId The id of the email.
	 */
	public void showEmail(String messageId) {
		update(conn, "UPDATE EMAIL SET HIDDEN = FALSE WHERE MESSAGE_ID = ?", messageId);
	}

	private int hideEmailsByQuery(String msg, String conditions, Object... args) {
		try {
			conn.setAutoCommit(false);
			List<String> ids = fetch(conn, "SELECT MESSAGE_ID FROM EMAIL WHERE " + conditions, rs -> rs.getString(1), args);
			long mId = insertWithId(conn, "INSERT INTO MUTATION (DESCRIPTION) VALUES (?)", msg);
			try (var stmt = conn.prepareStatement("INSERT INTO MUTATION_EMAIL(MUTATION_ID, MESSAGE_ID) VALUES (?, ?)")) {
				stmt.setLong(1, mId);
				for (var id : ids) {
					stmt.setString(2, id);
					stmt.executeUpdate();
				}
			}
			int count = update(conn, "UPDATE EMAIL SET HIDDEN = TRUE WHERE " + conditions, args);
			update(conn, "UPDATE MUTATION SET AFFECTED_EMAIL_COUNT = ? WHERE ID = ?", mId, count);
			conn.commit();
			conn.setAutoCommit(true);
			return count;
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Hides all emails whose body matches the given text.
	 * @param body The body text to match.
	 * @return The number of emails that were hidden.
	 */
	public int hideAllEmailsByBody(String body) {
		return hideEmailsByQuery(
				"Hiding all emails with a body like:\n\n" + body,
				"HIDDEN = FALSE AND BODY LIKE ?",
				body
		);
	}

	/**
	 * Hides all emails sent from a given email address.
	 * @param sentFrom The address to hide emails from.
	 * @return The number of emails that were hidden.
	 */
	public int hideAllEmailsBySentFrom(String sentFrom) {
		return hideEmailsByQuery(
				"Hiding all emails sent by email addresses like: " + sentFrom,
				"HIDDEN = FALSE AND SENT_FROM LIKE ?",
				sentFrom
		);
	}

	/**
	 * Permanently deletes all hidden emails, which can be used to save space.
	 * It is recommended to call {@link nl.andrewl.email_indexer.gen.EmailIndexGenerator#generateIndex(EmailDataset, Consumer)}
	 * after deleting many emails.
	 */
	public void deleteAllHidden() {
		int count = update(conn, "DELETE FROM EMAIL WHERE HIDDEN = TRUE");
		String desc = "Permanently deleting all hidden emails.";
		update(conn, "INSERT INTO MUTATION (DESCRIPTION, AFFECTED_EMAIL_COUNT) VALUES (?, ?)", desc, count);
	}

	/**
	 * Gets a list of all mutations applied to the dataset, ordered from latest
	 * to earliest.
	 * @return The list of mutations.
	 */
	public List<MutationEntry> getAllMutations() {
		return fetch(
				conn,
				QueryCache.load("/sql/fetch_all_mutations.sql"),
				MutationEntry::new
		);
	}
}

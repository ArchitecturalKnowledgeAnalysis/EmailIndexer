package nl.andrewl.email_indexer.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static nl.andrewl.email_indexer.util.DbUtils.*;

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
		return count(conn, "SELECT COUNT(ID) FROM EMAIL");
	}

	/**
	 * Gets the total number of emails that have at least one tag applied.
	 * @return The total number of tagged emails.
	 */
	public long countTaggedEmails() {
		return count(conn, "SELECT COUNT(DISTINCT EMAIL_ID) FROM EMAIL_TAG");
	}

	public Optional<Long> findId(String messageId) {
		try (var stmt = conn.prepareStatement("SELECT ID FROM EMAIL WHERE MESSAGE_ID = ?")) {
			stmt.setString(1, messageId);
			var rs = stmt.executeQuery();
			if (rs.next()) return Optional.of(rs.getLong(1));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	/**
	 * Fetches an email by its id.
	 * @param id The email's id.
	 * @return An optional that contains the email that was found, if any.
	 */
	public Optional<EmailEntry> findEmailById(long id) {
		try (var stmt = conn.prepareStatement(QueryCache.load("/sql/fetch_email_by_id.sql"))) {
			stmt.setLong(1, id);
			var rs = stmt.executeQuery();
			if (!rs.next()) return Optional.empty();
			var entry = new EmailEntry(
					rs.getLong(1),
					rs.getObject(2, Long.class),
					rs.getString(3),
					rs.getString(4),
					rs.getString(5),
					rs.getString(6),
					rs.getObject(7, ZonedDateTime.class),
					rs.getString(8),
					rs.getBoolean(9)
			);
			return Optional.of(entry);
		} catch (SQLException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	/**
	 * Finds a preview of an email by its unique message id.
	 * @param id The id of the email.
	 * @return An optional that contains a preview of the email that was found,
	 * if any.
	 */
	public Optional<EmailEntryPreview> findPreviewById(long id) {
		try (var stmt = conn.prepareStatement(QueryCache.load("/sql/preview/fetch_email_preview_by_id.sql"))) {
			stmt.setLong(1, id);
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
	 * @param id The email's id.
	 * @return A list of previews of emails that are replies to the email
	 * identified by the provided message id.
	 */
	public List<EmailEntryPreview> findAllReplies(long id) {
		return fetch(
				conn,
				QueryCache.load("/sql/preview/fetch_email_preview_by_parent_id.sql"),
				EmailEntryPreview::new,
				id
		);
	}

	/**
	 * Finds the number of replies that exist for an email.
	 * @param id The parent email's id.
	 * @return The number of replies to the given email.
	 */
	public long countReplies(long id) {
		return count(conn, "SELECT COUNT(ID) FROM EMAIL WHERE EMAIL.PARENT_ID = ?", id);
	}

	/**
	 * Finds the number of replies that exist for an email, and all replies
	 * to those, and so on.
	 * @param id The parent email's id.
	 * @return The total number of replies to the given email, including children.
	 */
	public long countRepliesRecursive(long id) {
		int sum = 0;
		for (var reply : findAllReplies(id)) {
			sum += 1 + countRepliesRecursive(reply.id());
		}
		return sum;
	}

	/**
	 * Determines if an email has any replies.
	 * @param id The parent email id.
	 * @return True if this email has at least one reply, or false if not.
	 */
	public boolean hasReplies(long id) {
		return countReplies(id) == 0;
	}

	/**
	 * Gets just the body of an email identified by the given id.
	 * @param id The id of the email whose body to get.
	 * @return The body of the requested email.
	 */
	public Optional<String> getBody(long id) {
		try (var stmt = conn.prepareStatement("SELECT BODY FROM EMAIL WHERE ID = ?")) {
			stmt.setLong(1, id);
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
	 * @param id The id of the email to get the root of.
	 * @return The root email, if any was found.
	 */
	public Optional<EmailEntryPreview> findRootEmailByChildId(long id) {
		try (var stmt = conn.prepareStatement("SELECT PARENT_ID FROM EMAIL WHERE EMAIL.ID = ?")) {
			Long nextId = id;
			Long lastId = null;
			while (nextId != null) {
				stmt.setLong(1, nextId);
				var rs = stmt.executeQuery();
				if (!rs.next()) return Optional.empty();
				Long parentId = rs.getObject(1, Long.class);
				lastId = nextId;
				nextId = parentId;
			}
			try (var stmt2 = conn.prepareStatement(QueryCache.load("/sql/preview/fetch_email_preview_by_id.sql"))) {
				stmt2.setLong(1, lastId);
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
	 * Sets an email as hidden.
	 * @param id The id of the email.
	 */
	public void hideEmail(long id) {
		update(conn, "UPDATE EMAIL SET HIDDEN = TRUE WHERE ID = ?", id);
	}

	/**
	 * Sets an email as not hidden.
	 * @param id The id of the email.
	 */
	public void showEmail(long id) {
		update(conn, "UPDATE EMAIL SET HIDDEN = FALSE WHERE ID = ?", id);
	}

	private int hideEmailsByQuery(String msg, String conditions, Object... args) {
		try {
			conn.setAutoCommit(false);
			List<Long> ids = fetch(conn, "SELECT ID FROM EMAIL WHERE " + conditions, rs -> rs.getLong(1), args);
			long mId = insertWithId(conn, "INSERT INTO MUTATION (DESCRIPTION) VALUES (?)", msg);
			try (var stmt = conn.prepareStatement("INSERT INTO MUTATION_EMAIL(MUTATION_ID, EMAIL_ID) VALUES (?, ?)")) {
				stmt.setLong(1, mId);
				for (var id : ids) {
					stmt.setLong(2, id);
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
	 * It is recommended to call {@link nl.andrewl.email_indexer.gen.EmailIndexGenerator#generateIndex(EmailDataset)}
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

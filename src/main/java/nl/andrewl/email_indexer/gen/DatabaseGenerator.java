package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.QueryCache;
import nl.andrewl.email_indexer.util.DbUtils;
import nl.andrewl.email_indexer.util.Status;
import nl.andrewl.mboxparser.Email;
import nl.andrewl.mboxparser.EmailHandler;

import java.nio.file.Path;
import java.sql.*;
import java.time.ZonedDateTime;

/**
 * This component parses a set of mbox files to build relational database
 * containing the emails and their tree-structured relationships.
 */
public class DatabaseGenerator implements AutoCloseable, EmailHandler {
	private final Connection conn;
	private final PreparedStatement emailInsertStatement;
	private final PreparedStatement emailExistsStatement;

	public DatabaseGenerator(Path dbFile) throws SQLException {
		this.conn = DriverManager.getConnection(EmailDataset.getJdbcUrl(dbFile));
		initDatabase();
		this.conn.setAutoCommit(false);
		this.emailInsertStatement = this.conn.prepareStatement("""
			INSERT INTO EMAIL (MESSAGE_ID, SUBJECT, IN_REPLY_TO, SENT_FROM, DATE, BODY)
			VALUES (?, ?, ?, ?, ?, ?)""");
		this.emailExistsStatement = this.conn.prepareStatement("SELECT COUNT(MESSAGE_ID) FROM EMAIL WHERE MESSAGE_ID = ?;");
	}

	private void initDatabase() throws SQLException {
		try (Statement stmt = this.conn.createStatement()) {
			stmt.executeUpdate(QueryCache.load("/sql/schema.sql"));
		}
	}

	public void addEmail(Email email) throws SQLException {
		addEmail(email.messageId, email.subject, email.inReplyTo, email.sentFrom, email.date, email.readBodyAsText());
	}

	public synchronized void addEmail(String messageId, String subject, String inReplyTo, String sentFrom, ZonedDateTime date, String body) throws SQLException {
		emailExistsStatement.setString(1, messageId);
		try (var rs = emailExistsStatement.executeQuery()) {
			if (rs.next() && rs.getLong(1) > 0) return;
		}
		emailInsertStatement.setString(1, messageId);
		emailInsertStatement.setString(2, subject);
		emailInsertStatement.setString(3, inReplyTo);
		emailInsertStatement.setString(4, sentFrom);
		emailInsertStatement.setLong(5, date.toEpochSecond());
		emailInsertStatement.setString(6, body);
		emailInsertStatement.executeUpdate();
	}

	public Connection getConn() {
		return this.conn;
	}

	/**
	 * Performs post-processing steps on the data. This involves the following:
	 * <ul>
	 *     <li>Determine and set each email's PARENT_ID, based on their IN_REPLY_TO.</li>
	 * </ul>
	 * @param status A status tracker.
	 */
	public synchronized void postProcess(Status status) {
		long count = DbUtils.count(conn, "SELECT COUNT(ID) FROM EMAIL WHERE IN_REPLY_TO IS NOT NULL");
		status.sendMessage("Applying parent-id lookup for %d emails.".formatted(count));
		final int pageSize = 1000;
		int pageCount = (int) (count / pageSize) + (count % pageSize == 0 ? 0 : 1);
		DbUtils.doTransaction(conn, c -> {
			for (int page = 1; page <= pageCount; page++) {
				status.sendMessage("Processing page %d of %d.".formatted(page, pageCount));
				try (
					var fetchStmt = c.prepareStatement("""
						SELECT ID, IN_REPLY_TO
						FROM EMAIL
						WHERE IN_REPLY_TO IS NOT NULL
						LIMIT %d OFFSET %d""".formatted(pageSize, (page - 1) * pageSize));
					var fetchByMessageId = c.prepareStatement("""
						SELECT ID
						FROM EMAIL
						WHERE MESSAGE_ID = ?""");
					var updateParentId = c.prepareStatement("""
						UPDATE EMAIL
						SET PARENT_ID = ?
						WHERE ID = ?""")
				) {
					var rs = fetchStmt.executeQuery();
					while (rs.next()) {
						long id = rs.getLong(1);
						String inReplyTo = rs.getString(2);
						fetchByMessageId.setString(1, inReplyTo);
						var msgIdRs = fetchByMessageId.executeQuery();
						if (msgIdRs.next()) {
							long parentId = msgIdRs.getLong(1);
							updateParentId.setLong(1, parentId);
							updateParentId.setLong(2, id);
							updateParentId.executeUpdate();
						}
					}
				}
			}
		});
	}

	@Override
	public void close() throws Exception {
		this.emailExistsStatement.close();
		this.emailInsertStatement.close();
		this.conn.close();
	}

	@Override
	public void emailReceived(Email email) {
		try {
			addEmail(email);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

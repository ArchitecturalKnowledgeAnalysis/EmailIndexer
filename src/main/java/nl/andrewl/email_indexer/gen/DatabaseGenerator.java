package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.QueryCache;
import nl.andrewl.mboxparser.Email;
import nl.andrewl.mboxparser.EmailHandler;

import java.nio.file.Path;
import java.sql.*;

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

	public synchronized void addEmail(Email email) throws SQLException {
		// First check that no email with this id exists yet.
		this.emailExistsStatement.setString(1, email.messageId);
		var rs = this.emailExistsStatement.executeQuery();
		if (rs.next() && rs.getLong(1) > 0) return;

		this.emailInsertStatement.setString(1, email.messageId);
		this.emailInsertStatement.setString(2, email.subject);
		this.emailInsertStatement.setString(3, email.inReplyTo);
		this.emailInsertStatement.setString(4, email.sentFrom);
		this.emailInsertStatement.setObject(5, email.date);
		this.emailInsertStatement.setString(6, email.readBodyAsText());
		this.emailInsertStatement.executeUpdate();
	}

	@Override
	public void close() throws Exception {
		this.emailExistsStatement.close();
		this.emailInsertStatement.close();
		this.conn.commit();
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

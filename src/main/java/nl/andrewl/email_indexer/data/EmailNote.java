package nl.andrewl.email_indexer.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public record EmailNote(
		long id,
		long emailId,
		long createdAt,
		String message
) {
	public static EmailNote fromResultSet(ResultSet rs) throws SQLException {
		return new EmailNote(
				rs.getLong(1),
				rs.getLong(2),
				rs.getLong(3),
				rs.getString(4)
		);
	}
}

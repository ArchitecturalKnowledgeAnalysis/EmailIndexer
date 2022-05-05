package nl.andrewl.email_indexer.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

/**
 * More concise data about an email entry that is lightweight for use in search
 * results and other places where quick retrieval is necessary.
 */
public record EmailEntryPreview(
		long id,
		Long parentId,
		String messageId,
		String subject,
		String sentFrom,
		ZonedDateTime date,
		boolean hidden
) {
	public EmailEntryPreview (ResultSet rs) throws SQLException {
		this(
				rs.getLong(1),
				rs.getObject(2, Long.class),
				rs.getString(3),
				rs.getString(4),
				rs.getString(5),
				rs.getObject(6, ZonedDateTime.class),
				rs.getBoolean(7)
		);
	}
}

package nl.andrewl.email_indexer.data;

import nl.andrewl.email_indexer.util.DbUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
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
	public static EmailEntryPreview fromResultSet(ResultSet rs) throws SQLException {
		long id = rs.getLong(1);
		Long parentId = DbUtils.getNullableLong(rs, 2);
		String messageId = rs.getString(3);
		String subject = rs.getString(4);
		String sentFrom = rs.getString(5);
		ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(rs.getLong(6)), ZoneOffset.UTC);
		boolean hidden = rs.getBoolean(7);
		return new EmailEntryPreview(id, parentId, messageId, subject, sentFrom, date, hidden);
	}
}

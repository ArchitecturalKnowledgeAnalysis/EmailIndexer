package nl.andrewl.email_indexer.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * More concise data about an email entry that is lightweight for use in search
 * results and other places where quick retrieval is necessary.
 */
public record EmailEntryPreview(
		String messageId,
		String subject,
		String sentFrom,
		ZonedDateTime date,
		List<String> tags
) {
	public EmailEntryPreview (ResultSet rs) throws SQLException {
		this(
				rs.getString(1),
				rs.getString(2),
				rs.getString(3),
				rs.getObject(4, ZonedDateTime.class),
				new ArrayList<>()
		);
		String t = rs.getString(5);
		if (t != null && !t.isEmpty()) {
			Collections.addAll(tags, t.split(","));
			Collections.sort(tags);
		}
	}
}

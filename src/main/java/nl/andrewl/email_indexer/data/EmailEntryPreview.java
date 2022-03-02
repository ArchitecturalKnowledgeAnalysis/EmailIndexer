package nl.andrewl.email_indexer.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record EmailEntryPreview(
		String messageId,
		String subject,
		ZonedDateTime date,
		List<String> tags
) {
	public EmailEntryPreview (ResultSet rs) throws SQLException {
		this(
				rs.getString(1),
				rs.getString(2),
				rs.getObject(3, ZonedDateTime.class),
				new ArrayList<>()
		);
		String t = rs.getString(4);
		if (t != null && !t.isEmpty()) {
			Collections.addAll(tags, t.split(","));
			Collections.sort(tags);
		}
	}
}

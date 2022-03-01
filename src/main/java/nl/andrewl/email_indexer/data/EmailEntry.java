package nl.andrewl.email_indexer.data;

import java.time.ZonedDateTime;
import java.util.Set;

public record EmailEntry(
		String messageId,
		String subject,
		String inReplyTo,
		String sentFrom,
		ZonedDateTime date,
		String body,
		Set<String> tags
) {}

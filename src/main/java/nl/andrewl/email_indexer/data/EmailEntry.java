package nl.andrewl.email_indexer.data;

import java.time.ZonedDateTime;

/**
 * Represents a full email entry in the dataset.
 * @param id The internal id used to reference this email.
 * @param parentId The id of this email's parent, if there is one.
 * @param messageId The email's original message id.
 * @param subject The email's subject.
 * @param inReplyTo The email's "in reply to" field. This is originally used
 *                  to determine the email's parent.
 * @param sentFrom The address from which this email was sent.
 * @param date The date at which this email was sent.
 * @param body The body of the email.
 * @param hidden Whether this email has been hidden.
 */
public record EmailEntry(
		long id,
		Long parentId,
		String messageId,
		String subject,
		String inReplyTo,
		String sentFrom,
		ZonedDateTime date,
		String body,
		boolean hidden
) {}

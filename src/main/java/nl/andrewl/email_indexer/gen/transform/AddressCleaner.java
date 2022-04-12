package nl.andrewl.email_indexer.gen.transform;

import nl.andrewl.mboxparser.Email;

import java.util.function.Consumer;

public class AddressCleaner implements Consumer<Email> {
    @Override
    public void accept(Email email) {
        if (email.messageId.startsWith("<") && email.messageId.endsWith(">")) {
            email.messageId = email.messageId.substring(1, email.messageId.length() - 1);
        }
        if (email.inReplyTo != null && email.inReplyTo.startsWith("<") && email.inReplyTo.endsWith(">")) {
            email.inReplyTo = email.inReplyTo.substring(1, email.inReplyTo.length() - 1);
        }
        if (email.sentFrom != null && email.sentFrom.startsWith("<") && email.sentFrom.endsWith(">")) {
            email.sentFrom = email.sentFrom.substring(1, email.sentFrom.length() - 1);
        }
    }
}

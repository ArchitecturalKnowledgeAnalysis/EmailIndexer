package nl.andrewl.email_indexer.gen.transform;

import nl.andrewl.mboxparser.Email;

import java.util.function.Consumer;

public class AddressCleaner implements Consumer<Email> {
    @Override
    public void accept(Email email) {
        email.messageId = strip("<", ">", email.messageId);
        email.inReplyTo = strip("<", ">", email.inReplyTo);
        email.sentFrom = strip("<", ">", email.sentFrom);
    }

    private String strip(String pre, String post, String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith(pre) && s.endsWith(post)) {
            s = s.substring(pre.length(), s.length() - post.length());
        }
        s = s.trim();
        if (s.isBlank()) return null;
        return s;
    }
}

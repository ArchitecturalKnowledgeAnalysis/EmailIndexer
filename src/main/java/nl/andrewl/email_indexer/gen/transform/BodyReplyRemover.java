package nl.andrewl.email_indexer.gen.transform;

import nl.andrewl.mboxparser.Email;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class BodyReplyRemover implements Consumer<Email> {
    @Override
    public void accept(Email email) {
        String[] lines = email.readBodyAsText().split("\n");
        StringBuilder sb = new StringBuilder(email.body.length);
        for (var line : lines) {
            // Quit if we see this: it means we're about to read a quoted original message in a reply.
            if (line.trim().equalsIgnoreCase("-----Original Message-----")) break;
            // Only add lines that don't start with ">" since this character is used to indicate quotes in 99% of cases.
            if (!line.trim().startsWith(">")) {
                sb.append(line).append("\n");
            }
        }
        email.body = sb.toString().getBytes(StandardCharsets.UTF_8);
        email.charset = StandardCharsets.UTF_8.name();
        email.transferEncoding = "8bit";
    }
}

package nl.andrewl.email_indexer.gen;

import nl.andrewl.mbox_parser.Email;
import nl.andrewl.mbox_parser.EmailHandler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A simple handler that helps to sanitize emails by removing those without a
 * charset, and doing a best-effort attempt to remove annoying indented reply
 * content.
 */
public class SanitizingEmailHandler implements EmailHandler {
	private final EmailHandler handler;
	private final List<Function<Email, Boolean>> filterFunctions;
	private final List<Consumer<Email>> transformers;

	public SanitizingEmailHandler(EmailHandler handler) {
		this.handler = handler;

		this.filterFunctions = new ArrayList<>();
		filterFunctions.add(email -> email.charset != null);
		filterFunctions.add(email -> email.body.length > 0);

		this.transformers = new ArrayList<>();
		transformers.add(email -> {
			String[] lines = email.readBodyAsText().split("\n");
			StringBuilder sb = new StringBuilder(email.body.length);
			for (var line : lines) {
				if (!line.trim().startsWith(">")) {
					sb.append(line).append("\n");
				}
			}
			email.body = sb.toString().getBytes(StandardCharsets.UTF_8);
			email.charset = StandardCharsets.UTF_8.name();
			email.transferEncoding = "8bit";
		});
	}

	@Override
	public void emailReceived(Email email) {
		for (var f : filterFunctions) {
			if (!f.apply(email)) return;
		}
		for (var t : transformers) {
			t.accept(email);
		}
		handler.emailReceived(email);
	}
}

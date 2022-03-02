package nl.andrewl.email_indexer.gen;

import nl.andrewl.mbox_parser.Email;
import nl.andrewl.mbox_parser.EmailHandler;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class FilterTransformEmailHandler implements EmailHandler {
	private final EmailHandler handler;
	private final List<Function<Email, Boolean>> filterFunctions;
	private final List<Consumer<Email>> transformers;

	public FilterTransformEmailHandler(EmailHandler handler, List<Function<Email, Boolean>> filterFunctions, List<Consumer<Email>> transformers) {
		this.handler = handler;
		this.filterFunctions = filterFunctions;
		this.transformers = transformers;
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

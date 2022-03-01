package nl.andrewl.email_indexer.gen;

import nl.andrewl.mbox_parser.Email;
import nl.andrewl.mbox_parser.EmailHandler;

import java.util.List;
import java.util.function.Function;

public class FilterEmailHandler implements EmailHandler {
	private final EmailHandler handler;
	private final List<Function<Email, Boolean>> filterFunctions;

	public FilterEmailHandler(EmailHandler handler, List<Function<Email, Boolean>> filterFunctions) {
		this.handler = handler;
		this.filterFunctions = filterFunctions;
	}

	@Override
	public void emailReceived(Email email) {
		for (var f : filterFunctions) {
			if (!f.apply(email)) return;
		}
		handler.emailReceived(email);
	}
}

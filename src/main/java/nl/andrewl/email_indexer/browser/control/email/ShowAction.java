package nl.andrewl.email_indexer.browser.control.email;

import nl.andrewl.email_indexer.browser.email.EmailViewPanel;
import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;

import java.awt.event.ActionEvent;

public class ShowAction extends EmailAction {
	public ShowAction(EmailViewPanel emailViewPanel) {
		super("Show", emailViewPanel);
	}

	@Override
	protected boolean shouldBeEnabled(EmailEntry email) {
		return email.hidden();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		new EmailRepository(emailViewPanel.getCurrentDataset()).showEmail(emailViewPanel.getEmail().messageId());
		emailViewPanel.refresh();
	}
}

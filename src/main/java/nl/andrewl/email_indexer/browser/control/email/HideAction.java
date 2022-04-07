package nl.andrewl.email_indexer.browser.control.email;

import nl.andrewl.email_indexer.browser.email.EmailViewPanel;
import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;

import java.awt.event.ActionEvent;

public class HideAction extends EmailAction {
	public HideAction(EmailViewPanel emailViewPanel) {
		super("Hide", emailViewPanel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		new EmailRepository(emailViewPanel.getCurrentDataset()).hideEmail(emailViewPanel.getEmail().messageId());
		emailViewPanel.refresh();
	}

	@Override
	protected boolean shouldBeEnabled(EmailEntry email) {
		return !email.hidden();
	}
}

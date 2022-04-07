package nl.andrewl.email_indexer.browser.control.email;

import nl.andrewl.email_indexer.browser.email.EmailViewListener;
import nl.andrewl.email_indexer.browser.email.EmailViewPanel;
import nl.andrewl.email_indexer.data.EmailEntry;

import javax.swing.*;

public abstract class EmailAction extends AbstractAction implements EmailViewListener {
	protected final EmailViewPanel emailViewPanel;

	protected EmailAction(String name, EmailViewPanel emailViewPanel) {
		super(name);
		this.emailViewPanel = emailViewPanel;
		emailViewPanel.addListener(this);
	}

	@Override
	public void emailUpdated(EmailEntry email) {
		setEnabled(email != null && shouldBeEnabled(email));
	}

	protected abstract boolean shouldBeEnabled(EmailEntry email);
}

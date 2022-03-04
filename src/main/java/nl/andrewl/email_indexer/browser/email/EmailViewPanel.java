package nl.andrewl.email_indexer.browser.email;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;

import javax.swing.*;
import java.awt.*;
import java.util.Stack;

public class EmailViewPanel extends JPanel {
	private EmailDataset currentDataset = null;
	private EmailEntry email;
	private Stack<String> navigationStack = new Stack<>();

	private final JTextPane emailTextPane;
	private final EmailInfoPanel infoPanel;
	private final JButton backButton;

	public EmailViewPanel() {
		this.setLayout(new BorderLayout());
		this.emailTextPane = new JTextPane();
		this.emailTextPane.setEditable(false);
		JScrollPane emailScrollPane = new JScrollPane(this.emailTextPane);
		this.add(emailScrollPane, BorderLayout.CENTER);

		this.infoPanel = new EmailInfoPanel(this);
		this.infoPanel.setPreferredSize(new Dimension(400, -1));
		this.add(this.infoPanel, BorderLayout.EAST);
		setEmail(null);
		JPanel navbar = new JPanel();
		backButton = new JButton("Back");
		backButton.addActionListener(e -> {
			navigateBack();
		});
		navbar.add(backButton);
		this.add(navbar, BorderLayout.NORTH);
	}

	public void setDataset(EmailDataset dataset) {
		this.currentDataset = dataset;
		setEmail(null);
	}

	public EmailDataset getCurrentDataset() {
		return this.currentDataset;
	}

	private void setEmail(EmailEntry email) {
		this.email = email;
		if (email != null) {
			this.emailTextPane.setText(email.body());
			this.emailTextPane.setCaretPosition(0);
		} else {
			this.emailTextPane.setText(null);
		}
		this.infoPanel.setEmail(email);
		this.infoPanel.setVisible(email != null);
	}

	private void fetchAndSetEmail(String messageId) {
		if (this.currentDataset != null) {
			new EmailRepository(currentDataset).findEmailById(messageId)
					.ifPresentOrElse(this::setEmail, () -> setEmail(null));
		} else {
			setEmail(null);
		}
	}

	public void navigateTo(String messageId) {
		fetchAndSetEmail(messageId);
		navigationStack.push(messageId);
		backButton.setEnabled(true);
	}

	public void navigateTo(EmailEntry email) {
		setEmail(email);
		navigationStack.push(email.messageId());
		backButton.setEnabled(true);
	}

	public void navigateBack() {
		if (navigationStack.size() > 1) {
			navigationStack.pop();
			fetchAndSetEmail(navigationStack.peek());
		}
		backButton.setEnabled(navigationStack.size() > 1);
	}

	public void startNavigate(EmailEntry email) {
		setEmail(email);
		navigationStack.clear();
		navigationStack.push(email.messageId());
		backButton.setEnabled(false);
	}

	public void refresh() {
		if (this.email != null) fetchAndSetEmail(email.messageId());
	}
}

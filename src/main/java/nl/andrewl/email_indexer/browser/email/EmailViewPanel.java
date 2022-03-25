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
	private final Stack<String> navigationStack = new Stack<>();

	private final JTextPane emailTextPane;
	private final EmailInfoPanel infoPanel;
	private final JButton backButton;
	private final JButton removeSimilarButton;
	private final JButton removeAuthorButton;

	public EmailViewPanel() {
		this.setLayout(new BorderLayout());
		this.emailTextPane = new JTextPane();
		this.emailTextPane.setEditable(false);
		this.emailTextPane.setFont(new Font("monospaced", emailTextPane.getFont().getStyle(), 16));
		this.emailTextPane.setBackground(this.emailTextPane.getBackground().darker());
		JScrollPane emailScrollPane = new JScrollPane(this.emailTextPane);
		this.add(emailScrollPane, BorderLayout.CENTER);

		this.infoPanel = new EmailInfoPanel(this);
		this.infoPanel.setPreferredSize(new Dimension(400, -1));
		this.add(this.infoPanel, BorderLayout.EAST);

		JPanel navbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		backButton = new JButton("Back");
		backButton.addActionListener(e -> {
			navigateBack();
		});
		removeSimilarButton = new JButton("Hide all with same body");
		removeSimilarButton.addActionListener(e -> {
			long removed = new EmailRepository(this.currentDataset).hideAllEmailsByBody(this.email.body());
			JOptionPane.showMessageDialog(
					this,
					"Removed %d emails.".formatted(removed),
					"Removed Emails",
					JOptionPane.INFORMATION_MESSAGE
			);
		});
		removeAuthorButton = new JButton("Hide all sent by this author");
		removeAuthorButton.addActionListener(e -> {
			String emailAddress = email.sentFrom().substring(email.sentFrom().lastIndexOf('<') + 1, email.sentFrom().length() - 1);
			long removed = new EmailRepository(this.currentDataset).hideAllEmailsBySentFrom('%' + emailAddress + '%');
			JOptionPane.showMessageDialog(
					this,
					"Removed %d emails.".formatted(removed),
					"Removed Emails",
					JOptionPane.INFORMATION_MESSAGE
			);
		});
		navbar.add(removeSimilarButton);
		navbar.add(removeAuthorButton);
		navbar.add(backButton);
		this.add(navbar, BorderLayout.NORTH);

		setEmail(null);
	}

	public void setDataset(EmailDataset dataset) {
		this.currentDataset = dataset;
		setEmail(null);
		clearNavigation();
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
		removeSimilarButton.setEnabled(email != null);
		removeAuthorButton.setEnabled(email != null);
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

	public void clearNavigation() {
		navigationStack.clear();
		backButton.setEnabled(false);
	}

	public void refresh() {
		if (this.email != null) fetchAndSetEmail(email.messageId());
	}
}

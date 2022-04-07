package nl.andrewl.email_indexer.browser.email;

import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RepliesPanel extends JPanel implements EmailViewListener {
	private final EmailViewPanel parent;

	private final JPanel buttonPanel;

	public RepliesPanel(EmailViewPanel parent) {
		super(new BorderLayout());
		this.parent = parent;
		this.setBorder(BorderFactory.createTitledBorder("Replies"));
		this.buttonPanel = new JPanel();
		this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.PAGE_AXIS));
		JScrollPane scrollPane = new JScrollPane(buttonPanel);
		this.add(scrollPane, BorderLayout.CENTER);
	}

	public void setEmail(EmailEntry email) {
		List<JButton> buttonsToAdd = new ArrayList<>();
		if (email != null) {
			var repo = new EmailRepository(parent.getCurrentDataset());
			var replies = repo.findAllReplies(email.messageId());
			for (var reply : replies) {
				JButton button = new JButton("<html><strong>%s</strong><br>by <em>%s</em></html>".formatted(reply.subject(), reply.sentFrom()));
				button.addActionListener(e -> SwingUtilities.invokeLater(() -> parent.navigateTo(reply.messageId())));
				buttonsToAdd.add(button);
			}
		}
		SwingUtilities.invokeLater(() -> {
			buttonPanel.removeAll();
			buttonsToAdd.forEach(buttonPanel::add);
			System.out.println(buttonPanel.getComponents().length);
			buttonPanel.repaint();
		});
	}

	@Override
	public void emailUpdated(EmailEntry email) {
		setEmail(email);
	}
}

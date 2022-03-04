package nl.andrewl.email_indexer.browser.email;

import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;

import javax.swing.*;
import java.awt.*;

public class RepliesPanel extends JPanel {
	private final EmailViewPanel parent;

	private final JPanel buttonPanel;

	public RepliesPanel(EmailViewPanel parent) {
		super(new BorderLayout());
		this.parent = parent;
		this.buttonPanel = new JPanel();
		this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.PAGE_AXIS));
		JScrollPane scrollPane = new JScrollPane(buttonPanel);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(new JLabel("Replies"), BorderLayout.NORTH);
	}

	public void setEmail(EmailEntry email) {
		this.buttonPanel.removeAll();
		if (email == null) return;
		var repo = new EmailRepository(parent.getCurrentDataset());
		var replies = repo.findAllReplies(email.messageId());
		for (var reply : replies) {
			JButton button = new JButton(reply.subject());
			button.addActionListener(e -> {
				SwingUtilities.invokeLater(() -> parent.navigateTo(reply.messageId()));
			});
			this.buttonPanel.add(button);
		}
		this.buttonPanel.repaint();
	}
}

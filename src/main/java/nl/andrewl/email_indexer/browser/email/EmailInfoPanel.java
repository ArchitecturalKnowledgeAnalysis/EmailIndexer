package nl.andrewl.email_indexer.browser.email;

import nl.andrewl.email_indexer.data.EmailEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;

public class EmailInfoPanel extends JPanel {
	private final EmailViewPanel parent;

	private final JLabel messageIdLabel = new JLabel();
	private final JLabel subjectLabel = new JLabel();
	private final JButton inReplyToButton = new JButton("None");
	private ActionListener inReplyToActionListener;
	private final JLabel dateLabel = new JLabel();
	private final JLabel sentFromLabel = new JLabel();
	private final TagPanel tagPanel;
	private final RepliesPanel repliesPanel;

	public EmailInfoPanel(EmailViewPanel parent) {
		super(new GridBagLayout());
		this.parent = parent;
		this.tagPanel = new TagPanel(parent);
		this.tagPanel.setPreferredSize(new Dimension(-1, 200));
		this.repliesPanel = new RepliesPanel(parent);
		this.repliesPanel.setPreferredSize(new Dimension(-1, 200));

		GridBagConstraints labelConstraint = new GridBagConstraints();
		labelConstraint.anchor = GridBagConstraints.FIRST_LINE_START;
		labelConstraint.weightx = 0.01;
		labelConstraint.weighty = 0.01;
		labelConstraint.insets = new Insets(3, 3, 3, 3);
		labelConstraint.gridx = 0;
		labelConstraint.gridy = 0;
		String[] labels = new String[]{"Message Id", "Subject", "In Reply To", "Sent From", "Date"};
		for (var l : labels) {
			var label = new JLabel(l);
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			this.add(label, labelConstraint);
			labelConstraint.gridy++;
		}

		GridBagConstraints fieldConstraint = new GridBagConstraints();
		fieldConstraint.anchor = GridBagConstraints.FIRST_LINE_START;
		fieldConstraint.weightx = 0.99;
		fieldConstraint.weighty = 0.01;
		fieldConstraint.insets = new Insets(3, 3, 3, 3);
		fieldConstraint.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraint.gridx = 1;
		fieldConstraint.gridy = 0;
		Component[] values = new Component[]{messageIdLabel, subjectLabel, inReplyToButton, sentFromLabel, dateLabel};
		for (var v : values) {
			this.add(v, fieldConstraint);
			fieldConstraint.gridy++;
		}

		// Add complex sections.
		GridBagConstraints subsectionConstraint = new GridBagConstraints();
		subsectionConstraint.anchor = GridBagConstraints.FIRST_LINE_START;
		subsectionConstraint.weightx = 1;
		subsectionConstraint.weighty = 0.99;
		subsectionConstraint.fill = GridBagConstraints.BOTH;
		subsectionConstraint.gridwidth = 2;
		subsectionConstraint.gridy = values.length;
		subsectionConstraint.insets = new Insets(3, 3, 3, 3);
		this.add(this.tagPanel, subsectionConstraint);
		subsectionConstraint.gridy++;
		this.add(this.repliesPanel, subsectionConstraint);

		// Add filler space.
//		c.gridy = labels.length;
//		c.gridx = 0;
//		c.gridwidth = 2;
//		c.weighty = 1;
//		JPanel filler = new JPanel();
//		contentPanel.add(filler, c);

//		JScrollPane scrollPane = new JScrollPane(contentPanel);
//		this.add(scrollPane);
	}

	public void setEmail(EmailEntry email) {
		if (email != null) {
			this.messageIdLabel.setText(email.messageId());
			this.subjectLabel.setText(email.subject());
			this.dateLabel.setText(email.date().format(DateTimeFormatter.ofPattern("dd MMMM, yyyy HH:mm:ss Z")));
			this.sentFromLabel.setText(email.sentFrom());
			String inReplyToButtonText = email.inReplyTo() == null || email.inReplyTo().isBlank() ? "None" : email.inReplyTo();
			this.inReplyToButton.setText(inReplyToButtonText);
			this.inReplyToButton.setEnabled(email.inReplyTo() != null && !email.inReplyTo().isBlank());
			if (inReplyToActionListener != null) inReplyToButton.removeActionListener(inReplyToActionListener);
			inReplyToActionListener = e -> {
				SwingUtilities.invokeLater(() -> parent.navigateTo(email.inReplyTo()));
			};
			inReplyToButton.addActionListener(inReplyToActionListener);
		} else {
			this.inReplyToButton.setEnabled(false);
		}
		this.tagPanel.setEmail(email);
		this.repliesPanel.setEmail(email);
	}
}

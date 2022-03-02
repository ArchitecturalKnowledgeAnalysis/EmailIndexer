package nl.andrewl.email_indexer.browser;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntry;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class EmailViewPanel extends JPanel {
	private EmailDataset currentDataset = null;
	private EmailEntry email;

	private final JTextPane emailTextPane;

	private final JPanel sidePanel;
	private final JLabel messageIdLabel = new JLabel();
	private final JLabel subjectLabel = new JLabel();
	private final JLabel inReplyToLabel = new JLabel();
	private final JLabel dateLabel = new JLabel();
	private final JLabel sentFromLabel = new JLabel();
	private final TagPanel tagPanel;

	public EmailViewPanel() {
		this.setLayout(new BorderLayout());
		this.emailTextPane = new JTextPane();
		this.emailTextPane.setEditable(false);
		JScrollPane emailScrollPane = new JScrollPane(this.emailTextPane);
		this.add(emailScrollPane, BorderLayout.CENTER);

		sidePanel = new JPanel();
		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
		sidePanel.setPreferredSize(new Dimension(300, -1));

		this.tagPanel = new TagPanel(this);

		JPanel infoPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets(3, 3, 3, 3);
		c.weighty = 0;
		String[] labels = new String[]{"Message Id", "Subject", "In Reply To", "Sent From", "Date", "Tags"};
		Component[] values = new Component[]{messageIdLabel, subjectLabel, inReplyToLabel, sentFromLabel, dateLabel, tagPanel};
		for (int i = 0; i < labels.length; i++) {
			c.gridy = i;
			c.gridx = 0;
			c.weightx = 0.01;
			var label = new JLabel(labels[i]);
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			infoPanel.add(label, c);
			c.gridx = 1;
			c.weightx = 0.99;
			infoPanel.add(values[i], c);
		}
		// Add filler space.
		c.gridy = labels.length;
		c.gridx = 0;
		c.gridwidth = 2;
		c.weighty = 1;
		JPanel filler = new JPanel();
		infoPanel.add(filler, c);
		sidePanel.add(new JScrollPane(infoPanel));


//		sidePanel.add(tagPanel);

		this.add(sidePanel, BorderLayout.EAST);
		setEmail(null);
	}

	public void setDataset(EmailDataset dataset) {
		this.currentDataset = dataset;
		setEmail(null);
	}

	public EmailDataset getCurrentDataset() {
		return this.currentDataset;
	}

	public void setEmail(EmailEntry email) {
		if (email != null) {
			this.email = email;
			this.emailTextPane.setText(email.body());
			this.emailTextPane.setCaretPosition(0);

			this.messageIdLabel.setText(email.messageId());
			this.subjectLabel.setText(email.subject());
			this.inReplyToLabel.setText(email.inReplyTo());
			this.dateLabel.setText(email.date().format(DateTimeFormatter.ofPattern("dd MMMM, yyyy HH:mm:ss Z")));
			this.sentFromLabel.setText(email.sentFrom());
			this.sidePanel.setVisible(true);
		} else {
			this.emailTextPane.setText(null);
			this.sidePanel.setVisible(false);
		}
		tagPanel.setEmail(email);
	}

	public void refresh() {
		if (this.currentDataset != null) {
			this.currentDataset.findEmailById(email.messageId())
					.ifPresentOrElse(this::setEmail, () -> setEmail(null));
		} else {
			setEmail(null);
		}
	}
}

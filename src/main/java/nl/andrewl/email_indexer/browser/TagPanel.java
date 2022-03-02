package nl.andrewl.email_indexer.browser;

import nl.andrewl.email_indexer.data.EmailEntry;

import javax.swing.*;
import java.awt.*;

public class TagPanel extends JPanel {
	private final DefaultListModel<String> tagListModel = new DefaultListModel<>();
	private EmailEntry email = null;
	private final JButton removeButton = new JButton("Remove");

	public TagPanel(EmailViewPanel parent) {
		super(new BorderLayout());

		this.removeButton.setEnabled(false);
		JList<String> tagList = new JList<>(this.tagListModel);
		tagList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tagList.getSelectionModel().addListSelectionListener(e -> {
			removeButton.setEnabled(tagList.getSelectedIndices().length > 0);
		});
		JScrollPane scroller = new JScrollPane(tagList);
		this.add(scroller, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton addButton = new JButton("Add");
		addButton.addActionListener(e -> {
			String tag = JOptionPane.showInputDialog("Enter a tag.");
			if (tag != null) {
				parent.getCurrentDataset().addTag(email.messageId(), tag);
				parent.refresh();
			}
		});
		buttonPanel.add(addButton);
		removeButton.addActionListener(e -> {
			for (var tag : tagList.getSelectedValuesList()) {
				parent.getCurrentDataset().removeTag(email.messageId(), tag);
			}
			parent.refresh();
		});
		buttonPanel.add(removeButton);
		this.add(buttonPanel, BorderLayout.SOUTH);
	}

	public void setEmail(EmailEntry email) {
		this.email = email;
		this.tagListModel.clear();
		if (email != null) {
			this.tagListModel.addAll(email.tags());
		}
	}
}

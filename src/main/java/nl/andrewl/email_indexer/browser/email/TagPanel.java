package nl.andrewl.email_indexer.browser.email;

import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that's used to manage the tags belonging to a single email entry. It
 * shows the list of tags, and provides facilities to modify that list.
 */
public class TagPanel extends JPanel {
	private final EmailViewPanel parent;
	private final DefaultListModel<String> tagListModel = new DefaultListModel<>();
	private final DefaultComboBoxModel<String> tagComboBoxModel = new DefaultComboBoxModel<>();
	private EmailEntry email = null;
	private final JButton removeButton = new JButton("Remove");

	public TagPanel(EmailViewPanel parent) {
		super(new BorderLayout());
		this.parent = parent;
		this.add(new JLabel("Tags"), BorderLayout.NORTH);
		this.removeButton.setEnabled(false);
		JList<String> tagList = new JList<>(this.tagListModel);
		tagList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tagList.setCellRenderer(new TagListCellRenderer());
		tagList.getSelectionModel().addListSelectionListener(e -> {
			SwingUtilities.invokeLater(() -> {
				removeButton.setEnabled(tagList.getSelectedIndices().length > 0);
			});
		});
		JScrollPane scroller = new JScrollPane(tagList);
		this.add(scroller, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JComboBox<String> tagComboBox = new JComboBox<>(this.tagComboBoxModel);
		tagComboBox.setEditable(true);
		buttonPanel.add(tagComboBox);
		JButton addButton = new JButton("Add");
		addButton.addActionListener(e -> {
			String tag = (String) tagComboBox.getSelectedItem();
			if (tag != null) {
				new EmailRepository(parent.getCurrentDataset()).addTag(email.messageId(), tag);
				parent.refresh();
			}
		});
		buttonPanel.add(addButton);
		removeButton.addActionListener(e -> {
			var repo = new EmailRepository(parent.getCurrentDataset());
			for (var tag : tagList.getSelectedValuesList()) {
				repo.removeTag(email.messageId(), tag);
			}
			parent.refresh();
		});
		buttonPanel.add(removeButton);
		this.add(buttonPanel, BorderLayout.SOUTH);
	}

	public void setEmail(EmailEntry email) {
		this.email = email;
		this.tagListModel.clear();
		this.tagComboBoxModel.removeAllElements();
		if (email != null) {
			this.tagListModel.addAll(email.tags());
			this.tagComboBoxModel.addAll(new EmailRepository(parent.getCurrentDataset()).getAllTags());
		}
	}
}

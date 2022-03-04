package nl.andrewl.email_indexer.browser;

import nl.andrewl.email_indexer.browser.email.EmailViewPanel;
import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.EmailSearchResult;

import javax.swing.*;
import java.awt.*;

public class SearchPanel extends JPanel {
	private final JTextField searchField;
	private final DefaultListModel<EmailEntryPreview> emailListModel;
	private EmailDataset currentDataset;

	public SearchPanel(EmailViewPanel emailViewPanel) {
		super(new BorderLayout());
		this.setPreferredSize(new Dimension(300, -1));

		this.searchField = new JTextField(16);
		JButton searchButton = new JButton("Search");
		JPanel searchPanel = new JPanel();
		searchPanel.add(this.searchField);
		searchPanel.add(searchButton);
		this.add(searchPanel, BorderLayout.NORTH);

		this.emailListModel = new DefaultListModel<>();
		JList<EmailEntryPreview> emailList = new JList<>(this.emailListModel);
		emailList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		emailList.setCellRenderer(new EmailListItemRenderer());
		emailList.addListSelectionListener(e -> {
			var selected = emailList.getSelectedValue();
			if (selected != null) {
				new EmailRepository(currentDataset).findEmailById(selected.messageId()).ifPresent(emailViewPanel::startNavigate);
			}
		});
		JScrollPane listScroller = new JScrollPane(emailList);
		this.add(listScroller, BorderLayout.CENTER);

		searchButton.addActionListener(e -> {
			if (this.currentDataset != null) {
				var results = new EmailRepository(currentDataset).search(this.searchField.getText(), 1, 100);
				showResults(results);
			}
		});
	}

	public void setDataset(EmailDataset ds) {
		this.currentDataset = ds;
		this.emailListModel.clear();
		if (ds != null) {
			showResults(new EmailRepository(currentDataset).findAll(1, 100));
		}
	}

	private void showResults(EmailSearchResult result) {
		this.emailListModel.clear();
		this.emailListModel.addAll(result.emails());
	}
}

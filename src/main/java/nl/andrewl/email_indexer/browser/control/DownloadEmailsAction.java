package nl.andrewl.email_indexer.browser.control;

import nl.andrewl.apache_email_downloader.ApacheMailingListDownloader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;

public class DownloadEmailsAction extends AbstractAction {
	private final Window owner;

	public DownloadEmailsAction(Window owner) {
		super("Download Emails");
		this.owner = owner;
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		JDialog dialog = new JDialog(owner, "Download ", Dialog.ModalityType.APPLICATION_MODAL);
		JPanel p = new JPanel(new BorderLayout());

		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.PAGE_AXIS));
		JTextField domainField = new JTextField(0);
		JTextField listField = new JTextField(0);
		JPanel domainPanel = new JPanel(new BorderLayout());
		domainPanel.add(new JLabel("Domain"), BorderLayout.WEST);
		domainPanel.add(domainField, BorderLayout.CENTER);
		inputPanel.add(domainPanel);
		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.add(new JLabel("List"), BorderLayout.WEST);
		listPanel.add(listField, BorderLayout.CENTER);
		inputPanel.add(listPanel);
		PathSelectField dirField = PathSelectField.directorySelectField();
		inputPanel.add(dirField);

		p.add(inputPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(event -> dialog.dispose());
		JButton downloadButton = new JButton("Download");
		downloadButton.addActionListener(event -> {
			ApacheMailingListDownloader downloader = new ApacheMailingListDownloader(domainField.getText(), listField.getText());
			Path outputDir = dirField.getSelectedPath();
			domainField.setEditable(false);
			listField.setEditable(false);
			dirField.setEnabled(false);
			downloadButton.setEnabled(false);
			cancelButton.setEnabled(false);
			dialog.setTitle("Downloading...");
			downloader.downloadAll(outputDir, null, null)
					.exceptionally(throwable -> {
						throwable.printStackTrace();
						return new ArrayList<>();
					})
					.thenRun(() -> {
						JOptionPane.showMessageDialog(
								dialog,
								"MBox files downloaded successfully.",
								"Done",
								JOptionPane.INFORMATION_MESSAGE
						);
						dialog.dispose();
					});
		});
		buttonPanel.add(downloadButton);
		buttonPanel.add(cancelButton);
		p.add(buttonPanel, BorderLayout.SOUTH);

		dialog.setContentPane(p);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
	}
}

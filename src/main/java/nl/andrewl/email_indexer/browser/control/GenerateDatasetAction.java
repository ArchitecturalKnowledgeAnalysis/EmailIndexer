package nl.andrewl.email_indexer.browser.control;

import nl.andrewl.email_indexer.browser.EmailDatasetBrowser;
import nl.andrewl.email_indexer.gen.EmailDatasetGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class GenerateDatasetAction extends AbstractAction {
	private final EmailDatasetBrowser browser;

	public GenerateDatasetAction(EmailDatasetBrowser browser) {
		super("Generate Dataset");
		this.browser = browser;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JDialog dialog = new JDialog(browser, "Generate Dataset", true);
		JPanel p = new JPanel(new BorderLayout());
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.PAGE_AXIS));

		var items = buildMBoxDirsPanel(dialog);
		inputPanel.add(items.getKey());
		JList<Path> mboxDirsList = items.getValue();

		JPanel datasetDirPanel = new JPanel(new BorderLayout());
		JTextField datasetDirField = new JTextField(0);
		datasetDirField.setEditable(false);
		JButton datasetDirButton = new JButton("Generate to");
		datasetDirButton.addActionListener(event -> {
			JFileChooser fc = new JFileChooser(Path.of(".").toFile());
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fc.showSaveDialog(browser);
			if (result == JFileChooser.APPROVE_OPTION) {
				Path dsDir = fc.getSelectedFile().toPath();
				datasetDirField.setText(dsDir.toAbsolutePath().toString());
			}
		});
		datasetDirPanel.add(datasetDirButton, BorderLayout.EAST);
		datasetDirPanel.add(datasetDirField, BorderLayout.CENTER);
		inputPanel.add(datasetDirPanel);

		p.add(inputPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(event -> dialog.dispose());
		JButton generateButton = new JButton("Generate");
		generateButton.addActionListener(event -> {
			int size = mboxDirsList.getModel().getSize();
			if (size < 1) {
				JOptionPane.showMessageDialog(
						dialog,
						"No MBox directories have been added.",
						"No MBox Directories",
						JOptionPane.WARNING_MESSAGE
				);
			} else {
				Collection<Path> paths = new ArrayList<>();
				for (int i = 0; i < size; i++) {
					paths.add(mboxDirsList.getModel().getElementAt(i));
				}
				Path dsDir = Path.of(datasetDirField.getText());
				generateButton.setEnabled(false);
				cancelButton.setEnabled(false);
				dialog.setTitle("Generating...");
				try {
					new EmailDatasetGenerator().generate(paths, dsDir)
							.thenRun(() -> {
								JOptionPane.showMessageDialog(dialog, "Dataset generated!");
								dialog.dispose();
							});
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(
							dialog,
							"An error occurred while generating the dataset:\n" + ex.getMessage(),
							"Error",
							JOptionPane.ERROR_MESSAGE
					);
					dialog.dispose();
				}
			}
		});
		buttonPanel.add(generateButton);
		buttonPanel.add(cancelButton);
		p.add(buttonPanel, BorderLayout.SOUTH);

		dialog.setContentPane(p);
		dialog.pack();
		dialog.setLocationRelativeTo(browser);
		dialog.setVisible(true);
	}

	private Map.Entry<JPanel, JList<Path>> buildMBoxDirsPanel(JDialog owner) {
		DefaultListModel<Path> mboxDirsListModel = new DefaultListModel<>();
		JList<Path> mboxDirsList = new JList<>(mboxDirsListModel);
		mboxDirsList.setPreferredSize(new Dimension(500, 300));
		JPanel mboxDirsPanel = new JPanel(new BorderLayout());
		mboxDirsPanel.add(new JScrollPane(mboxDirsList), BorderLayout.CENTER);
		JPanel mboxDirsButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton addMboxDirButton = new JButton("Add Mbox Directory");
		addMboxDirButton.addActionListener(event -> {
			JFileChooser fc = new JFileChooser(Path.of(".").toFile());
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fc.showOpenDialog(browser);
			if (result == JFileChooser.APPROVE_OPTION) {
				Path mboxDir = fc.getSelectedFile().toPath();
				if (!mboxDirsListModel.contains(mboxDir)) {
					try (var s = Files.list(mboxDir)) {
						if (s.noneMatch(file -> file.getFileName().toString().toLowerCase().endsWith(".mbox"))) {
							JOptionPane.showMessageDialog(
									fc,
									"This directory doesn't contain any MBox files.",
									"No Mbox Files",
									JOptionPane.WARNING_MESSAGE
							);
						} else {
							mboxDirsListModel.addElement(mboxDir);
						}
					} catch (IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(
								fc,
								"An error occurred while scanning the directory for MBox files:\n" + ex.getMessage(),
								"Error",
								JOptionPane.ERROR_MESSAGE
						);
					}
				}
			}
		});
		JButton removeMboxDirButton = new JButton("Remove Selected");
		removeMboxDirButton.addActionListener(event -> mboxDirsList.getSelectedValuesList().forEach(mboxDirsListModel::removeElement));
		mboxDirsButtonPanel.add(addMboxDirButton);
		mboxDirsButtonPanel.add(removeMboxDirButton);
		mboxDirsButtonPanel.add(new JButton(new DownloadEmailsAction(owner)));
		mboxDirsPanel.add(mboxDirsButtonPanel, BorderLayout.SOUTH);

		return new AbstractMap.SimpleEntry<>(mboxDirsPanel, mboxDirsList);
	}
}

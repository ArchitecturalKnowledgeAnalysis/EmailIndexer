package nl.andrewl.email_indexer.browser;

import nl.andrewl.email_indexer.browser.email.EmailViewPanel;
import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.gen.EmailDatasetGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;

public class EmailDatasetBrowser extends JFrame {
	private final EmailViewPanel emailViewPanel;
	private final SearchPanel searchPanel;
	private EmailDataset currentDataset = null;

	public EmailDatasetBrowser () {
		super("Email Dataset Browser");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setJMenuBar(buildMenu());

		this.emailViewPanel = new EmailViewPanel();
		this.searchPanel = new SearchPanel(emailViewPanel);
		this.setContentPane(buildUi());
		this.setPreferredSize(new Dimension(1000, 600));
		this.pack();
		this.setLocationRelativeTo(null);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (currentDataset != null) {
					try {
						currentDataset.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});
	}

	private void setDataset(EmailDataset ds) {
		if (currentDataset != null) {
			try {
				currentDataset.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.currentDataset = ds;
		searchPanel.setDataset(ds);
		emailViewPanel.setDataset(ds);
	}

	private JMenuBar buildMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem openItem = new JMenuItem("Open");
		openItem.addActionListener(e -> {
			JFileChooser fc = new JFileChooser(Path.of(".").toFile());
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int result = fc.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				File f = fc.getSelectedFile();
				try {
					EmailDataset.open(f.toPath())
							.exceptionally(throwable -> {
								throwable.printStackTrace();
								JOptionPane.showMessageDialog(this, "Could not open dataset: " + throwable.getMessage(), "Could not open dataset", JOptionPane.WARNING_MESSAGE);
								return null;
							})
							.thenAccept(this::setDataset);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		fileMenu.add(openItem);

		JMenuItem generateItem = new JMenuItem("Generate Dataset");
		generateItem.addActionListener(e -> {
			JFileChooser fc = new JFileChooser(Path.of(".").toFile());
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fc.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				Path mboxDir = fc.getSelectedFile().toPath();
				fc.setCurrentDirectory(Path.of(".").toFile());
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				result = fc.showSaveDialog(this);
				if (result == JFileChooser.APPROVE_OPTION) {
					Path dsDir = fc.getSelectedFile().toPath();
					try {
						new EmailDatasetGenerator().generate(mboxDir, dsDir)
								.thenRun(() -> JOptionPane.showMessageDialog(this, "Dataset generated!"));
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});
		fileMenu.add(generateItem);

		menuBar.add(fileMenu);

		return menuBar;
	}

	private Container buildUi() {
		JPanel container = new JPanel(new BorderLayout());
		container.add(this.emailViewPanel, BorderLayout.CENTER);
		container.add(this.searchPanel, BorderLayout.WEST);
		return container;
	}
}

package nl.andrewl.email_indexer.browser;

import nl.andrewl.email_indexer.browser.control.DatasetOpenAction;
import nl.andrewl.email_indexer.browser.control.ExportDatasetAction;
import nl.andrewl.email_indexer.browser.control.GenerateDatasetAction;
import nl.andrewl.email_indexer.browser.email.EmailViewPanel;
import nl.andrewl.email_indexer.data.EmailDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
		this.setDataset(null);

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

	public EmailDataset getCurrentDataset() {
		return this.currentDataset;
	}

	public void setDataset(EmailDataset ds) {
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
		fileMenu.add(new JMenuItem(new DatasetOpenAction(this)));
		fileMenu.add(new JMenuItem(new GenerateDatasetAction(this)));
		fileMenu.add(new JMenuItem(new ExportDatasetAction(this)));

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

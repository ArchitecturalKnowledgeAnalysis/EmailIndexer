package nl.andrewl.email_indexer;

import com.formdev.flatlaf.FlatDarkLaf;
import nl.andrewl.email_indexer.browser.EmailDatasetBrowser;

public class EmailIndexerProgram {
	public static void main(String[] args) throws Exception {
		FlatDarkLaf.setup();
		var browser = new EmailDatasetBrowser();
		browser.setVisible(true);
	}
}

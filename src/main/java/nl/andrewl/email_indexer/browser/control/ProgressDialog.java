package nl.andrewl.email_indexer.browser.control;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * A dialog that can be used to show the progress of an ongoing task by
 * periodically posting messages to the dialog's log text component.
 */
public class ProgressDialog extends JDialog implements Consumer<String> {
	private final JTextPane textPane = new JTextPane();
	private final JButton doneButton = new JButton("Done");

	public ProgressDialog(Window owner, String title, String description) {
		super(owner, title, ModalityType.APPLICATION_MODAL);

		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel(description), BorderLayout.NORTH);
		textPane.setPreferredSize(new Dimension(500, 400));
		textPane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		p.add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		doneButton.setEnabled(false);
		doneButton.addActionListener(e -> dispose());
		buttonPanel.add(doneButton);
		p.add(buttonPanel, BorderLayout.SOUTH);

		setContentPane(p);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		pack();
		setLocationRelativeTo(owner);
	}

	/**
	 * Begins showing the dialog. Use this instead of calling setVisible(true).
	 */
	public void activate() {
		new Thread(() -> setVisible(true)).start();
	}

	public synchronized void append(String msg) {
		SwingUtilities.invokeLater(() -> {
			textPane.setText(textPane.getText() + "\n" + msg);
			textPane.setCaretPosition(textPane.getText().length());
		});
	}

	public void done() {
		doneButton.setEnabled(true);
	}

	@Override
	public void accept(String s) {
		append(s);
	}
}

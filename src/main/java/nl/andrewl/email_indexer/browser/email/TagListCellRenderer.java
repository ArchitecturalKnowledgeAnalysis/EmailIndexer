package nl.andrewl.email_indexer.browser.email;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * Renderer for the list of tags that belong to an email. Renders each tag with
 * a random color determined by the hashcode of its name.
 */
public class TagListCellRenderer implements ListCellRenderer<String> {
	private final Random random = new Random();
	private final JLabel label = new JLabel();

	public TagListCellRenderer() {
		this.label.setOpaque(true);
		this.label.setFont(this.label.getFont().deriveFont(Font.BOLD).deriveFont(16.0f));
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
		// Generate color based on hash of name.
		random.setSeed(value.hashCode());
		float hue = random.nextFloat();
		float saturation = random.nextFloat() / 4f + 0.75f;
		float luminance = 0.9f;
		Color foregroundColor = Color.getHSBColor(hue, saturation, luminance);

		label.setText(value);
		label.setForeground(foregroundColor);
		if (isSelected) {
			label.setBackground(list.getSelectionBackground());
		} else {
			label.setBackground(null);
		}
		label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		return label;
	}
}

package nl.andrewl.email_indexer.data;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represents a tag that can be applied to emails, in order to categorize them.
 * @param id The tag's internal id.
 * @param name The name of the tag. This is required, and must be unique.
 * @param description A description for the tag. This may be null.
 */
public record Tag(
		int id,
		String name,
		String description
) implements Comparable<Tag> {
	public Tag(ResultSet rs) throws SQLException {
		this(
				rs.getInt(1),
				rs.getString(2),
				rs.getString(3)
		);
	}

	@Override
	public int compareTo(Tag o) {
		return name.compareToIgnoreCase(o.name);
	}

	@Override
	public String toString() {
		return name;
	}
}

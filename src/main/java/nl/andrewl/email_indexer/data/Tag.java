package nl.andrewl.email_indexer.data;

import java.sql.ResultSet;
import java.sql.SQLException;

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
}

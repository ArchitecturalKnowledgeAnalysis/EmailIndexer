package nl.andrewl.email_indexer.data;

import nl.andrewl.email_indexer.util.DbUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

public record TagGroup(
		int id,
		Integer parentId,
		String name,
		String description,
		int seq
) {
	public static TagGroup fromResultSet(ResultSet rs) throws SQLException {
		return new TagGroup(
				rs.getInt(1),
				DbUtils.getNullableInt(rs, 2),
				rs.getString(3),
				rs.getString(4),
				rs.getInt(5)
		);
	}
}

package nl.andrewl.email_indexer.data;

import java.sql.Connection;
import java.sql.SQLException;

public class DbUtils {
	public static long count(Connection c, String query) {
		try (var stmt = c.prepareStatement(query)) {
			var rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
}

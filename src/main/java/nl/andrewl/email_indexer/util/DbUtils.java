package nl.andrewl.email_indexer.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DbUtils {
	private DbUtils() {}

	/**
	 * Performs a count query.
	 * @param c The connection to use.
	 * @param query The query to use for counting. It should select a single
	 *              integer/long value as the first item in the result set.
	 * @param args The arguments to the query.
	 * @return The count that was obtained.
	 */
	public static long count(Connection c, String query, Object... args) {
		try (var stmt = c.prepareStatement(query)) {
			int idx = 1;
			for (var arg : args) stmt.setObject(idx++, arg);
			var rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static int update(Connection c, String query, Object... args) {
		try (var stmt = c.prepareStatement(query)) {
			int idx = 1;
			for (var arg : args) stmt.setObject(idx++, arg);
			return stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public static long insertWithId(Connection c, String query, Object... args) {
		try (var stmt = c.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
			int idx = 1;
			for (var arg : args) stmt.setObject(idx++, arg);
			int count = stmt.executeUpdate();
			if (count != 1) throw new SQLException("Only one row should be inserted.");
			var rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getLong(1);
			} else {
				throw new SQLException("No keys were returned.");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	@FunctionalInterface
	public interface ResultSetMapper<T> {
		T map(ResultSet rs) throws SQLException;
	}

	@FunctionalInterface
	public interface Transaction {
		void doTransaction(Connection c) throws SQLException;
	}

	/**
	 * Fetches a list of results from a database.
	 * @param c The connection to use.
	 * @param query The query to use.
	 * @param mapper A mapping function to apply to each row.
	 * @param args Arguments to supply to the query.
	 * @return A list of items.
	 * @param <T> The type of items.
	 */
	public static <T> List<T> fetch(Connection c, String query, ResultSetMapper<T> mapper, Object... args) {
		List<T> items = new ArrayList<>();
		try (var stmt = c.prepareStatement(query)) {
			int idx = 1;
			for (var arg : args) stmt.setObject(idx++, arg);
			var rs = stmt.executeQuery();
			while (rs.next()) {
				var item = mapper.map(rs);
				items.add(item);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return items;
	}

	/**
	 * Fetches a single result from a database.
	 * @param c The connection to use.
	 * @param query The query to use.
	 * @param mapper A mapping function to apply to each row.
	 * @param args Arguments to supply to the query.
	 * @return An optional that contains the item, if it was found.
	 * @param <T> The type of the result.
	 */
	public static <T>Optional<T> fetchOne(Connection c, String query, ResultSetMapper<T> mapper, Object... args) {
		try (var stmt = c.prepareStatement(query)) {
			int idx = 1;
			for (var arg : args) stmt.setObject(idx++, arg);
			var rs = stmt.executeQuery();
			if (rs.next()) return Optional.of(mapper.map(rs));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	public static void doTransaction(Connection c, Transaction tx) {
		try {
			c.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		try {
			tx.doTransaction(c);
			c.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				c.rollback();
			} catch (SQLException e1) {
				throw new IllegalStateException(e1);
			}
		} finally {
			try {
				c.setAutoCommit(true);
			} catch (SQLException e1) {
				throw new IllegalStateException(e1);
			}
		}
	}
}

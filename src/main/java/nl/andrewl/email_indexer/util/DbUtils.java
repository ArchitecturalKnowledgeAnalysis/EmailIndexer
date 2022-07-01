package nl.andrewl.email_indexer.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
			try (var rs = stmt.getGeneratedKeys()) {
				if (rs.next()) {
					return rs.getLong(1);
				} else {
					throw new SQLException("No keys were returned.");
				}
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
			try {
				tx.doTransaction(c);
				c.commit();
			} catch (SQLException e) {
				e.printStackTrace();
				c.rollback();
			}
			c.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Helper method that gets a nullable long integer value from a result set.
	 * @param rs The result set.
	 * @param columnIndex The column index to get the value from.
	 * @return The long value, or null.
	 * @throws SQLException If an error occurs.
	 */
	public static Long getNullableLong(ResultSet rs, int columnIndex) throws SQLException {
		long value = rs.getLong(columnIndex);
		return rs.wasNull() ? null : value;
	}

	/**
	 * Helper method that gets a {@link ZonedDateTime} from a long integer
	 * value representing the seconds since the unix epoch.
	 * @param rs The result set.
	 * @param columnIndex The column index to get the value from.
	 * @return The date.
	 * @throws SQLException If an error occurs.
	 */
	public static ZonedDateTime fromEpochSeconds(ResultSet rs, int columnIndex) throws SQLException {
		long seconds = rs.getLong(columnIndex);
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneOffset.UTC);
	}
}

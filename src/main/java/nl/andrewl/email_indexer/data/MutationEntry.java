package nl.andrewl.email_indexer.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

/**
 * Represents the information stored in a mutation record, that describes a
 * change to the set of emails available in the dataset.
 * @param id
 * @param description
 * @param performedAt
 * @param affectedEmailCount
 */
public record MutationEntry(
        long id,
        String description,
        ZonedDateTime performedAt,
        long affectedEmailCount
) {
    public MutationEntry(ResultSet rs) throws SQLException {
        this(
                rs.getLong(1),
                rs.getString(2),
                rs.getObject(3, ZonedDateTime.class),
                rs.getLong(4)
        );
    }
}

package nl.andrewl.email_indexer.data;

import nl.andrewl.email_indexer.util.DbUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static nl.andrewl.email_indexer.util.DbUtils.count;
import static nl.andrewl.email_indexer.util.DbUtils.update;

/**
 * Repository for interacting with tags.
 */
public class TagRepository {
	private final Connection conn;

	public TagRepository(Connection conn) {
		this.conn = conn;
	}

	public TagRepository(EmailDataset ds) {
		this.conn = ds.getConnection();
	}

	public int countTags() {
		return (int) count(conn, "SELECT COUNT(ID) FROM TAG");
	}

	public Tag createTag(String name, String description) {
		int id = (int) DbUtils.insertWithId(conn, "INSERT INTO TAG (NAME, DESCRIPTION) VALUES (?, ?)", name, description);
		return new Tag(id, name, description);
	}

	public void deleteTag(long id) {
		update(conn, "DELETE FROM TAG WHERE ID = ?", id);
	}

	public boolean tagExists(String name) {
		return count(conn, "SELECT COUNT(ID) FROM TAG WHERE NAME = ?", name) > 0;
	}

	public void setDescription(long tagId, String newDescription) {
		update(conn, "UPDATE TAG SET DESCRIPTION = ? WHERE ID = ?", newDescription, tagId);
	}

	public void setName(long tagId, String newName) {
		update(conn, "UPDATE TAG SET NAME = ? WHERE ID = ?", newName, tagId);
	}

	/**
	 * Determines if an email has a certain tag.
	 * @param emailId The id of the email.
	 * @param tagId The tag to check.
	 * @return True if the email has the tag, or false otherwise.
	 */
	public boolean hasTag(long emailId, int tagId) {
		return count(conn, "SELECT COUNT(TAG_ID) FROM EMAIL_TAG WHERE EMAIL_ID = ? AND TAG_ID = ?", emailId, tagId) > 0;
	}

	/**
	 * Adds a tag to an email. Does nothing if the email already has the tag.
	 * @param emailId The id of the email.
	 * @param tagId The tag to add.
	 */
	public void addTag(long emailId, int tagId) {
		if (!hasTag(emailId, tagId)) {
			update(conn, "INSERT INTO EMAIL_TAG (EMAIL_ID, TAG_ID) VALUES (?, ?)", emailId, tagId);
		}
	}

	/**
	 * Removes a tag from an email. Does nothing if the email doesn't have the
	 * tag.
	 * @param emailId The id of the email.
	 * @param tagId The tag to remove.
	 */
	public void removeTag(long emailId, int tagId) {
		update(conn, "DELETE FROM EMAIL_TAG WHERE EMAIL_ID = ? AND TAG_ID = ?", emailId, tagId);
	}

	/**
	 * Gets the list of all tags belonging to any parents of the given email.
	 * @param emailId The id of the email.
	 * @return A list of tags.
	 */
	public List<Tag> getAllParentTags(long emailId) {
		Set<Tag> tags = new HashSet<>();
		try (
				var parentStmt = conn.prepareStatement("SELECT PARENT_ID FROM EMAIL WHERE ID = ?");
				var tagStmt = conn.prepareStatement(QueryCache.load("/sql/tag/fetch_tags_by_email_id.sql"))
		) {
			Long nextId = emailId;
			while (nextId != null) {
				parentStmt.setLong(1, nextId);
				var rs = parentStmt.executeQuery();
				if (!rs.next()) break;
				Long parentId = rs.getLong(1);
				if (rs.wasNull()) parentId = null;
				nextId = parentId;

				if (parentId != null) {
					tagStmt.setLong(1, parentId);
					var tagRs = tagStmt.executeQuery();
					while (tagRs.next()) tags.add(new Tag(tagRs));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		List<Tag> tagList = new ArrayList<>(tags);
		Collections.sort(tagList);
		return tagList;
	}

	/**
	 * Gets the list of all tags in any emails that are children of the given
	 * email, by using a breadth-first search of all children.
	 * @param emailId The email to check.
	 * @return The list of all tags.
	 */
	public List<Tag> getAllChildTags(long emailId) {
		Set<Tag> tags = new HashSet<>();
		Queue<Long> messageIdQueue = new LinkedList<>();
		messageIdQueue.add(emailId);
		try (
				var childStmt = conn.prepareStatement("SELECT ID FROM EMAIL WHERE PARENT_ID = ?");
				var tagStmt = conn.prepareStatement(QueryCache.load("/sql/tag/fetch_tags_by_email_id.sql"))
		) {
			while (!messageIdQueue.isEmpty()) {
				childStmt.setLong(1, messageIdQueue.remove());
				var rs = childStmt.executeQuery();
				while (rs.next()) {
					long childId = rs.getLong(1);
					tagStmt.setLong(1, childId);
					var tagRs = tagStmt.executeQuery();
					while (tagRs.next()) {
						tags.add(new Tag(tagRs));
					}
					messageIdQueue.add(childId);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		List<Tag> tagList = new ArrayList<>(tags);
		Collections.sort(tagList);
		return tagList;
	}
}

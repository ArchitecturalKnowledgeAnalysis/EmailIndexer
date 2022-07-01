package nl.andrewl.email_indexer.data;

import nl.andrewl.email_indexer.util.DbUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static nl.andrewl.email_indexer.util.DbUtils.*;

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

	/**
	 * Gets a tag by its id.
	 * @param id The tag's id.
	 * @return An optional that contains the tag, if it exists.
	 */
	public Optional<Tag> getTagById(int id) {
		return DbUtils.fetchOne(conn, QueryCache.load("/sql/tag/fetch_tag_by_id.sql"), Tag::new, id);
	}

	/**
	 * Gets a tag by its name.
	 * @param name The tag's name.
	 * @return An optional that contains the tag, if it exists.
	 */
	public Optional<Tag> getTagByName(String name) {
		return DbUtils.fetchOne(conn, QueryCache.load("/sql/tag/fetch_tag_by_name.sql"), Tag::new, name);
	}

	/**
	 * Finds all tags in the dataset.
	 * @return The list of tags, ordered by their name.
	 */
	public List<Tag> findAll() {
		return DbUtils.fetch(conn, QueryCache.load("/sql/tag/fetch_all_tags.sql"), Tag::new);
	}

	/**
	 * Counts the number of tags in the database.
	 * @return The number of tags.
	 */
	public int countTags() {
		return (int) count(conn, "SELECT COUNT(ID) FROM TAG");
	}

	/**
	 * Counts the number of emails that have a given tag.
	 * @param tagId The id of the tag.
	 * @return The number of emails with the tag.
	 */
	public long countTaggedEmails(int tagId) {
		return DbUtils.count(conn, "SELECT COUNT(EMAIL_ID) FROM EMAIL_TAG WHERE TAG_ID = ?", tagId);
	}

	/**
	 * Gets a sorted list of tags for an email id.
	 * @param emailId The id of the email.
	 * @return A list of tags for the given email.
	 */
	public List<Tag> getTags(long emailId) {
		return DbUtils.fetch(conn, QueryCache.load("/sql/tag/fetch_tags_by_email_id.sql"), Tag::new, emailId);
	}

	/**
	 * Creates a new tag.
	 * @param name The name of the tag. This should be unique, and not null.
	 * @param description The description for the tag.
	 * @return The tag that was created.
	 */
	public Tag createTag(String name, String description) {
		int id = (int) DbUtils.insertWithId(conn, "INSERT INTO TAG (NAME, DESCRIPTION) VALUES (?, ?)", name, description);
		rectifyTagOrdering();
		return getTagById(id).orElseThrow();
	}

	/**
	 * Deletes a tag from the database. Note that this also permanently removes
	 * any trace of this tag being applied to any emails.
	 * @param id The id of the tag to delete.
	 */
	public void deleteTag(int id) {
		update(conn, "DELETE FROM TAG WHERE ID = ?", id);
		rectifyTagOrdering();
	}

	/**
	 * Determines whether the given tag exists.
	 * @param name The name of the tag.
	 * @return True if it exists, or false if not.
	 */
	public boolean tagExists(String name) {
		return count(conn, "SELECT COUNT(ID) FROM TAG WHERE NAME = ?", name) > 0;
	}

	/**
	 * Updates a tag's description.
	 * @param tagId The id of the tag.
	 * @param newDescription The new description.
	 */
	public void setDescription(int tagId, String newDescription) {
		update(conn, "UPDATE TAG SET DESCRIPTION = ? WHERE ID = ?", newDescription, tagId);
	}

	/**
	 * Updates a tag's name.
	 * @param tagId The id of the tag.
	 * @param newName The new name. This should not be null, and it should be
	 *                unique. No other tag should exist. {@link TagRepository#tagExists(String)}
	 */
	public void setName(int tagId, String newName) {
		update(conn, "UPDATE TAG SET NAME = ? WHERE ID = ?", newName, tagId);
	}

	/**
	 * Updates a tag's sequence. All other tags' sequence values will be
	 * reordered to fit this tag's sequence.
	 * @param tagId The id of the tag.
	 * @param newSeq The new sequence value. 1 indicates the tag is first, and
	 * increase by 1 for each tag.
	 */
	public void setSeq(int tagId, int newSeq) {
		DbUtils.doTransaction(conn, c -> {
			List<Integer> tagIds = DbUtils.fetch(c, "SELECT ID FROM TAG ORDER BY SEQ ASC", rs -> rs.getInt(1));
			tagIds.remove(tagId);
			tagIds.add(Math.min(tagIds.size(), Math.max(0, newSeq - 1)), tagId);
			// Rectify all tags with the new sequence values.
			int n = 1;
			try (var stmt = conn.prepareStatement("UPDATE TAG SET SEQ = ? WHERE ID = ?")) {
				for (var id : tagIds) {
					stmt.setInt(1, n++);
					stmt.setInt(2, id);
					stmt.executeUpdate();
				}
			}
		});
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
	 * Determines if an email has a certain tag.
	 * @param emailId The id of the email.
	 * @param tagName The tag to check.
	 * @return True if the email has the tag, or false otherwise.
	 */
	public boolean hasTag(long emailId, String tagName) {
		return count(
				conn,
				"SELECT COUNT(EMAIL_ID) FROM EMAIL_TAG WHERE EMAIL_ID = ? AND TAG_ID = (SELECT ID FROM TAG WHERE NAME = ?)",
				emailId,
				tagName
		) > 0;
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
	 * Adds a tag to an email. If the tag doesn't exist yet, it will be added
	 * automatically with an empty description.
	 * @param emailId The id of the email.
	 * @param tagName The name of the tag.
	 */
	public void addTag(long emailId, String tagName) {
		if (hasTag(emailId, tagName)) return;
		DbUtils.doTransaction(conn, c -> {
			var tag = this.getTagByName(tagName).orElse(createTag(tagName, null));
			addTag(emailId, tag.id());
		});
	}

	/**
	 * Adds a tag to an email, and recursively to all replies of that email.
	 * @param emailId The id of the first email to add the tag to.
	 * @param tagId The tag to add.
	 */
	public void addTagRecursive(long emailId, int tagId) throws SQLException {
		try {
			conn.setAutoCommit(false);
			var repo = new EmailRepository(conn);
			Queue<Long> emailIdQueue = new LinkedList<>();
			emailIdQueue.add(emailId);
			while (!emailIdQueue.isEmpty()) {
				long nextId = emailIdQueue.remove();
				addTag(nextId, tagId);
				for (var reply : repo.findAllReplies(nextId)) {
					emailIdQueue.add(reply.id());
				}
			}
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			conn.rollback();
		} finally {
			conn.setAutoCommit(true);
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
	 * Removes a tag from an email, and recursively from all replies of that email.
	 * @param emailId The id of the first email to remove the tag from.
	 * @param tagId The tag to remove.
	 */
	public void removeTagRecursive(long emailId, int tagId) throws SQLException {
		try {
			conn.setAutoCommit(false);
			var repo = new EmailRepository(conn);
			Queue<Long> emailIdQueue = new LinkedList<>();
			emailIdQueue.add(emailId);
			while (!emailIdQueue.isEmpty()) {
				long nextId = emailIdQueue.remove();
				removeTag(nextId, tagId);
				for (var reply : repo.findAllReplies(nextId)) {
					emailIdQueue.add(reply.id());
				}
			}
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			conn.rollback();
		} finally {
			conn.setAutoCommit(true);
		}
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

	public List<TagGroup> getGroups(int tagId) {
		return fetch(
				conn,
				"SELECT * FROM TAG_GROUP WHERE ID IN (SELECT GROUP_ID FROM TAG_GROUP_TAGS WHERE TAG_ID = ?)",
				TagGroup::fromResultSet,
				tagId
		);
	}

	public boolean isInGroup(int tagId, int groupId) {
		return count(conn, "SELECT COUNT(TAG_ID) FROM TAG_GROUP_TAGS WHERE GROUP_ID = ? AND TAG_ID = ?", groupId, tagId) > 0;
	}

	public void addGroup(int tagId, int groupId) {
		if (!isInGroup(tagId, groupId)) {
			update(conn, "INSERT INTO TAG_GROUP_TAGS (GROUP_ID, TAG_ID) VALUES (?, ?)", groupId, tagId);
		}
	}

	public void removeGroup(int tagId, int groupId) {
		update(conn, "DELETE FROM TAG_GROUP_TAGS WHERE GROUP_ID = ? AND TAG_ID = ?", groupId, tagId);
	}

	private void rectifyTagOrdering() {
		doTransaction(conn, c -> {
			List<Integer> tagIds = DbUtils.fetch(conn, "SELECT ID FROM TAG ORDER BY SEQ ASC", rs -> rs.getInt(1));
			int n = 1;
			try (var stmt = conn.prepareStatement("UPDATE TAG SET SEQ = ? WHERE ID = ?")) {
				for (var tagId : tagIds) {
					stmt.setInt(1, n++);
					stmt.setInt(2, tagId);
					stmt.executeUpdate();
				}
			}
		});
	}
}

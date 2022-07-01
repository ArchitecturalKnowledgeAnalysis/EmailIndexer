package nl.andrewl.email_indexer.data;

import nl.andrewl.email_indexer.util.DbUtils;

import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static nl.andrewl.email_indexer.util.DbUtils.*;

public class TagGroupRepository {
	private final Connection conn;

	public TagGroupRepository(Connection conn) {
		this.conn = conn;
	}

	public TagGroupRepository(EmailDataset ds) {
		this(ds.getConnection());
	}

	public List<TagGroup> findAll() {
		return fetch(conn, "SELECT * FROM TAG_GROUP ORDER BY SEQ ASC", TagGroup::fromResultSet);
	}

	public List<TagGroup> getChildren(int id) {
		return fetch(
				conn,
				"SELECT * FROM TAG_GROUP WHERE PARENT_ID = ? ORDER BY SEQ ASC",
				TagGroup::fromResultSet,
				id
		);
	}

	public int countGroups() {
		return (int) count(conn, "SELECT COUNT(ID) FROM TAG_GROUP");
	}

	public Optional<TagGroup> getGroupById(int id) {
		return fetchOne(conn, "SELECT * FROM TAG_GROUP WHERE ID = ?", TagGroup::fromResultSet, id);
	}

	public Optional<TagGroup> getGroupByName(String name) {
		return fetchOne(conn, "SELECT * FROM TAG_GROUP WHERE NAME = ?", TagGroup::fromResultSet, name);
	}

	public boolean groupExists(String name) {
		return count(conn, "SELECT COUNT(ID) FROM TAG_GROUP WHERE NAME = ?", name) > 0;
	}

	public List<Tag> getTagsInGroup(int groupId) {
		return DbUtils.fetch(
				conn,
				"SELECT * FROM TAG WHERE ID IN (SELECT TAG_ID FROM TAG_GROUP_TAGS WHERE GROUP_ID = ?) ORDER BY SEQ ASC",
				Tag::new,
				groupId
		);
	}

	public List<Tag> getTagsInGroupRecursive(int groupId) {
		List<Tag> tags = getTagsInGroup(groupId);
		for (var child : getChildren(groupId)) {
			List<Tag> childTags = getTagsInGroupRecursive(child.id());
			for (var tag : childTags) {
				if (!tags.contains(tag)) tags.add(tag);
			}
		}
		tags.sort(Comparator.comparing(Tag::seq));
		return tags;
	}

	public TagGroup createGroup(String name, String description) {
		int id = (int) DbUtils.insertWithId(conn, "INSERT INTO TAG_GROUP(NAME, DESCRIPTION) VALUES (?, ?)", name, description);
		rectifyGroupOrdering();
		return getGroupById(id).orElseThrow();
	}

	public void deleteGroup(int id) {
		update(conn, "DELETE FROM TAG_GROUP WHERE ID = ?", id);
		rectifyGroupOrdering();
	}

	public void setName(int id, String newName) {
		update(conn, "UPDATE TAG_GROUP SET NAME = ? WHERE ID = ?", newName, id);
	}

	public void setDescription(int id, String newDescription) {
		update(conn, "UPDATE TAG_GROUP SET DESCRIPTION = ? WHERE ID = ?", newDescription, id);
	}

	public void setParent(int groupId, Integer parentId) {
		update(conn, "UPDATE TAG_GROUP SET PARENT_ID = ? WHERE ID = ?", parentId, groupId);
	}

	public void setSeq(int groupId, int newSeq) {
		doTransaction(conn, c -> {
			List<Integer> groupIds = DbUtils.fetch(c, "SELECT ID FROM TAG_GROUP ORDER BY SEQ ASC", rs -> rs.getInt(1));
			groupIds.remove(groupId);
			groupIds.add(Math.min(groupIds.size(), Math.max(0, newSeq - 1)), groupId);
			// Rectify all groups with the new sequence values.
			int n = 1;
			try (var stmt = conn.prepareStatement("UPDATE TAG_GROUP SET SEQ = ? WHERE ID = ?")) {
				for (var id : groupIds) {
					stmt.setInt(1, n++);
					stmt.setInt(2, id);
					stmt.executeUpdate();
				}
			}
		});
	}

	public boolean hasTag(int groupId, int tagId) {
		return count(conn, "SELECT COUNT(TAG_ID) FROM TAG_GROUP_TAGS WHERE GROUP_ID = ? AND TAG_ID = ?", groupId, tagId) > 0;
	}

	public void addTag(int groupId, int tagId) {
		if (!hasTag(groupId, tagId)) {
			update(conn, "INSERT INTO TAG_GROUP_TAGS (GROUP_ID, TAG_ID) VALUES (?, ?)", groupId, tagId);
		}
	}

	public void removeTag(int groupId, int tagId) {
		update(conn, "DELETE FROM TAG_GROUP_TAGS WHERE GROUP_ID = ? AND TAG_ID = ?", groupId, tagId);
	}

	private void rectifyGroupOrdering() {
		doTransaction(conn, c -> {
			List<Integer> groupIds = DbUtils.fetch(conn, "SELECT ID FROM TAG_GROUP ORDER BY SEQ ASC", rs -> rs.getInt(1));
			int n = 1;
			try (var stmt = conn.prepareStatement("UPDATE TAG_GROUP SET SEQ = ? WHERE ID = ?")) {
				for (var groupId : groupIds) {
					stmt.setInt(1, n++);
					stmt.setInt(2, groupId);
					stmt.executeUpdate();
				}
			}
		});
	}
}

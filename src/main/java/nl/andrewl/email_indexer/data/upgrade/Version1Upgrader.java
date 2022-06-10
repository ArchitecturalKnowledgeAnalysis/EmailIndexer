package nl.andrewl.email_indexer.data.upgrade;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.gen.DatabaseGenerator;
import nl.andrewl.email_indexer.gen.EmailIndexGenerator;
import nl.andrewl.email_indexer.util.Status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Properties;

/**
 * Upgrades datasets from version 1 to the latest version.
 */
public class Version1Upgrader {
	public void upgrade(Path originalDatasetPath, Path newDatasetDir, Status status) throws Exception {
		status.sendMessage("Opening the old dataset from " + originalDatasetPath);
		EmailDataset ds1 = EmailDataset.open(originalDatasetPath).join();

		if (!Files.exists(newDatasetDir)) {
			Files.createDirectories(newDatasetDir);
		}

		upgradeDatabase(newDatasetDir, ds1, status);
		status.sendMessage("Generating indexes for the upgraded dataset.");
		EmailDataset ds2 = new EmailDataset(newDatasetDir);
		new EmailIndexGenerator().generateIndex(ds2);
		// Generate metadata
		status.sendMessage("Generating metadata for the upgraded dataset.");
		Properties props = new Properties();
		props.setProperty("version", "2");
		props.store(Files.newBufferedWriter(ds2.getMetadataFile()), null);
		status.sendMessage("Done.");
	}

	private void upgradeDatabase(Path newDatasetDir, EmailDataset ds1, Status status) throws Exception {
		try (var dbGen = new DatabaseGenerator(newDatasetDir.resolve("database"))) {
			status.sendMessage("Copying emails from old dataset to new one.");
			try (
				var stmt = ds1.getConnection().prepareStatement("SELECT * FROM EMAIL ORDER BY DATE")
			) {
				var rs = stmt.executeQuery();
				while (rs.next()) {
					String msgId = rs.getString("MESSAGE_ID");
					dbGen.addEmail(
						msgId,
						rs.getString("SUBJECT"),
						rs.getString("IN_REPLY_TO"),
						rs.getString("SENT_FROM"),
						rs.getObject("DATE", ZonedDateTime.class),
						rs.getString("BODY")
					);
				}
			}
			dbGen.postProcess(status);
			status.sendMessage("Copying tags from old dataset to new one.");
			TagRepository tagRepo = new TagRepository(dbGen.getConn());
			EmailRepository emailRepo = new EmailRepository(dbGen.getConn());
			try (
				var stmt = ds1.getConnection().prepareStatement("SELECT * FROM EMAIL_TAG")
			) {
				var rs = stmt.executeQuery();
				while (rs.next()) {
					String msgId = rs.getString("MESSAGE_ID");
					String tagName = rs.getString("TAG");
					emailRepo.findId(msgId).ifPresent(emailId -> {
						tagRepo.addTag(emailId, tagName);
					});
				}
			}
			status.sendMessage("Copying mutation info from old dataset to new one.");
			try (
				var stmt = ds1.getConnection().prepareStatement("SELECT * FROM MUTATION");
				var insertStmt = dbGen.getConn().prepareStatement("INSERT INTO MUTATION (ID, DESCRIPTION, PERFORMED_AT, AFFECTED_EMAIL_COUNT) VALUES (?, ?, ?, ?)")
			) {
				var rs = stmt.executeQuery();
				while (rs.next()) {
					insertStmt.setLong(1, rs.getLong(1));
					insertStmt.setString(2, rs.getString(2));
					insertStmt.setObject(3, rs.getObject(3, ZonedDateTime.class));
					insertStmt.setLong(4, rs.getLong(4));
					insertStmt.executeUpdate();
				}
			}
		}
	}
}

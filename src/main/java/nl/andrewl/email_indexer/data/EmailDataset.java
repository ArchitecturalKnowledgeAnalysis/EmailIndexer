package nl.andrewl.email_indexer.data;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import nl.andrewl.email_indexer.EmailIndexSearcher;
import org.apache.lucene.queryparser.classic.ParseException;
import org.h2.store.fs.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * An email dataset is a complete set of emails that have been parsed from one
 * or more mbox files, and an Apache Lucene index directory. It is stored on the
 * disk as a ZIP file.
 */
public class EmailDataset implements AutoCloseable {
	private final Path openDir;
	private final Path dsFile;
	private final Connection dbConn;

	/**
	 * Opens a dataset from the given file.
	 * @param dsFile The file to open a dataset from.
	 * @throws IOException If the dataset could not be opened.
	 */
	public EmailDataset(Path dsFile) throws IOException {
		this.dsFile = dsFile;
		this.openDir = Files.createTempDirectory("email-dataset");
		var zip = new ZipFile(dsFile.toFile());
		zip.extractAll(openDir.toAbsolutePath().toString());
		zip.close();
		try {
			this.dbConn = DriverManager.getConnection(getJdbcUrl(getDatabaseFile()));
		} catch (SQLException e) {
			throw new IOException("Could not open database.", e);
		}
	}

	public Connection getConnection() {
		return this.dbConn;
	}

	public Path getIndexDir() {
		return this.openDir.resolve("index");
	}

	public Path getDatabaseFile() {
		return this.openDir.resolve("database.mv.db");
	}

	public Optional<EmailEntry> findEmailById(String messageId) {
		try (var stmt = this.dbConn.prepareStatement("""
			SELECT SUBJECT, IN_REPLY_TO, SENT_FROM, DATE, BODY, LISTAGG(TAG, ',')
			FROM EMAIL
			LEFT JOIN EMAIL_TAG ON EMAIL.MESSAGE_ID = EMAIL_TAG.MESSAGE_ID
			WHERE EMAIL.MESSAGE_ID = ?""")) {
			stmt.setString(1, messageId);
			var rs = stmt.executeQuery();
			if (!rs.next()) return Optional.empty();
			var entry = new EmailEntry(
					messageId,
					rs.getString(1),
					rs.getString(2),
					rs.getString(3),
					rs.getObject(4, ZonedDateTime.class),
					rs.getString(5),
					new HashSet<>()
			);
			String tags = rs.getString(6);
			if (tags != null && !tags.isEmpty()) {
				for (var tag : tags.split(",")) {
					entry.tags().add(tag);
				}
			}
			return Optional.of(entry);
		} catch (SQLException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	public List<EmailEntryPreview> findAllReplies(String messageId) {
		List<EmailEntryPreview> entries = new ArrayList<>();
		try (var stmt = this.dbConn.prepareStatement("""
			SELECT EMAIL.MESSAGE_ID, SUBJECT, DATE, LISTAGG(TAG, ',')
			FROM EMAIL
			LEFT JOIN EMAIL_TAG ON EMAIL.MESSAGE_ID = EMAIL_TAG.MESSAGE_ID
			WHERE EMAIL.IN_REPLY_TO = ?""")) {
			stmt.setString(1, messageId);
			var rs = stmt.executeQuery();
			while (rs.next()) entries.add(new EmailEntryPreview(rs));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return entries;
	}

	public EmailSearchResult search(String query, int page, int size) {
		try {
			var result = new EmailIndexSearcher().search(getIndexDir(), query, page, size);
			List<EmailEntryPreview> entries = new ArrayList<>(result.size());
			for (var messageId : result.resultIds()) {
				try (var stmt = this.dbConn.prepareStatement("""
					SELECT EMAIL.MESSAGE_ID, SUBJECT, DATE, LISTAGG(TAG, ',')
					FROM EMAIL
					LEFT JOIN EMAIL_TAG ON EMAIL.MESSAGE_ID = EMAIL_TAG.MESSAGE_ID
					WHERE EMAIL.MESSAGE_ID = ?""")) {
					stmt.setString(1, messageId);
					var rs = stmt.executeQuery();
					while (rs.next()) entries.add(new EmailEntryPreview(rs));
				}
			}
			return new EmailSearchResult(entries, result.page(), result.size(), result.totalResultsCount());
		} catch (IOException | ParseException | SQLException e) {
			e.printStackTrace();
			return new EmailSearchResult(new ArrayList<>(), 1, size, 0);
		}
	}

	@Override
	public void close() throws Exception {
		this.dbConn.close();
		buildDatasetZip(this.openDir, this.dsFile);
	}

	/**
	 * Zips a dataset directory into a ZIP file for storage. The directory
	 * should contain an "index" directory containing Apache Lucene index files,
	 * and a "database.mv.db" file that holds the relational database.
	 * @param dir The directory to use.
	 * @param file The file to place the ZIP file at.
	 * @throws IOException If an error occurs.
	 */
	public static void buildDatasetZip(Path dir, Path file) throws IOException {
		var zip = new ZipFile(file.toFile());
		ZipParameters params = new ZipParameters();
		params.setCompressionLevel(CompressionLevel.ULTRA);
		params.setOverrideExistingFilesInZip(true);
		zip.addFolder(dir.resolve("index").toFile(), params);
		zip.addFile(dir.resolve("database.mv.db").toFile(), params);
		// Clear away the opened files.
		FileUtils.deleteRecursive(dir.toAbsolutePath().toString(), true);
	}

	public static String getJdbcUrl(Path dbFile) {
		String dbFileName = dbFile.toAbsolutePath().toString();
		if (dbFileName.endsWith(".mv.db")) {
			dbFileName = dbFileName.substring(0, dbFileName.length() - ".mv.db".length());
		}
		return "jdbc:h2:file:" + dbFileName + ";DB_CLOSE_ON_EXIT=FALSE";
	}
}

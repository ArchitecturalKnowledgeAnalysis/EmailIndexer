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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

/**
 * An email dataset is a complete set of emails that have been parsed from one
 * or more mbox files, and an Apache Lucene index directory. It is stored on the
 * disk as a ZIP file.
 */
public class EmailDataset implements AutoCloseable {
	private final Path openDir;
	private final Path dsFile;
	private final Connection dbConn;

	public EmailDataset(Path openDir, Path dsFile, Connection dbConn) {
		this.openDir = openDir;
		this.dsFile = dsFile;
		this.dbConn = dbConn;
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
					new ArrayList<>()
			);
			String tags = rs.getString(6);
			if (tags != null && !tags.isEmpty()) {
				for (var tag : tags.split(",")) {
					entry.tags().add(tag);
				}
				Collections.sort(entry.tags());
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

	public EmailSearchResult findAll(int page, int size) {
		String queryFormat = """
			SELECT EMAIL.MESSAGE_ID, SUBJECT, DATE, LISTAGG(TAG, ',')
			FROM EMAIL
			LEFT JOIN EMAIL_TAG ON EMAIL.MESSAGE_ID = EMAIL_TAG.MESSAGE_ID
			GROUP BY EMAIL.MESSAGE_ID
			ORDER BY EMAIL.DATE DESC
			LIMIT %d OFFSET %d""";
		String query = String.format(queryFormat, size, (page - 1) * size);
		List<EmailEntryPreview> entries = new ArrayList<>(size);
		try (var stmt = this.dbConn.prepareStatement(query)) {
			var rs = stmt.executeQuery();
			while (rs.next()) entries.add(new EmailEntryPreview(rs));
			return new EmailSearchResult(entries, page, size, -1);
		} catch (SQLException e) {
			e.printStackTrace();
			return new EmailSearchResult(new ArrayList<>(), 1, size, 0);
		}
	}

	public boolean hasTag(String messageId, String tag) {
		try (var stmt = dbConn.prepareStatement("SELECT COUNT(TAG) FROM EMAIL_TAG WHERE MESSAGE_ID = ? AND TAG = ?")) {
			stmt.setString(1, messageId);
			stmt.setString(2, tag);
			var rs = stmt.executeQuery();
			if (rs.next()) return rs.getLong(1) > 0;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void addTag(String messageId, String tag) {
		if (!hasTag(messageId, tag)) {
			try (var stmt = dbConn.prepareStatement("INSERT INTO EMAIL_TAG (MESSAGE_ID, TAG) VALUES (?, ?)")) {
				stmt.setString(1, messageId);
				stmt.setString(2, tag);
				stmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void removeTag(String messageId, String tag) {
		try (var stmt = dbConn.prepareStatement("DELETE FROM EMAIL_TAG WHERE MESSAGE_ID = ? AND TAG = ?")) {
			stmt.setString(1, messageId);
			stmt.setString(2, tag);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws Exception {
		this.dbConn.close();
	}

	/**
	 * Opens a dataset from the given file.
	 * @param dsFile The file to open.
	 * @return A completion stage that completes when the dataset is opened.
	 */
	public static CompletionStage<EmailDataset> openDataset(Path dsFile) {
		CompletableFuture<EmailDataset> cf = new CompletableFuture<>();
		ForkJoinPool.commonPool().execute(() -> {
			try {
				if (Files.isDirectory(dsFile)) {
					if (!Files.exists(dsFile.resolve("index")) || !Files.exists(dsFile.resolve("database.mv.db"))) {
						throw new IOException("Invalid dataset directory.");
					}
					Connection dbConn = DriverManager.getConnection(getJdbcUrl(dsFile.resolve("database.mv.db")));
					cf.complete(new EmailDataset(dsFile, dsFile.getParent().resolve(dsFile.getFileName().toString() + ".zip"), dbConn));
				} else {
					Path openDir = Files.createTempDirectory(Path.of("."), "email-dataset");
					var zip = new ZipFile(dsFile.toFile());
					zip.extractAll(openDir.toAbsolutePath().toString());
					zip.close();
					Connection dbConn = DriverManager.getConnection(getJdbcUrl(openDir.resolve("database.mv.db")));
					cf.complete(new EmailDataset(openDir, dsFile, dbConn));
				}
			} catch (Exception e) {
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	/**
	 * Zips a dataset directory into a ZIP file for storage. The directory
	 * should contain an "index" directory containing Apache Lucene index files,
	 * and a "database.mv.db" file that holds the relational database.
	 * @param dir The directory to use. It will be deleted by this method.
	 * @param file The file to place the ZIP file at. If it already exists, it
	 *             will be overwritten.
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

package nl.andrewl.email_indexer.data;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

/**
 * An email dataset is a complete set of emails that have been parsed from one
 * or more mbox files, and an Apache Lucene index directory. It is stored on the
 * disk as a ZIP file.
 */
public class EmailDataset {
	/**
	 * The directory that this dataset resides in.
	 */
	private final Path openDir;

	/**
	 * The database connection used by this dataset, while it's open.
	 */
	private Connection dbConn;

	public EmailDataset(Path openDir) throws SQLException {
		this.openDir = openDir;
		establishConnection();
	}

	public void establishConnection() throws SQLException {
		if (this.dbConn != null && !this.dbConn.isClosed()) return;
		this.dbConn = DriverManager.getConnection(getJdbcUrl(openDir.resolve("database.mv.db")));
	}

	public Connection getConnection() {
		return this.dbConn;
	}

	public Path getOpenDir() {
		return this.openDir;
	}

	public Path getIndexDir() {
		return this.openDir.resolve("index");
	}

	public Path getDatabaseFile() {
		return this.openDir.resolve("database.mv.db");
	}

	public void close() throws SQLException {
		try (var stmt = dbConn.prepareStatement("SHUTDOWN COMPACT;")) {
			stmt.execute();
		}
		this.dbConn.close();
	}

	/**
	 * Opens a dataset from the given file.
	 * @param dsFile The file to open.
	 * @return A completion stage that completes when the dataset is opened.
	 */
	public static CompletionStage<EmailDataset> open(Path dsFile) {
		CompletableFuture<EmailDataset> cf = new CompletableFuture<>();
		ForkJoinPool.commonPool().execute(() -> {
			try {
				if (Files.isDirectory(dsFile)) {
					if (!Files.exists(dsFile.resolve("index")) || !Files.exists(dsFile.resolve("database.mv.db"))) {
						throw new IOException("Invalid dataset directory. A dataset must contain an \"index\" directory, and a \"database.mv.db\" file.");
					}
					cf.complete(new EmailDataset(dsFile));
				} else if (dsFile.getFileName().toString().toLowerCase().endsWith(".zip")) {
					String filename = dsFile.getFileName().toString();
					String dirName = filename.substring(0, filename.lastIndexOf('.'));
					// Add '_' prefix until we have a new directory that doesn't exist yet.
					Path openDir = dsFile.resolveSibling(dirName);
					while (Files.exists(openDir)) {
						dirName = "_" + dirName;
						openDir = dsFile.resolveSibling(dirName);
					}
					Files.createDirectory(openDir);
					var zip = new ZipFile(dsFile.toFile());
					zip.extractAll(openDir.toAbsolutePath().toString());
					zip.close();
					cf.complete(new EmailDataset(openDir));
				} else {
					throw new IOException("Invalid file.");
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
	 * @return A completion stage that completes when the dataset is saved.
	 */
	public static CompletionStage<Void> buildZip(Path dir, Path file) {
		CompletableFuture<Void> cf = new CompletableFuture<>();
		ForkJoinPool.commonPool().execute(() -> {
			var zip = new ZipFile(file.toFile());
			ZipParameters params = new ZipParameters();
			params.setCompressionLevel(CompressionLevel.ULTRA);
			params.setOverrideExistingFilesInZip(true);
			try {
				zip.addFolder(dir.resolve("index").toFile(), params);
				zip.addFile(dir.resolve("database.mv.db").toFile(), params);
				cf.complete(null);
			} catch (IOException e) {
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	public static String getJdbcUrl(Path dbFile) {
		String dbFileName = dbFile.toAbsolutePath().toString();
		if (dbFileName.endsWith(".mv.db")) {
			dbFileName = dbFileName.substring(0, dbFileName.length() - ".mv.db".length());
		}
		return "jdbc:h2:file:" + dbFileName + ";DB_CLOSE_ON_EXIT=FALSE";
	}
}

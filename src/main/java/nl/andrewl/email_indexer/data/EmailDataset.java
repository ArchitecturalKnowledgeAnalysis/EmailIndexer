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
	private final Path openDir;
	private final Path dsFile;
	private Connection dbConn;

	public EmailDataset(Path openDir, Path dsFile) throws SQLException {
		this.openDir = openDir;
		this.dsFile = dsFile;
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
						throw new IOException("Invalid dataset directory.");
					}
					cf.complete(new EmailDataset(dsFile, dsFile.getParent().resolve(dsFile.getFileName().toString() + ".zip")));
				} else {
					Path openDir = Files.createTempDirectory(Path.of("."), "email-dataset");
					var zip = new ZipFile(dsFile.toFile());
					zip.extractAll(openDir.toAbsolutePath().toString());
					zip.close();
					cf.complete(new EmailDataset(openDir, dsFile));
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

package nl.andrewl.email_indexer.data;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import nl.andrewl.email_indexer.util.Async;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

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

	/**
	 * Constructs a dataset from the given directory which should contain a
	 * valid dataset. Use {@link EmailDataset#open(Path)} in most cases.
	 * @param openDir The directory containing the dataset.
	 * @throws SQLException If a connection to the database could not be
	 * established.
	 */
	public EmailDataset(Path openDir) throws SQLException {
		this.openDir = openDir;
		establishConnection();
	}

	public void establishConnection() throws SQLException {
		if (this.dbConn != null) return;
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

	public Path getMetadataFile() {
		return this.openDir.resolve("metadata.properties");
	}

	/**
	 * Gets the integer version number of this dataset.
	 * @return The version number.
	 * @throws IOException If an error occurs.
	 */
	public int getVersion() throws IOException {
		Properties props = new Properties();
		if (Files.exists(getMetadataFile())) {
			props.load(Files.newBufferedReader(getMetadataFile()));
			if (!props.containsKey("version")) {
				props.setProperty("version", "1");
				props.store(Files.newBufferedWriter(getMetadataFile()), null);
			}
		} else {
			// No properties file exists. That means it must be version 1.
			props.setProperty("version", "1");
			props.store(Files.newBufferedWriter(getMetadataFile()), null);
		}
		try {
			return Integer.parseInt(props.getProperty("version"));
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Closes all resources used by this dataset. This involves closing the
	 * database connection(s), and issuing a COMPACT command which may take
	 * some time.
	 * @return A future that completes when the dataset is successfully closed.
	 */
	public CompletableFuture<Void> close() {
		return Async.run(() -> {
			try (var stmt = dbConn.prepareStatement("SHUTDOWN COMPACT;")) {
				stmt.execute();
			}
			this.dbConn.close();
			this.dbConn = null;
		});
	}

	/**
	 * Opens a dataset from the given file.
	 * @param dsFile The file to open.
	 * @return A future that completes when the dataset is opened.
	 */
	public static CompletableFuture<EmailDataset> open(Path dsFile) {
		return Async.supply(() -> {
			if (Files.isDirectory(dsFile)) {
				if (!Files.exists(dsFile.resolve("index")) || !Files.exists(dsFile.resolve("database.mv.db"))) {
					throw new IOException("Invalid dataset directory. A dataset must contain an \"index\" directory, and a \"database.mv.db\" file.");
				}
				return new EmailDataset(dsFile);
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
				return new EmailDataset(openDir);
			} else {
				throw new IOException("Invalid file.");
			}
		});
	}

	/**
	 * Zips a dataset directory into a ZIP file for storage. The directory
	 * should contain an "index" directory containing Apache Lucene index files,
	 * and a "database.mv.db" file that holds the relational database.
	 * @param dir The directory to use. It will be deleted by this method.
	 * @param file The file to place the ZIP file at. If it already exists, it
	 *             will be overwritten.
	 * @return A future that completes when the dataset is saved.
	 */
	public static CompletableFuture<Void> buildZip(Path dir, Path file) {
		return Async.run(() -> {
			ZipParameters params = new ZipParameters();
			params.setOverrideExistingFilesInZip(true);
			try (var zip = new ZipFile(file.toFile())) {
				zip.addFolder(dir.resolve("index").toFile(), params);
				zip.addFile(dir.resolve("database.mv.db").toFile(), params);
			}
		});
	}

	public static String getJdbcUrl(Path dbFile) {
		String dbFileName = dbFile.toAbsolutePath().toString();
		if (dbFileName.endsWith(".mv.db")) {
			dbFileName = dbFileName.substring(0, dbFileName.length() - ".mv.db".length());
		}
		return "jdbc:h2:file:" + dbFileName + ";DB_CLOSE_ON_EXIT=FALSE";
	}
}

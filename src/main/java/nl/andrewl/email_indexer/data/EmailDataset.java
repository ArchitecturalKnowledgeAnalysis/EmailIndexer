package nl.andrewl.email_indexer.data;

import nl.andrewl.email_indexer.data.imports.DirectoryImporter;
import nl.andrewl.email_indexer.data.imports.EmailDatasetImporter;
import nl.andrewl.email_indexer.data.imports.ZipImporter;
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
		this.dbConn = DriverManager.getConnection(getJdbcUrl(getDatabaseFile()));
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
	 * Gets this dataset's metadata properties. If no metadata file exists yet,
	 * it will be created, and we assume the dataset was at version 1, since
	 * metadata was introduced in version 2.
	 * @return The properties which were loaded.
	 * @throws IOException If an error occurs while loading or creating the file.
	 */
	public Properties getMetadata() throws IOException {
		Properties props = new Properties();
		if (Files.notExists(getMetadataFile())) {
			props.setProperty("version", "1");
			try (var writer = Files.newBufferedWriter(getMetadataFile())) {
				props.store(writer, null);
			}
		} else {
			try (var reader = Files.newBufferedReader(getMetadataFile())) {
				props.load(reader);
			}
		}
		return props;
	}

	/**
	 * Gets the integer version number of this dataset.
	 * @return The version number.
	 * @throws IOException If an error occurs.
	 */
	public int getVersion() throws IOException {
		var props = getMetadata();
		try {
			return Integer.parseInt(props.getProperty("version", "1"));
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
		EmailDatasetImporter importer;
		if (dsFile.getFileName().toString().toLowerCase().endsWith(".zip")) {
			importer = new ZipImporter();
		} else {
			importer = new DirectoryImporter();
		}
		return importer.importFrom(dsFile);
	}

	/**
	 * Derives a JDBC connection URL for an H2 database file at a given path.
	 * @param dbFile The database file.
	 * @return A string that can be used to obtain a connection to the database.
	 */
	public static String getJdbcUrl(Path dbFile) {
		String dbFileName = dbFile.toAbsolutePath().toString();
		if (dbFileName.endsWith(".mv.db")) {
			dbFileName = dbFileName.substring(0, dbFileName.length() - ".mv.db".length());
		}
		return "jdbc:h2:file:" + dbFileName + ";DB_CLOSE_ON_EXIT=FALSE";
	}
}

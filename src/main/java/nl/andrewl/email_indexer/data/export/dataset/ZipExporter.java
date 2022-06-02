package nl.andrewl.email_indexer.data.export.dataset;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.export.EmailDatasetExporter;
import nl.andrewl.email_indexer.util.Async;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * An exporter that simply exports the dataset to a ZIP file archive.
 * <p>
 *     Note: In order to ensure data integrity, the dataset must be temporarily
 *     closed during the export. It will be reopened at the end of the export.
 * </p>
 */
public class ZipExporter implements EmailDatasetExporter {
	@Override
	public CompletableFuture<Void> export(EmailDataset ds, Path path) {
		return Async.run(() -> {
			if (Files.exists(path) && !path.getFileName().toString().endsWith(".zip")) {
				throw new IllegalArgumentException("Cannot export dataset to non-zip file: " + path);
			}
			if (Files.isDirectory(path)) {
				throw new IllegalArgumentException("Cannot export dataset to directory: " + path);
			}
			if (!Files.exists(path.getParent())) {
				throw new IllegalArgumentException("Cannot export dataset into directory that doesn't exist.");
			}
			ZipParameters params = new ZipParameters();
			params.setOverrideExistingFilesInZip(true);
			try (var zip = new ZipFile(path.toFile())) {
				zip.addFolder(ds.getIndexDir().toFile(), params);
				zip.addFile(ds.getMetadataFile().toFile(), params);
				try {// Close the database prior to zipping it.
					ds.close().join();
					zip.addFile(ds.getDatabaseFile().toFile(), params);
				} finally {// Reopen the connection, whether we were successful or not.
					ds.establishConnection();
				}
			}
		});
	}
}

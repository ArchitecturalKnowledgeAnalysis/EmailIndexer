package nl.andrewl.email_indexer.data.export.dataset;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.export.EmailDatasetExporter;
import nl.andrewl.email_indexer.util.Async;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * An exporter that simply exports the dataset to a ZIP file archive.
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
			ds.close().join();
			EmailDataset.buildZip(ds.getOpenDir(), path);
			ds.establishConnection();
		});
	}
}

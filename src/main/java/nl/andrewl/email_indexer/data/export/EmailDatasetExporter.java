package nl.andrewl.email_indexer.data.export;

import nl.andrewl.email_indexer.data.EmailDataset;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * This interface is implemented by components that can export an email dataset
 * to a particular format.
 */
public interface EmailDatasetExporter {
	/**
	 * Exports a dataset to the given path.
	 * @param ds The dataset to export.
	 * @param path The path to export to.
	 * @return A future that completes when the dataset has been exported.
	 */
	CompletableFuture<Void> export(EmailDataset ds, Path path);
}

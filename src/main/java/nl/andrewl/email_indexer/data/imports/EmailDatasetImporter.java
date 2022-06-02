package nl.andrewl.email_indexer.data.imports;

import nl.andrewl.email_indexer.data.EmailDataset;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * A common interface for importing datasets (into memory, i.e. Java) from
 * various sources, like directories, ZIP files, etc.
 */
public interface EmailDatasetImporter {
	/**
	 * Imports a dataset from the given path.
	 * @param path The path to import the dataset from.
	 * @return A future that completes once the dataset is imported, and
	 * provides the dataset upon completion. This future may complete
	 * exceptionally if an error occurs and the dataset could not be imported.
	 */
	CompletableFuture<EmailDataset> importFrom(Path path);
}

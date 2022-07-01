package nl.andrewl.email_indexer.data.imports;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.util.Async;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Importer that imports a dataset from a directory. This is the most common
 * import use case, as this is the default format in which datasets are stored.
 */
public class DirectoryImporter implements EmailDatasetImporter {
	@Override
	public CompletableFuture<EmailDataset> importFrom(Path path) {
		return Async.supply(() -> {
			if (!Files.isDirectory(path)) throw new IllegalArgumentException(path + " is not a directory.");
			if (Files.notExists(path.resolve("index")) || Files.notExists(path.resolve("database.db"))) {
				throw new IllegalArgumentException("Invalid dataset directory. A dataset must contain an \"index\" directory, and a \"database.db\" file.");
			}
			return new EmailDataset(path);
		});
	}
}

package nl.andrewl.email_indexer.data.imports;

import net.lingala.zip4j.ZipFile;
import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.util.Async;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Imports datasets from ZIP files.
 */
public class ZipImporter implements EmailDatasetImporter {
	@Override
	public CompletableFuture<EmailDataset> importFrom(Path path) {
		return Async.supply(() -> {
			String filename = path.getFileName().toString();
			if (!Files.isRegularFile(path) || !path.getFileName().toString().toLowerCase().endsWith(".zip")) {
				throw new IllegalArgumentException(path + " is not a .zip file.");
			}
			String dirName = filename.substring(0, filename.lastIndexOf('.'));
			Path openDir = path.resolveSibling(dirName);
			while (Files.exists(openDir)) {
				dirName = "_" + dirName;
				openDir = path.resolveSibling(dirName);
			}
			Files.createDirectory(openDir);
			try (var zip = new ZipFile(path.toFile())) {
				zip.extractAll(openDir.toAbsolutePath().toString());
			}
			return new EmailDataset(openDir);
		});
	}
}

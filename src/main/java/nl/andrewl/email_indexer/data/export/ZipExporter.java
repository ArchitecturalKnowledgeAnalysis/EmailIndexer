package nl.andrewl.email_indexer.data.export;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.util.Async;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ZipExporter implements EmailDatasetExporter {
	@Override
	public CompletableFuture<Void> exportTo(EmailDataset ds, Path file) {
		return Async.run(() -> {
			if (Files.exists(file) && !file.getFileName().toString().endsWith(".zip")) {
				throw new IllegalArgumentException("Cannot export dataset to non-zip file: " + file);
			}
			if (Files.isDirectory(file)) {
				throw new IllegalArgumentException("Cannot export dataset to directory: " + file);
			}
		}).thenAccept(unused -> EmailDataset.buildZip(ds.getOpenDir(), file));
	}
}

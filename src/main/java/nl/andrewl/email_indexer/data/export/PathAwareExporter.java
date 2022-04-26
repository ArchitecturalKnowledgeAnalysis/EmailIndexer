package nl.andrewl.email_indexer.data.export;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.util.Async;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class PathAwareExporter implements EmailDatasetExporter {
	@Override
	public CompletableFuture<Void> exportTo(EmailDataset ds, Path file) {
		return Async.supply(() -> {
			String filename = file.getFileName().toString().toLowerCase();
			int idx = filename.lastIndexOf('.');
			if (idx == -1 || idx == filename.length() - 1) throw new IllegalArgumentException("Unsupported file type: " + filename);
			String ext = filename.substring(idx + 1);
			EmailDatasetExporter exporter = switch (ext) {
				case "zip" -> new ZipExporter();
				default -> throw new IllegalArgumentException("Unsupported file type: " + ext);
			};
			return exporter;
		}).thenAccept(exporter -> exporter.exportTo(ds, file));
	}
}

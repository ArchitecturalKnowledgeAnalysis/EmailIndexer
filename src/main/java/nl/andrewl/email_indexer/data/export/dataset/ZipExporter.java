package nl.andrewl.email_indexer.data.export.dataset;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.export.EmailDatasetExporter;
import nl.andrewl.email_indexer.data.export.ExporterParameters;
import nl.andrewl.email_indexer.util.Async;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ZipExporter implements EmailDatasetExporter {
	@Override
	public CompletableFuture<Void> export(ExporterParameters exportParameters) {
		Path file = exportParameters.getOutputPath();
		EmailDataset ds = exportParameters.getDataset();
		return Async.run(() -> {
			if (Files.exists(file) && !file.getFileName().toString().endsWith(".zip")) {
				throw new IllegalArgumentException("Cannot export dataset to non-zip file: " + file);
			}
			if (Files.isDirectory(file)) {
				throw new IllegalArgumentException("Cannot export dataset to directory: " + file);
			}
		})
				.thenAccept(unused -> ds.close())
				.thenAccept(unused -> EmailDataset.buildZip(ds.getOpenDir(), file))
				.thenCompose(unused -> Async.run(ds::establishConnection));
	}
}

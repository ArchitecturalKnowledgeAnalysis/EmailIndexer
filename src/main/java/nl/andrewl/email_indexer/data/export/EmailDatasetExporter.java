package nl.andrewl.email_indexer.data.export;

import nl.andrewl.email_indexer.data.EmailDataset;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface EmailDatasetExporter {
	CompletableFuture<Void> exportTo(EmailDataset ds, Path file);
}

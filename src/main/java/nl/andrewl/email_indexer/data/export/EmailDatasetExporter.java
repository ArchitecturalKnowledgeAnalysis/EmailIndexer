package nl.andrewl.email_indexer.data.export;

import java.util.concurrent.CompletableFuture;

public interface EmailDatasetExporter {
	CompletableFuture<Void> export(ExporterParameters exportParams);
}

package nl.andrewl.email_indexer.data.export;

import nl.andrewl.email_indexer.data.export.dataset.ZipExporter;
import nl.andrewl.email_indexer.data.export.query.PdfQueryExporter;
import nl.andrewl.email_indexer.data.export.query.PlainTextQueryExporter;
import nl.andrewl.email_indexer.util.Async;

import java.util.concurrent.CompletableFuture;

public class TypeAwareExporter implements EmailDatasetExporter {
	@Override
	public CompletableFuture<Void> export(ExporterParameters exportParameters) {
		return Async.supply(() -> {
			return switch (exportParameters.getOutputType()) {
				case DATASET_ZIP -> new ZipExporter();
				case QUERY_TXT -> new PlainTextQueryExporter();
				case QUERY_PDF -> new PdfQueryExporter();
				default -> throw new IllegalArgumentException(
						"Unsupported file type: " + exportParameters.getOutputType().toString());
			};
		}).thenAccept(exporter -> exporter.export(exportParameters));
	}
}

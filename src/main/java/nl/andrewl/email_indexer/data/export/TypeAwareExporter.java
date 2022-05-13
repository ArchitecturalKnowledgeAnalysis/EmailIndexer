package nl.andrewl.email_indexer.data.export;

import nl.andrewl.email_indexer.data.export.ExporterParameters.OutputType;
import nl.andrewl.email_indexer.data.export.dataset.ZipExporter;
import nl.andrewl.email_indexer.data.export.query.PdfQueryExporter;
import nl.andrewl.email_indexer.data.export.query.PlainTextQueryExporter;
import nl.andrewl.email_indexer.util.Async;

import java.util.concurrent.CompletableFuture;

/**
 * Standardized builder class for exporters that
 * finds the right exporter and exports data.
 */
public class TypeAwareExporter implements EmailDatasetExporter {
	@Override
	public CompletableFuture<Void> export(ExporterParameters exportParameters) {
		if (exportParameters.getOutputType() == OutputType.UNSET) {
			exportParameters.withOutputType(getOutputTypeFromExt(exportParameters));
		}
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

	/**
	 * Removes the requirements for the output type to be set.
	 */
	private OutputType getOutputTypeFromExt(ExporterParameters exportParameters) {
		String path = exportParameters.getOutputPath().toString().toLowerCase();
		int index = path.lastIndexOf(".");
		if (index == -1) {
			return OutputType.UNSET;
		}
		String ext = path.substring(index + 1);
		return switch (ext) {
			case "txt" -> OutputType.QUERY_TXT;
			default -> OutputType.UNSET;
		};
	}
}

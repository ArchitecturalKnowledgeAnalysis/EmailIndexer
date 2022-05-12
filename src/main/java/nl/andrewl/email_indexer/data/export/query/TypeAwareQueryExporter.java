package nl.andrewl.email_indexer.data.export.query;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.export.ExportException;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;

/**
 * Query exporter that automatically invokes the right QueryExporter
 * using the provided file type.
 */
public final class TypeAwareQueryExporter implements QueryExporter {

    @Override
    public CompletableFuture<Void> export(QueryExporterParams exportParams) throws ExportException {
        QueryExporter exporter = switch (exportParams.fileType()) {
            case "txt" -> new PlainTextQueryExporter();
            case "pdf" -> new PdfQueryExporter();
            default -> throw new IllegalArgumentException("Unsupported export file type: " + exportParams.fileType());
        };
        return new EmailIndexSearcher().searchAsync(exportParams.dataset(), exportParams.query())
                .handleAsync((emailIds, throwable) -> {
                    if (throwable != null) {
                        throw new ExportException("An error occurred while exporting.", throwable);
                    }
                    List<EmailEntryPreview> emails = emailIds.parallelStream()
                            .map(id -> exportParams.repository().findPreviewById(id).orElse(null))
                            .filter(Objects::nonNull)
                            .limit(exportParams.maxResultCount())
                            .toList();
                    return new QueryExporterParams(exportParams, emails);
                }).thenAccept((params) -> exporter.export(params));
    }
}

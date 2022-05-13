package nl.andrewl.email_indexer.data.export.query;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Objects;

import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.export.EmailDatasetExporter;
import nl.andrewl.email_indexer.data.export.ExporterParameters;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;

public abstract class QueryExporter implements EmailDatasetExporter {
    @Override
    public CompletableFuture<Void> export(ExporterParameters exportParams) {
        return new EmailIndexSearcher().searchAsync(exportParams.getDataset(), exportParams.getQuery())
                .handleAsync((emailIds, throwable) -> {
                    List<EmailEntryPreview> emails = emailIds.parallelStream()
                            .map(id -> exportParams.getRepository().findPreviewById(id).orElse(null))
                            .filter(Objects::nonNull)
                            .limit(exportParams.getMaxResultCount())
                            .toList();
                    return exportParams.withEmails(emails);
                }).thenAccept(newParams -> doQueryExport(newParams));
    }

    protected abstract CompletableFuture<Void> doQueryExport(ExporterParameters exportParams);
}

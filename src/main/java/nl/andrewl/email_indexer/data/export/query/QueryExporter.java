package nl.andrewl.email_indexer.data.export.query;

import java.util.concurrent.CompletableFuture;

import nl.andrewl.email_indexer.data.export.ExportException;

/**
 * Base class for query exporter implementations.
 */
public interface QueryExporter {
    /**
     * Exports query results to permanent storage.
     * 
     * @param exportParams parameter object used to export.
     * @throws ExportException thrown when the export failed.
     */
    CompletableFuture<Void> export(QueryExporterParams exportParams) throws ExportException;
}

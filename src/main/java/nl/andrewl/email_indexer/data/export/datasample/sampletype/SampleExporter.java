package nl.andrewl.email_indexer.data.export.datasample.sampletype;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.export.EmailDatasetExporter;
import nl.andrewl.email_indexer.data.export.ExporterParameters;
import nl.andrewl.email_indexer.data.export.datasample.datatype.TypeExporter;
import nl.andrewl.email_indexer.util.Async;

/**
 * Parent calss of concrete SampleExporter implementations, that abstracts away
 * common asynchronous export behaviour.
 */
public abstract class SampleExporter implements EmailDatasetExporter {
    /**
     * The exporter used to export data.
     */
    protected TypeExporter typeExporter;

    /**
     * The parameters to use during the export.
     */
    protected final ExporterParameters params;

    /**
     * @param typeExporter The type exporter object used to export data.
     * @param params       The exporter parameters used to export data.
     */
    protected SampleExporter(TypeExporter typeExporter, ExporterParameters params) {
        this.typeExporter = typeExporter;
        this.params = params;
    }

    @Override
    public CompletableFuture<Void> export(EmailDataset ds, Path path) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        Async.run(() -> {
            try {
                doExport(ds, path);
                cf.complete(null);
            } catch (Exception ex) {
                cf.completeExceptionally(ex);
            }
        });
        return cf;
    }

    /**
     * Performs the export using the set set data type exporter and exporter
     * parameters.
     * 
     * @param ds   The dataset to export.
     * @param path The path to export to.
     */
    protected abstract void doExport(EmailDataset ds, Path path) throws Exception;
}

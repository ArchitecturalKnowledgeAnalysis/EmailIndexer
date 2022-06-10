package nl.andrewl.email_indexer.data.export.query;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.export.EmailDatasetExporter;
import nl.andrewl.email_indexer.data.export.ExporterParameters;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.email_indexer.data.search.EmailSearcher;
import nl.andrewl.email_indexer.util.Async;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.lucene.queryparser.classic.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract parent class for any exporter that exports results from a Lucene
 * query search.
 */
public abstract class QueryExporter implements EmailDatasetExporter {
    /**
     * The parameters to use during the export.
     */
    protected final ExporterParameters params;

    protected QueryExporter(ExporterParameters params) {
        this.params = params;
    }

    @Override
    public CompletableFuture<Void> export(EmailDataset ds, Path path) {
        return Async.run(() -> {
            if (this.params.getFilters() == null) {
                exportWithQuery(ds, path);
            } else {
                exportWithFilter(ds, path);
            }
        });
    }

    private void exportWithQuery(EmailDataset ds, Path path) throws IOException, ParseException {
        List<Long> rootIds = new EmailIndexSearcher().search(ds, params.getQuery(), params.getMaxResultCount());
        params.withMaxResultCount(rootIds.size());
        beforeExport(ds, path);
        EmailRepository emailRepo = new EmailRepository(ds);
        TagRepository tagRepo = new TagRepository(ds);
        int rank = 1;
        for (var id : rootIds) {
            Optional<EmailEntry> optionalEmail = emailRepo.findEmailById(id);
            if (optionalEmail.isPresent()) {
                exportEmail(optionalEmail.get(), rank++, emailRepo, tagRepo);
            }
        }
        afterExport();
    }

    private void exportWithFilter(EmailDataset ds, Path path) throws IOException, ParseException {
        beforeExport(ds, path);
        EmailRepository emailRepo = new EmailRepository(ds);
        TagRepository tagRepo = new TagRepository(ds);
        new EmailSearcher(ds).findAll(1, this.params.getMaxResultCount(), this.params.getFilters())
                .handle((results, throwable) -> {
                    if (throwable != null) {
                        throw new RuntimeException(throwable);
                    }
                    int rank = 0;
                    for (EmailEntryPreview email : results.emails()) {
                        Optional<EmailEntry> entry = emailRepo.findEmailById(email.id());
                        if (entry.isPresent()) {
                            try {
                                exportEmail(entry.get(), rank++, emailRepo, tagRepo);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    return null;
                }).join();
        afterExport();
    }

    /**
     * This method is called before any emails are exported, so that the
     * exporter has an opportunity to prepare any prerequisite documents or
     * headings before the export starts.
     * 
     * @param ds   The dataset that will be exported.
     * @param path The path to export to.
     * @throws IOException If an error occurs while preparing the export.
     */
    protected void beforeExport(EmailDataset ds, Path path) throws IOException {
        // Do nothing by default; children may override this.
    }

    /**
     * This method is called once for each email in a search result, in the
     * order in which they appear.
     * 
     * @param email     The email which was found.
     * @param rank      The email's rank in the search results. 1 is the first, and
     *                  it increments for each email thereafter.
     * @param emailRepo An email repository to use.
     * @param tagRepo   A tag repository to use.
     * @throws IOException If an error occurs while exporting the email.
     */
    protected abstract void exportEmail(EmailEntry email, int rank, EmailRepository emailRepo, TagRepository tagRepo)
            throws IOException;

    /**
     * This method is called after all emails have been exported, to do cleanup
     * operations and add any final touches to the export.
     * 
     * @throws IOException If an error occurs while finalizing the export.
     */
    protected void afterExport() throws IOException {
        // Do nothing by default; children may override this.
    }
}

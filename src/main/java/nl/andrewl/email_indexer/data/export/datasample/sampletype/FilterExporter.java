package nl.andrewl.email_indexer.data.export.datasample.sampletype;

import java.nio.file.Path;
import java.util.Optional;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.export.ExporterParameters;
import nl.andrewl.email_indexer.data.export.datasample.datatype.TypeExporter;
import nl.andrewl.email_indexer.data.search.EmailSearchResult;
import nl.andrewl.email_indexer.data.search.EmailSearcher;

/**
 * Class for any exporter that exports results from a filter
 * query search.
 */
public class FilterExporter extends SampleExporter {
    public FilterExporter(TypeExporter typeExporter, ExporterParameters params) {
        super(typeExporter, params);
    }

    protected void doExport(EmailDataset ds, Path path) throws Exception {
        if (this.params.getFilters() == null) {
            throw new IllegalArgumentException("Filter parameter cannot be null.");
        }
        typeExporter.beforeExport(ds, path, this.params);
        EmailRepository emailRepo = new EmailRepository(ds);
        TagRepository tagRepo = new TagRepository(ds);
        EmailSearchResult results = new EmailSearcher(ds)
                .findAll(1, this.params.getMaxResultCount(), this.params.getFilters()).join();
        int rank = 1;
        for (EmailEntryPreview email : results.emails()) {
            Optional<EmailEntry> entry = emailRepo.findEmailById(email.id());
            if (entry.isPresent()) {
                typeExporter.exportEmail(entry.get(), rank++, emailRepo, tagRepo);
            }
        }
        typeExporter.afterExport();
    }
}

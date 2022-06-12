package nl.andrewl.email_indexer.data.export.datasample.sampletype;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.export.ExporterParameters;
import nl.andrewl.email_indexer.data.export.datasample.datatype.TypeExporter;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Class for any exporter that exports results from a Lucene
 * query search.
 */
public class QueryExporter extends SampleExporter {
    public QueryExporter(TypeExporter typeExporter, ExporterParameters params) {
        super(typeExporter, params);
    }

    protected void doExport(EmailDataset ds, Path path) throws Exception {
        if (this.params.getQuery() == null || this.params.getQuery().isBlank()) {
            throw new IllegalArgumentException("Query parameter cannot be blank or null.");
        }
        List<Long> rootIds = new EmailIndexSearcher().search(ds, params.getQuery(), params.getMaxResultCount());
        params.withMaxResultCount(rootIds.size());
        typeExporter.beforeExport(ds, path, this.params);
        EmailRepository emailRepo = new EmailRepository(ds);
        TagRepository tagRepo = new TagRepository(ds);
        int rank = 1;
        for (var id : rootIds) {
            Optional<EmailEntry> optionalEmail = emailRepo.findEmailById(id);
            if (optionalEmail.isPresent()) {
                typeExporter.exportEmail(optionalEmail.get(), rank++, emailRepo, tagRepo);
            }
        }
        typeExporter.afterExport();
    }
}

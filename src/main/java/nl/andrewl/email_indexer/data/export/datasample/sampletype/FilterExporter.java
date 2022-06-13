package nl.andrewl.email_indexer.data.export.datasample.sampletype;

import java.nio.file.Path;
import java.util.List;
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
	/**
	 * The page size used during the export.
	 */
	private static final int PAGE_SIZE = 100;

	public FilterExporter(TypeExporter typeExporter, ExporterParameters params) {
		super(typeExporter, params);
	}

	protected void exportSample(EmailDataset ds, Path path) throws Exception {
		if (this.params.getFilters() == null) {
			throw new IllegalArgumentException("Filter parameter cannot be null.");
		}
		typeExporter.beforeExport(ds, path, this.params);
		exportNextChunk(1, 1, new EmailRepository(ds), new TagRepository(ds), new EmailSearcher(ds));
		typeExporter.afterExport();
	}

	/**
	 * Recursively exports EmailSearcher results.
	 *
	 * @param page      Current page index.
	 * @param rank      Current email rank.
	 * @param emailRepo The email repository.
	 * @param tagRepo   The tag repository.
	 * @param searcher  The searcher object.
	 * @throws Exception Concrete implementations of TypeExporter can throw
	 *                   exceptions.
	 */
	private void exportNextChunk(int page, int rank, EmailRepository emailRepo, TagRepository tagRepo, EmailSearcher searcher) throws Exception {
		EmailSearchResult results = searcher.findAll(page, PAGE_SIZE, this.params.getFilters()).join();
		List<EmailEntryPreview> emailResults = results.emails();
		for (int i = 0; i < results.emails().size() && rank <= this.params.getMaxResultCount(); i++) {
			EmailEntryPreview email = emailResults.get(i);
			Optional<EmailEntry> entry = emailRepo.findEmailById(email.id());
			if (entry.isPresent()) {
				typeExporter.exportEmail(entry.get(), rank++, emailRepo, tagRepo);
			}
		}
		if (rank <= this.params.getMaxResultCount() && results.hasNextPage()) {
			exportNextChunk(page + 1, rank, emailRepo, tagRepo, searcher);
		}
	}
}

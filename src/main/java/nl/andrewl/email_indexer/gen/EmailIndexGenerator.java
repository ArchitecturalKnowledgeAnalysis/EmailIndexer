package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.search.EmailSearchResult;
import nl.andrewl.email_indexer.data.search.EmailSearcher;
import nl.andrewl.email_indexer.data.search.SearchFilter;
import nl.andrewl.email_indexer.data.search.filter.HiddenFilter;
import nl.andrewl.email_indexer.util.FileUtils;
import nl.andrewl.email_indexer.util.Status;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;

/**
 * Component that generates Lucene search indexes from various sources.
 */
public class EmailIndexGenerator {
	private final Status status;

	public EmailIndexGenerator(Status status) {
		this.status = status;
	}

	public EmailIndexGenerator() {
		this(Status.noOp());
	}

	/**
	 * Generates the indexes for a dataset, based entirely on non-hidden emails.
	 * @param dataset The dataset to index.
	 * @throws IOException If an error occurs while reading or writing.
	 */
	public void generateIndex(EmailDataset dataset) throws IOException {
		if (Files.exists(dataset.getIndexDir())) {
			FileUtils.deleteFiles(dataset.getIndexDir());
		} else {
			Files.createDirectory(dataset.getIndexDir());
		}
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		EmailRepository repo = new EmailRepository(dataset);
		EmailSearcher searcher = new EmailSearcher(dataset);
		try (
				Directory emailDirectory = FSDirectory.open(dataset.getIndexDir());
				IndexWriter emailIndexWriter = new IndexWriter(emailDirectory, config)
		) {
			Collection<SearchFilter> filters = new HashSet<>();
			filters.add(new HiddenFilter(false));
			long count = searcher.countAll(filters).join();
			status.sendMessage("Indexing %d emails.".formatted(count));
			final int pageSize = 1000;
			int pageCount = (int) (count / pageSize) + (count % pageSize == 0 ? 0 : 1);
			for (int page = 1; page <= pageCount; page++) {
				status.sendMessage("Fetching page %d of %d.".formatted(page, pageCount));
				EmailSearchResult result = searcher.findAll(page, pageSize, filters).join();
				indexPage(result, repo, emailIndexWriter);
			}
			status.sendMessage("Indexing complete.");
		}
	}

	private void indexPage(EmailSearchResult result, EmailRepository repo, IndexWriter emailIndexWriter) {
		status.sendMessage("Indexing page %d containing %d emails.".formatted(result.page(), result.emails().size()));
		for (var email : result.emails()) {
			addToIndex(email, repo, emailIndexWriter);
		}
		status.sendMessage("Completed index of page %d.".formatted(result.page()));
	}

	private void addToIndex(EmailEntryPreview email, EmailRepository repo, IndexWriter emailIndexWriter) {
		repo.getBody(email.id()).ifPresent(body -> {
			Document doc = new Document();
			FieldType indexedStringType = new FieldType();
			indexedStringType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
			indexedStringType.setStored(false);
			indexedStringType.freeze();
			doc.add(new StoredField("id", email.id()));
			doc.add(new Field("subject", email.subject(), indexedStringType));
			doc.add(new Field("body", body, indexedStringType));
			// Store the root id. If we couldn't find a root id, use this email's id.
			long rootId = repo.findRootEmailByChildId(email.id()).map(EmailEntryPreview::id).orElse(email.id());
			doc.add(new StoredField("rootId", rootId));
			try {
				emailIndexWriter.addDocument(doc);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}

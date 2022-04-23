package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.EmailSearchResult;
import nl.andrewl.email_indexer.data.util.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Component that generates Lucene search indexes from various sources.
 */
public class EmailIndexGenerator {
	/**
	 * Generates the indexes for a dataset, based entirely on non-hidden emails.
	 * @param dataset The dataset to index.
	 * @param messageConsumer A consumer to accept messages emitted during the
	 *                        generation.
	 * @throws IOException If an error occurs while reading or writing.
	 */
	public void generateIndex(EmailDataset dataset, Consumer<String> messageConsumer) throws IOException {
		if (Files.exists(dataset.getIndexDir())) {
			FileUtils.deleteFiles(dataset.getIndexDir());
		} else {
			Files.createDirectory(dataset.getIndexDir());
		}
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		EmailRepository repo = new EmailRepository(dataset);
		try (
				Directory emailDirectory = FSDirectory.open(dataset.getIndexDir());
				IndexWriter emailIndexWriter = new IndexWriter(emailDirectory, config)
		) {
			long count = repo.countAll(false, null);
			final int pageCount = Runtime.getRuntime().availableProcessors() - 1;
			int itemsPerPage = (int) (count / pageCount);
			int remainderItems = (int) (count % pageCount);
			Set<EmailSearchResult> pages = new HashSet<>();
			for (int i = 0; i < pageCount; i++) {
				final int page = i + 1;
				messageConsumer.accept("Fetching page %d of %d".formatted(page, pageCount + 1));
				pages.add(repo.findAll(page, itemsPerPage, false, null).join());
			}
			messageConsumer.accept("Fetching page %d of %d".formatted(pageCount + 1, pageCount + 1));
			pages.add(repo.findAll(pageCount + 1, remainderItems, false, null).join());
			Set<Thread> threads = pages.stream()
					.map(result -> new Thread(() -> indexPage(result, repo, emailIndexWriter, messageConsumer)))
					.peek(Thread::start)
					.collect(Collectors.toSet());
			messageConsumer.accept("Spawned " + threads.size() + " threads to process indexes.");
			for (var thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			messageConsumer.accept("Indexing complete.");
		}
	}

	private void indexPage(EmailSearchResult result, EmailRepository repo, IndexWriter emailIndexWriter, Consumer<String> messageConsumer) {
		messageConsumer.accept("Indexing page %d containing %d emails.".formatted(result.page(), result.emails().size()));
		for (var email : result.emails()) {
			addToIndex(email, repo, emailIndexWriter);
		}
		messageConsumer.accept("Completed index of page %d.".formatted(result.page()));
	}

	private void addToIndex(EmailEntryPreview email, EmailRepository repo, IndexWriter emailIndexWriter) {
		repo.getBody(email.messageId()).ifPresent(body -> {
			Document doc = new Document();
			doc.add(new StringField("id", email.messageId(), Field.Store.YES));
			doc.add(new StringField("subject", email.subject(), Field.Store.NO));
			doc.add(new Field("body", body, TextField.TYPE_NOT_STORED));
			repo.findRootEmailByChildId(email.messageId()).ifPresent(rootEmail -> {
				doc.add(new StringField("rootId", rootEmail.messageId(), Field.Store.YES));
			});
			try {
				emailIndexWriter.addDocument(doc);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}

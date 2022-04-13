package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.EmailSearchResult;
import nl.andrewl.email_indexer.data.util.FileUtils;
import nl.andrewl.mboxparser.CompositeEmailHandler;
import nl.andrewl.mboxparser.EmailHandler;
import nl.andrewl.mboxparser.MBoxParser;
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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Component which parses a set of mbox files to produce a Lucene index.
 */
public class EmailIndexGenerator {
	/**
	 * Generates an email index and provides all indexed emails to any supplied
	 * email handlers for processing.
	 * @param inputDirs Directories to parse mbox files from.
	 * @param outputDir The directory to place the index data.
	 * @param messageConsumer A consumer to accept messages emitted during the
	 *                        generation.
	 * @param emailHandlers Handlers that will process any indexed emails.
	 * @throws IOException If an error occurs while reading or writing.
	 */
	public void generateIndex(Collection<Path> inputDirs, Path outputDir, Consumer<String> messageConsumer, EmailHandler... emailHandlers) throws IOException {
		Files.createDirectories(outputDir);
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		try (
				Directory emailDirectory = FSDirectory.open(outputDir);
				IndexWriter emailIndexWriter = new IndexWriter(emailDirectory, config)
		) {
			// Create a composite handler that has a collection of handlers that are called for each email parsed.
			var compositeHandler = new CompositeEmailHandler(new IndexingEmailHandler(emailIndexWriter));
			for (var h : emailHandlers) compositeHandler.withHandler(h);
			// Wrap the composite handler in a sanitizing handler to filter out plain junk.
			MBoxParser parser = new MBoxParser(new SanitizingEmailHandler(compositeHandler));
			for (var dir : inputDirs) {
				parseRecursive(dir, parser, messageConsumer);
			}
			messageConsumer.accept("Indexing complete.");
		}
	}

	/**
	 * Removes and regenerates the indexes for a dataset, based entirely on
	 * non-hidden emails.
	 * @param dataset The dataset to index.
	 * @param messageConsumer A consumer to accept messages emitted during the
	 *                        generation.
	 * @throws IOException If an error occurs while reading or writing.
	 */
	public void regenerateIndex(EmailDataset dataset, Consumer<String> messageConsumer) throws IOException {
		FileUtils.deleteFiles(dataset.getIndexDir());
		messageConsumer.accept("Removed existing index files.");
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		EmailRepository repo = new EmailRepository(dataset);
		Set<String> emailIds = new HashSet<>();
		try (
				Directory emailDirectory = FSDirectory.open(dataset.getIndexDir());
				IndexWriter emailIndexWriter = new IndexWriter(emailDirectory, config)
		) {
			int page = 1;
			EmailSearchResult result = repo.findAll(page, 1000, false, null);
			// Continue until there are no more pages.
			while (true) {
				messageConsumer.accept("Indexing page %d of %d of emails.".formatted(result.page(), result.pageCount()));
				for (var email : result.emails()) {
					if (!emailIds.contains(email.messageId())) {
						emailIds.add(email.messageId());
						String body = repo.getBody(email.messageId());
						if (body != null) {
							Document doc = new Document();
							doc.add(new StringField("id", email.messageId(), Field.Store.YES));
							doc.add(new StringField("subject", email.subject(), Field.Store.NO));
							doc.add(new Field("body", body, TextField.TYPE_NOT_STORED));
							try {
								emailIndexWriter.addDocument(doc);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				if (result.hasNextPage()) {
					page++;
					result = repo.findAll(page, 1000, false, null);
				} else {
					break;
				}
			}
			messageConsumer.accept("Indexing complete.");
		}
	}

	private void parseRecursive(Path dir, MBoxParser parser, Consumer<String> messageConsumer) throws IOException {
		messageConsumer.accept("Parsing directory: " + dir);
		try (var s = Files.list(dir)) {
			for (var p : s.toList()) {
				if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
					parseRecursive(p, parser, messageConsumer);
				} else if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(p)) {
					messageConsumer.accept("Parsing file: " + p);
					parser.parse(p);
				}
			}
		}
	}
}

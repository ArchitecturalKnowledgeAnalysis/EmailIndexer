package nl.andrewl.email_indexer.gen;

import nl.andrewl.mboxparser.CompositeEmailHandler;
import nl.andrewl.mboxparser.EmailHandler;
import nl.andrewl.mboxparser.MBoxParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
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
		Directory emailDirectory = FSDirectory.open(outputDir);
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		messageConsumer.accept("Initialized indexing components and configuration.");

		try (IndexWriter emailIndexWriter = new IndexWriter(emailDirectory, config)) {
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

package nl.andrewl.email_indexer.gen;

import nl.andrewl.mbox_parser.CompositeEmailHandler;
import nl.andrewl.mbox_parser.EmailHandler;
import nl.andrewl.mbox_parser.MBoxParser;
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

/**
 * Component which parses a set of mbox files to produce a Lucene index.
 */
public class EmailIndexGenerator {
	public void generateIndex(Collection<Path> inputDirs, Path outputDir, EmailHandler... emailHandlers) throws IOException {
		Files.createDirectories(outputDir);
		Directory emailDirectory = FSDirectory.open(outputDir);
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		try (IndexWriter emailIndexWriter = new IndexWriter(emailDirectory, config)) {
			// Create a composite handler that has a collection of handlers that are called for each email parsed.
			var compositeHandler = new CompositeEmailHandler(new IndexingEmailHandler(emailIndexWriter));
			for (var h : emailHandlers) compositeHandler.withHandler(h);
			// Wrap the composite handler in a sanitizing handler to filter out plain junk.
			MBoxParser parser = new MBoxParser(new SanitizingEmailHandler(compositeHandler));
			for (var dir : inputDirs) {
				parseRecursive(dir, parser);
			}
		}
	}

	private void parseRecursive(Path dir, MBoxParser parser) throws IOException {
		System.out.println("Parsing directory: " + dir);
		try (var s = Files.list(dir)) {
			for (var p : s.toList()) {
				if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
					parseRecursive(p, parser);
				} else if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(p)) {
					System.out.println("Parsing file: " + p);
					parser.parse(p);
				}
			}
		}
	}
}

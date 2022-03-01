package nl.andrewl.email_indexer.gen;

import nl.andrewl.mbox_parser.CompositeEmailHandler;
import nl.andrewl.mbox_parser.Email;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class EmailIndexGenerator {
	public void generateIndex(Path inputDir, Path outputDir, EmailHandler... emailHandlers) throws IOException {
		Files.createDirectories(outputDir);
		Directory emailDirectory = FSDirectory.open(outputDir);
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		List<Function<Email, Boolean>> filters = new ArrayList<>();
		filters.add(email -> email.charset != null);

		try (IndexWriter emailIndexWriter = new IndexWriter(emailDirectory, config)) {
			var handler = new CompositeEmailHandler(new IndexingEmailHandler(emailIndexWriter));
			for (var h : emailHandlers) handler.withHandler(h);
			MBoxParser parser = new MBoxParser(new FilterEmailHandler(handler, filters));
			try (var s = Files.list(inputDir)) {
				for (var p : s.toList()) parser.parse(p);
			}
		}

	}
}

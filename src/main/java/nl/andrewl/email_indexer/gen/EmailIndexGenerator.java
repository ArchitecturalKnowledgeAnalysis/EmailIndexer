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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class EmailIndexGenerator {
	public void generateIndex(Path inputDir, Path outputDir, EmailHandler... emailHandlers) throws IOException {
		Files.createDirectories(outputDir);
		Directory emailDirectory = FSDirectory.open(outputDir);
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		List<Function<Email, Boolean>> filters = new ArrayList<>();
		filters.add(email -> email.charset != null);
		List<Consumer<Email>> transformers = new ArrayList<>();
		transformers.add(email -> {
			String[] lines = email.readBodyAsText().split("\n");
			StringBuilder sb = new StringBuilder(email.body.length);
			for (var line : lines) {
				if (!line.trim().startsWith(">")) {
					sb.append(line).append("\n");
				}
			}
			email.body = sb.toString().getBytes(StandardCharsets.UTF_8);
			email.charset = StandardCharsets.UTF_8.name();
			email.transferEncoding = "8bit";
		});

		try (IndexWriter emailIndexWriter = new IndexWriter(emailDirectory, config)) {
			var handler = new CompositeEmailHandler(new IndexingEmailHandler(emailIndexWriter));
			for (var h : emailHandlers) handler.withHandler(h);
			MBoxParser parser = new MBoxParser(new FilterTransformEmailHandler(handler, filters, transformers));
			try (var s = Files.list(inputDir)) {
				for (var p : s.toList()) parser.parse(p);
			}
		}

	}
}

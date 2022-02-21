package nl.andrewl.email_indexer;

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

public class EmailIndexGenerator {
	public void generateIndex(Path inputDir, Path outputDir) throws IOException {
		Files.createDirectories(outputDir);
		Directory emailDirectory = FSDirectory.open(outputDir);
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter emailIndexWriter = new IndexWriter(emailDirectory, config);
		MBoxParser parser = new MBoxParser(new IndexingEmailHandler(emailIndexWriter));
		try (var s = Files.list(inputDir)) {
			for (var p : s.toList()) parser.parse(p);
		}
		emailIndexWriter.close();
	}
}

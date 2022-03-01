package nl.andrewl.email_indexer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EmailIndexSearcher {
	public SearchResult search(Path indexDir, String queryString, int page, int size) throws IOException, ParseException {
		if (page < 1) throw new IllegalArgumentException("page must be positive.");
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
				new String[]{"subject", "body"},
				new StandardAnalyzer()
		);
		Query query = queryParser.parse(queryString);
		IndexReader indexReader = DirectoryReader.open(FSDirectory.open(indexDir));
		IndexSearcher searcher = new IndexSearcher(indexReader);
		TopDocs docs = searcher.search(query, page * size, Sort.RELEVANCE, false);
		ScoreDoc[] hits = docs.scoreDocs;
		List<String> resultIds = new ArrayList<>();
		for (int i = (page - 1) * size; i < hits.length; i++) {
			resultIds.add(searcher.doc(hits[i].doc).get("id"));
		}
		indexReader.close();
		return new SearchResult(resultIds, docs.totalHits.value, page, size);
	}
}

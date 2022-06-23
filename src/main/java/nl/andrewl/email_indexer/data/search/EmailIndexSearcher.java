package nl.andrewl.email_indexer.data.search;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.util.Async;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Component that provides methods for searching for emails using Lucene search
 * indexes.
 */
public class EmailIndexSearcher {
	/**
	 * Searches the dataset asynchronously using the given query, and returns
	 * an ordered list of root email ids for threads containing relevant emails.
	 * @param dataset The dataset to search.
	 * @param queryString The query to use.
	 * @param maxResults The maximum amount of results to return.
	 * @return A future that completes when the search is done.
	 */
	public CompletableFuture<List<Long>> searchAsync(EmailDataset dataset, String queryString, int maxResults) {
		return Async.supply(() -> search(dataset, queryString, maxResults));
	}

	/**
	 * Searches the dataset using the given query, and returns an ordered list
	 * of root email ids for threads containing relevant emails.
	 * @param dataset The dataset to search.
	 * @param queryString The query to use.
	 * @param maxResults The maximum amount of results to return.
	 * @return A list of root email ids.
	 * @throws IOException If an error occurs while opening the indexes.
	 * @throws ParseException If the query is invalid.
	 */
	public List<Long> search(EmailDataset dataset, String queryString, int maxResults) throws IOException, ParseException {
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
				new String[]{"subject", "body"},
				new StandardAnalyzer()
		);
		Query query = queryParser.parse(queryString);
		List<Long> rootEmailIds = new ArrayList<>();
		try (var reader = DirectoryReader.open(FSDirectory.open(dataset.getIndexDir()))) {
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(query, Integer.MAX_VALUE, Sort.RELEVANCE, false);
			ScoreDoc[] hits = docs.scoreDocs;
			Set<Long> rootIds = new HashSet<>();
			for (ScoreDoc hit : hits) {
				Document doc = searcher.doc(hit.doc);
				IndexableField rootIdField = doc.getField("rootId");
				if (rootIdField != null && rootIdField.numericValue() != null) {
					long rootId = rootIdField.numericValue().longValue();
					if (!rootIds.contains(rootId)) {
						rootIds.add(rootId);
						rootEmailIds.add(rootId);
						if (rootEmailIds.size() == maxResults) break;
					}
				}
			}
		}
		return rootEmailIds;
	}

	/**
	 * Searches the dataset asynchronously using the given query, and returns
	 * an ordered list of email ids.
	 * @param dataset The dataset to search.
	 * @param queryString The query to use.
	 * @param maxResults The maximum amount of results to return.
	 * @return A future that completes when the search is done.
	 */
	public CompletableFuture<List<Long>> searchEmailsAsync(EmailDataset dataset, String queryString, int maxResults) {
		return Async.supply(() -> searchEmails(dataset, queryString, maxResults));
	}

	/**
	 * Searches the dataset using the given query, and returns an ordered list
	 * of email ids, without any regard to the thread in which the emails lie.
	 * @param dataset The dataset to search.
	 * @param queryString The query to use.
	 * @param maxResults The maximum amount of results to return.
	 * @return A list of email ids.
	 * @throws IOException If an error occurs while opening the indexes.
	 * @throws ParseException If the query is invalid.
	 */
	public List<Long> searchEmails(EmailDataset dataset, String queryString, int maxResults) throws IOException, ParseException {
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
				new String[]{"subject", "body"},
				new StandardAnalyzer()
		);
		Query query = queryParser.parse(queryString);
		List<Long> emailIds = new ArrayList<>();
		try (var reader = DirectoryReader.open(FSDirectory.open(dataset.getIndexDir()))) {
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(query, maxResults, Sort.RELEVANCE, false);
			for (ScoreDoc hit : docs.scoreDocs) {
				Document doc = searcher.doc(hit.doc);
				emailIds.add(doc.getField("id").numericValue().longValue());
			}
		}
		return emailIds;
	}
}

package nl.andrewl.email_indexer.data;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
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
import java.util.concurrent.ForkJoinPool;

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
	 * @return A future that completes when the search is done.
	 */
	public CompletableFuture<List<String>> searchAsync(EmailDataset dataset, String queryString) {
		CompletableFuture<List<String>> cf = new CompletableFuture<>();
		ForkJoinPool.commonPool().submit(() -> {
			try {
				cf.complete(search(dataset, queryString));
			} catch (IOException | ParseException e) {
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	/**
	 * Searches the dataset using the given query, and returns an ordered list
	 * of root email ids for threads containing relevant emails.
	 * @param dataset The dataset to search.
	 * @param queryString The query to use.
	 * @return A list of root email ids.
	 * @throws IOException If an error occurs while opening the indexes.
	 * @throws ParseException If the query is invalid.
	 */
	public List<String> search(EmailDataset dataset, String queryString) throws IOException, ParseException {
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
				new String[]{"subject", "body"},
				new StandardAnalyzer()
		);
		Query query = queryParser.parse(queryString);
		List<String> rootEmailIds = new ArrayList<>();
		try (var reader = DirectoryReader.open(FSDirectory.open(dataset.getIndexDir()))) {
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(query, Integer.MAX_VALUE, Sort.RELEVANCE, false);
			ScoreDoc[] hits = docs.scoreDocs;
			var repo = new EmailRepository(dataset);
			Set<String> rootIds = new HashSet<>();
			for (ScoreDoc hit : hits) {
				String messageId = searcher.doc(hit.doc).get("id");
				repo.findRootEmailByChildId(messageId).ifPresent(email -> {
					if (!email.hidden() && !rootIds.contains(email.messageId())) {
						rootEmailIds.add(email.messageId());
						rootIds.add(email.messageId());
					}
				});
			}
		}
		return rootEmailIds;
	}
}

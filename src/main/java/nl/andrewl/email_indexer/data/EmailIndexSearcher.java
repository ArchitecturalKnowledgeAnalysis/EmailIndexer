package nl.andrewl.email_indexer.data;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class EmailIndexSearcher {
	public CompletableFuture<List<EmailEntryPreview>> searchAsync(EmailDataset dataset, String queryString) {
		CompletableFuture<List<EmailEntryPreview>> cf = new CompletableFuture<>();
		ForkJoinPool.commonPool().submit(() -> {
			try {
				cf.complete(search(dataset, queryString));
			} catch (IOException | ParseException e) {
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	public List<EmailEntryPreview> search(EmailDataset dataset, String queryString) throws IOException, ParseException {
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
				new String[]{"subject", "body"},
				new StandardAnalyzer()
		);
		Query query = queryParser.parse(queryString);
		List<EmailEntryPreview> entries = new ArrayList<>();
		try (var reader = DirectoryReader.open(FSDirectory.open(dataset.getIndexDir()))) {
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(query, Integer.MAX_VALUE, Sort.RELEVANCE, false);
			ScoreDoc[] hits = docs.scoreDocs;
			var repo = new EmailRepository(dataset);
			Set<String> rootIds = new HashSet<>();
			for (ScoreDoc hit : hits) {
				String messageId = searcher.doc(hit.doc).get("id");
				repo.findEmailById(messageId).ifPresent(email -> {
					if (!email.hidden() && email.inReplyTo() == null && !rootIds.contains(email.messageId())) {
						EmailEntryPreview entry = new EmailEntryPreview(
								email.messageId(),
								email.subject(),
								email.sentFrom(),
								email.date(),
								email.tags(),
								false,
								new ArrayList<>()
						);
						repo.loadRepliesRecursive(entry);
						entries.add(entry);
						rootIds.add(email.messageId());
					}
				});
			}
		}
		return entries;
	}
}

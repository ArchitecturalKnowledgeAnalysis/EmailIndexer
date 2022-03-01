package nl.andrewl.email_indexer.gen;

import nl.andrewl.mbox_parser.Email;
import nl.andrewl.mbox_parser.EmailHandler;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * An email handler that adds emails to an Apache Lucene index.
 */
public class IndexingEmailHandler implements EmailHandler {
	private final IndexWriter emailIndexWriter;
	private final Set<String> emailIds;

	public IndexingEmailHandler(IndexWriter emailIndexWriter) {
		this.emailIndexWriter = emailIndexWriter;
		this.emailIds = new HashSet<>();
	}

	@Override
	public void emailReceived(Email email) {
		if (!email.mimeType.equalsIgnoreCase("text/plain")) {
			return;
		}
		if (emailIds.contains(email.messageId)) return;
		emailIds.add(email.messageId);
		Document doc = new Document();
		doc.add(new StringField("id", email.messageId, Field.Store.YES));
		doc.add(new StringField("subject", email.subject, Field.Store.NO));
		doc.add(new Field("body", email.readBodyAsText(), TextField.TYPE_NOT_STORED));
		try {
			emailIndexWriter.addDocument(doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

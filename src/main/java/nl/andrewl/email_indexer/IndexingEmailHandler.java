package nl.andrewl.email_indexer;

import nl.andrewl.mbox_parser.Email;
import nl.andrewl.mbox_parser.EmailHandler;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;

public class IndexingEmailHandler implements EmailHandler {
	private final IndexWriter emailIndexWriter;

	public IndexingEmailHandler(IndexWriter emailIndexWriter) {
		this.emailIndexWriter = emailIndexWriter;
	}

	@Override
	public void emailReceived(Email email) {
		Document doc = new Document();
		doc.add(new StoredField("id", email.messageId));
		doc.add(new StoredField("sentFrom", email.sentFrom));
		doc.add(new StoredField("subject", email.subject));
		doc.add(new StoredField("body", email.body));
		try {
			emailIndexWriter.addDocument(doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

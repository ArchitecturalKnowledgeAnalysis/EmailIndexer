package nl.andrewl.email_indexer.data.export.datasample.datatype;

import nl.andrewl.email_indexer.data.*;
import nl.andrewl.email_indexer.data.export.ExporterParameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exports query results to a single file, or multiple files in a directory.
 */
public final class TxtExporter implements TypeExporter {
	private static final String MAIN_OUTPUT_FILE = "output.txt";

	private ExporterParameters params;

	/**
	 * A print writer for the main text file in the export.
	 */
	private PrintWriter printWriter;

	/**
	 * The directory to generate the export in.
	 */
	private Path outputDir;

	@Override
	public void beforeExport(EmailDataset ds, Path path, ExporterParameters params) throws IOException {
		this.params = params;
		outputDir = path;
		if (this.params.mailingThreadsAreSeparate() && !Files.exists(outputDir)) {
			Files.createDirectories(outputDir);
		}
		Path pwPath = this.params.mailingThreadsAreSeparate()
				? outputDir.resolve(MAIN_OUTPUT_FILE)
				: outputDir;
		printWriter = new PrintWriter(Files.newBufferedWriter(pwPath), false);
		writeMetadata(ds);
	}

	@Override
	public void exportEmail(EmailEntry email, int rank, EmailRepository emailRepo, TagRepository tagRepo)
			throws IOException {
		if (params.mailingThreadsAreSeparate()) {
			writeThreadInSeparateDocument(email, rank, emailRepo, tagRepo);
		} else {
			writeThreadInDocument(email, emailRepo, tagRepo, printWriter, 0);
		}
	}

	@Override
	public void afterExport() {
		printWriter.close();
	}

	private void writeMetadata(EmailDataset ds) {
		printWriter.println("""
                Query: %s
                Exported at: %s
                Tags: %s,
                Total emails: %d""".formatted(
				params.getQuery(),
				ZonedDateTime.now().toString(),
				new TagRepository(ds).findAll().stream().map(Tag::name).collect(Collectors.joining(", ")),
				params.getMaxResultCount()));
		printWriter.println();
	}

	/**
	 * Creates a new document and writes the mailing thread in it.
	 */
	private void writeThreadInSeparateDocument(EmailEntry email, int rank, EmailRepository emailRepo,
											   TagRepository tagRepo) throws IOException {
		try (PrintWriter p = new PrintWriter(Files.newBufferedWriter(outputDir.resolve("emailthread-" + rank + ".txt")),
				false)) {
			writeThreadInDocument(email, emailRepo, tagRepo, p, 0);
		}
	}

	/**
	 * Writes all information of a mailing thread into a plain-text document.
	 */
	private void writeThreadInDocument(EmailEntry email, EmailRepository emailRepo, TagRepository tagRepo,
									   PrintWriter p, int indentLevel) {
		String indent = "\t".repeat(indentLevel);
		p.println(indent + "Message id: " + email.messageId());
		p.println(indent + "Subject: " + email.subject());
		p.println(indent + "Sent from: " + email.sentFrom());
		p.println(indent + "Date: " + email.date());
		p.println(indent + "Tags: "
				+ tagRepo.getTags(email.id()).stream().map(Tag::name).collect(Collectors.joining(", ")));
		p.println(indent + "Hidden: " + email.hidden());
		p.println(indent + "Body---->>>");
		email.body().trim().lines().forEachOrdered(line -> p.println(indent + line));
		if (!params.repliesAreExported()){
			return;
		}
		p.println(indent + "-------->>>");
		List<EmailEntryPreview> replies = emailRepo.findAllReplies(email.id());
		if (!replies.isEmpty()) {
			p.println("Replies:");
			for (int i = 0; i < replies.size(); i++) {
				var reply = replies.get(i);
				EmailEntry replyFull = emailRepo.findEmailById(reply.id()).orElseThrow();
				p.println("\t" + indent + "Reply #" + (i + 1));
				writeThreadInDocument(replyFull, emailRepo, tagRepo, p, indentLevel + 1);
				p.println();
			}
		}
		p.println();
	}
}

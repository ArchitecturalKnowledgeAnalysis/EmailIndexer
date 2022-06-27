package nl.andrewl.email_indexer.data.export.datasample.datatype;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import nl.andrewl.email_indexer.data.*;
import nl.andrewl.email_indexer.data.export.ExporterParameters;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes all information of a mailing thread into a single PDF, or multiple
 * PDFs in a directory.
 */
public final class PdfExporter implements TypeExporter {
	public static final String MAIN_OUTPUT_FILE = "output.pdf";

	public static Font HEADER_TEXT = FontFactory.getFont(FontFactory.COURIER, FontFactory.defaultEncoding,
			BaseFont.EMBEDDED, 16, Font.BOLD, BaseColor.BLACK);
	public static Font SUBHEADER_TEXT = FontFactory.getFont(FontFactory.COURIER, FontFactory.defaultEncoding,
			BaseFont.EMBEDDED, 12, Font.BOLD, BaseColor.BLACK);
	public static Font REGULAR_TEXT = FontFactory.getFont(FontFactory.COURIER, FontFactory.defaultEncoding,
			BaseFont.EMBEDDED, 11, Font.NORMAL, BaseColor.BLACK);

	private Document mainDocument;
	private Path outputDir;
	private EmailRepository emailRepo;
	private TagRepository tagRepo;

	private ExporterParameters params;

	@Override
	public void beforeExport(EmailDataset ds, Path path, ExporterParameters params) throws IOException {
		this.params = params;
		outputDir = path;
		if (this.params.mailingThreadsAreSeparate() && !Files.exists(outputDir)) {
			Files.createDirectories(outputDir);
		}
		emailRepo = new EmailRepository(ds);
		tagRepo = new TagRepository(ds);
		try {
			mainDocument = this.params.mailingThreadsAreSeparate()
					? makeMetaFile(ds, path.resolve(MAIN_OUTPUT_FILE))
					: makeMetaFile(ds, path);
		} catch (DocumentException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void exportEmail(EmailEntry email, int rank, EmailRepository emailRepo, TagRepository tagRepo)
			throws IOException {
		try {
			if (params.mailingThreadsAreSeparate()) {
				writeThreadInSeparateDocument(email, Integer.toString(rank));
			} else {
				writeThreadInDocument(mainDocument, email, Integer.toString(rank));
			}
		} catch (DocumentException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void afterExport() {
		mainDocument.close();
	}

	private Document makeMetaFile(EmailDataset ds, Path file) throws DocumentException, IOException {
		Document document = new Document();
		PdfWriter.getInstance(document, new FileOutputStream(file.toString()));
		document.open();
		addText("Export Meta Data", document, HEADER_TEXT);
		addText("Query:", document, SUBHEADER_TEXT);
		addText(params.getQuery() + "\n\n", document, REGULAR_TEXT);
		addText("Exported at:", document, SUBHEADER_TEXT);
		addText(ZonedDateTime.now() + "\n\n", document, REGULAR_TEXT);
		addText("Tags:", document, SUBHEADER_TEXT);
		String tags = new TagRepository(ds).findAll().stream().map(Tag::name).collect(Collectors.joining(", "));
		addText(tags, document, REGULAR_TEXT);
		addText("Total emails:", document, SUBHEADER_TEXT);
		addText(params.getMaxResultCount() + "\n\n", document, REGULAR_TEXT);
		document.newPage();
		return document;
	}

	private void writeThreadInSeparateDocument(EmailEntry email, String index) throws DocumentException, IOException {
		Document document = new Document();
		Path targetPath = outputDir.resolve("emailthread-" + index + ".pdf");
		PdfWriter.getInstance(document, new FileOutputStream(targetPath.toString()));
		document.open();
		writeThreadInDocument(document, email, index);
		document.close();
	}

	private void writeThreadInDocument(Document document, EmailEntry email, String index) throws DocumentException {
		addText("Email " + index, document, HEADER_TEXT);
		addText("Subject:", document, SUBHEADER_TEXT);
		addText(email.subject() + "\n\n", document, REGULAR_TEXT);
		addText("Sent from:", document, SUBHEADER_TEXT);
		addText(email.sentFrom() + "\n\n", document, REGULAR_TEXT);
		addText("Date:", document, SUBHEADER_TEXT);
		addText(email.date() + "\n\n", document, REGULAR_TEXT);
		addText("Tags:", document, SUBHEADER_TEXT);
		String tags = tagRepo.getTags(email.id()).stream().map(Tag::name).collect(Collectors.joining(", "));
		addText(tags + "\n\n", document, REGULAR_TEXT);
		addText("Reply Count:", document, SUBHEADER_TEXT);
		List<EmailEntryPreview> replies = emailRepo.findAllReplies(email.id());
		addText(replies.size() + "\n\n", document, REGULAR_TEXT);
		addText("Body:\n\n", document, SUBHEADER_TEXT);
		addText(email.body(), document, REGULAR_TEXT);
        if (!params.repliesAreExported()){
			return;
		}
		document.newPage();
		for (int i = 0; i < replies.size(); i++) {
			EmailEntryPreview reply = replies.get(i);
			int replyId = i + 1;
			EmailEntry replyFull = emailRepo.findEmailById(reply.id()).orElseThrow();
			writeThreadInDocument(document, replyFull, index + "." + replyId);
		}
	}

	private void addText(String text, Document document, Font font) throws DocumentException {
		Chunk chunk = new Chunk(text, font);
		Paragraph paragraph = new Paragraph(chunk);
		document.add(paragraph);
	}
}

package nl.andrewl.email_indexer.data.export.query;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import nl.andrewl.email_indexer.data.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes all information of a mailing thread into one or more PDF files inside
 * a specific directory.
 */
public final class PdfQueryExporter extends QueryExporter {
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

    public PdfQueryExporter(QueryExportParams params) {
        super(params);
    }

    @Override
    protected void beforeExport(EmailDataset ds, Path path) throws IOException {
        outputDir = path;
        if (this.params.isSeparateEmailThreads() && !Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        emailRepo = new EmailRepository(ds);
        tagRepo = new TagRepository(ds);
        try {
            mainDocument = this.params.isSeparateEmailThreads()
                    ? makeMetaFile(ds, path.resolve(MAIN_OUTPUT_FILE))
                    : makeMetaFile(ds, path);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void exportEmail(EmailEntry email, int rank, EmailRepository emailRepo, TagRepository tagRepo)
            throws IOException {
        try {
            if (params.isSeparateEmailThreads()) {
                writeThreadInSeparateDocument(email, Integer.toString(rank));
            } else {
                writeThreadInDocument(mainDocument, email, Integer.toString(rank));
            }
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void afterExport() {
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
        addText(params.getResultCount() + "\n\n", document, REGULAR_TEXT);
        document.newPage();
        return document;
    }

    private void writeThreadInSeparateDocument(EmailEntry email, String index) throws DocumentException, IOException {
        Document document = new Document();
        Path targetPath = outputDir.resolve("email-" + index + ".pdf");
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
        String tags = tagRepo.findAll().stream().map(Tag::name).collect(Collectors.joining(", "));
        addText(tags + "\n\n", document, REGULAR_TEXT);
        addText("Reply Count:", document, SUBHEADER_TEXT);
        List<EmailEntryPreview> replies = emailRepo.findAllReplies(email.id());
        addText(replies.size() + "\n\n", document, REGULAR_TEXT);
        addText("Body:\n\n", document, SUBHEADER_TEXT);
        addText(email.body(), document, REGULAR_TEXT);
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

package nl.andrewl.email_indexer.data.export.query;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.Tag;
import nl.andrewl.email_indexer.data.export.ExportException;
import nl.andrewl.email_indexer.data.export.ExporterParameters;

/**
 * Writes all information of a mailing thread into a PDF document.
 */
public final class PdfQueryExporter extends QueryExporter {
    public static Font HEADER_TEXT = FontFactory.getFont(FontFactory.COURIER, FontFactory.defaultEncoding,
            BaseFont.EMBEDDED, 16, Font.BOLD, BaseColor.BLACK);
    public static Font SUBHEADER_TEXT = FontFactory.getFont(FontFactory.COURIER, FontFactory.defaultEncoding,
            BaseFont.EMBEDDED, 12, Font.BOLD, BaseColor.BLACK);
    public static Font REGULAR_TEXT = FontFactory.getFont(FontFactory.COURIER, FontFactory.defaultEncoding,
            BaseFont.EMBEDDED, 11, Font.NORMAL, BaseColor.BLACK);

    @Override
    public CompletableFuture<Void> doQueryExport(ExporterParameters exportParams) {
        if (exportParams.getEmails() == null) {
            throw new ExportException(
                    "Emails parameter cannot be null. In that case, use TypeAwareQueryExporter instead.");
        }
        Document metaDocument = makeMetaFile(exportParams);
        List<EmailEntryPreview> emails = exportParams.getEmails();
        Path targetDirectory = exportParams.getOutputPath();
        for (int i = 0; i < emails.size(); i++) {
            EmailEntryPreview email = emails.get(i);
            String replyId = "#" + (i + 1);
            if (exportParams.mailingThreadsAreSeparate()) {
                writeThreadInSeparateDocument(targetDirectory, exportParams, email, replyId);
            } else {
                writeThreadInDocument(metaDocument, exportParams, email, replyId);
            }
        }
        metaDocument.close();
        return null;
    }

    private Document makeMetaFile(ExporterParameters exportParams) {
        Document document = new Document();
        Path targetPath = Path.of(exportParams.getOutputPath().toString() + "/output.pdf");
        createPdfInstance(document, targetPath);
        document.open();
        addText("Export Meta Data", document, HEADER_TEXT);
        addText("Query:", document, SUBHEADER_TEXT);
        addText(exportParams.getQuery() + "\n\n", document, REGULAR_TEXT);
        addText("Exported at:", document, SUBHEADER_TEXT);
        addText(ZonedDateTime.now() + "\n\n", document, REGULAR_TEXT);
        addText("Tags:", document, SUBHEADER_TEXT);
        String tags = exportParams.getTagRepository().findAll().stream().map(Tag::name)
                .collect(Collectors.joining(", "));
        addText(tags, document, REGULAR_TEXT);
        addText("Total emails:", document, SUBHEADER_TEXT);
        addText(exportParams.getEmails().size() + "\n\n", document, REGULAR_TEXT);
        document.newPage();
        return document;
    }

    private void writeThreadInSeparateDocument(Path workingDir, ExporterParameters exportParams,
            EmailEntryPreview email,
            String mailIndex) {
        Document document = new Document();
        Path targetPath = Path.of(workingDir.toString() + "/email-" + mailIndex + ".pdf");
        createPdfInstance(document, targetPath);
        document.open();
        writeThreadInDocument(document, exportParams, email, mailIndex);
        document.close();
    }

    private void writeThreadInDocument(Document document, ExporterParameters exportParams, EmailEntryPreview email,
            String emailId) {
        EmailRepository repository = exportParams.getRepository();
        addText("Email " + emailId, document, HEADER_TEXT);
        addText("Subject:", document, SUBHEADER_TEXT);
        addText(email.subject() + "\n\n", document, REGULAR_TEXT);
        addText("Sent from:", document, SUBHEADER_TEXT);
        addText(email.sentFrom() + "\n\n", document, REGULAR_TEXT);
        addText("Date:", document, SUBHEADER_TEXT);
        addText(email.date() + "\n\n", document, REGULAR_TEXT);
        addText("Tags:", document, SUBHEADER_TEXT);
        String tags = exportParams.getTagRepository().getTags(email.id()).stream().map(Tag::name)
                .collect(Collectors.joining(", "));
        addText(tags + "\n\n", document, REGULAR_TEXT);
        addText("Reply Count:", document, SUBHEADER_TEXT);
        List<EmailEntryPreview> replies = repository.findAllReplies(email.id());
        addText(replies.size() + "\n\n", document, REGULAR_TEXT);
        addText("Body:\n\n", document, SUBHEADER_TEXT);
        repository.getBody(email.id()).ifPresent(body -> {
            addText(body, document, REGULAR_TEXT);
        });
        document.newPage();
        for (int i = 0; i < replies.size(); i++) {
            EmailEntryPreview reply = replies.get(i);
            int replyId = i + 1;
            writeThreadInDocument(document, exportParams, reply, emailId + "." + replyId);
        }
    }

    private void addText(String text, Document document, Font font) throws ExportException {
        Chunk chunk = new Chunk(text, font);
        Paragraph paragraph = new Paragraph(chunk);
        try {
            document.add(paragraph);
        } catch (DocumentException innerException) {
            throw new ExportException("Could not write do document.", innerException);
        }
    }

    private void createPdfInstance(Document document, Path targetPath) throws ExportException {
        try {
            PdfWriter.getInstance(document, new FileOutputStream(targetPath.toString()));
        } catch (DocumentException innerException) {
            throw new ExportException("Could not create PDF instance.", innerException);
        } catch (IOException innerException) {
            throw new ExportException("Could not create output stream.", innerException);
        }
    }
}

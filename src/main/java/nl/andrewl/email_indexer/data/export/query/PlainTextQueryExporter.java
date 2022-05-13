package nl.andrewl.email_indexer.data.export.query;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.Tag;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.export.ExportException;
import nl.andrewl.email_indexer.data.export.ExporterParameters;

/**
 * Exports query results to one or multiple plain-text files.
 */
public final class PlainTextQueryExporter extends QueryExporter {
    @Override
    public CompletableFuture<Void> doQueryExport(ExporterParameters exportParams) {
        if (exportParams.getEmails() == null) {
            throw new ExportException(
                    "Emails parameter cannot be null. In that case, use TypeAwareQueryExporter instead.");
        }
        Path outputPath = Path.of(exportParams.getOutputPath() + "/output.txt");
        try (PrintWriter p = new PrintWriter(new FileWriter(outputPath.toFile()), false)) {
            writeMetadata(exportParams, p);
            List<EmailEntryPreview> emails = exportParams.getEmails();
            for (int i = 0; i < emails.size(); i++) {
                EmailEntryPreview email = emails.get(i);
                if (exportParams.mailingThreadsAreSeparate()) {
                    writeThreadInSeparateDocument(exportParams, email, i);
                } else {
                    writeThreadInDocument(exportParams, email, p, 0);
                }
            }
        } catch (IOException innerException) {
            throw new ExportException("Could not export file.", innerException);
        }
        return null;
    }

    private void writeMetadata(ExporterParameters exportParams, PrintWriter p) {
        p.println("Query: " + exportParams.getQuery());
        p.println("Exported at: " + ZonedDateTime.now());
        p.println("Tags: "
                + exportParams.getTagRepository().findAll().stream().map(Tag::name).collect(Collectors.joining(", ")));
        p.println("Total emails: " + exportParams.getEmails().size());
        p.println("\n");
    }

    /**
     * Creates a new document and writes the mailing thread in it.
     */
    private void writeThreadInSeparateDocument(ExporterParameters exportParams, EmailEntryPreview email,
            int emailIndex)
            throws IOException {
        Path outputPath = Path.of(exportParams.getOutputPath() + "/email-" + emailIndex + ".txt");
        try (PrintWriter p = new PrintWriter(new FileWriter(outputPath.toFile()), false)) {
            writeThreadInDocument(exportParams, email, p, 0);
        }
    }

    /**
     * Writes all information of a mailing thread into a plain-text document.
     */
    private void writeThreadInDocument(ExporterParameters exportParams, EmailEntryPreview email, PrintWriter p,
            int indentLevel) {
        EmailRepository repo = exportParams.getRepository();
        TagRepository tagRepo = exportParams.getTagRepository();
        String indent = "\t".repeat(indentLevel);
        p.println(indent + "Message id: " + email.messageId());
        p.println(indent + "Subject: " + email.subject());
        p.println(indent + "Sent from: " + email.sentFrom());
        p.println(indent + "Date: " + email.date());
        p.println(indent + "Tags: "
                + tagRepo.getTags(email.id()).stream().map(Tag::name).collect(Collectors.joining(", ")));
        p.println(indent + "Hidden: " + email.hidden());
        repo.getBody(email.id()).ifPresent(body -> {
            p.println(indent + "Body---->>>");
            body.trim().lines().forEachOrdered(line -> p.println(indent + line));
            p.println(indent + "-------->>>");
        });
        List<EmailEntryPreview> replies = repo.findAllReplies(email.id());
        if (!replies.isEmpty()) {
            p.println("Replies:");
            for (int i = 0; i < replies.size(); i++) {
                var reply = replies.get(i);
                p.println("\t" + indent + "Reply #" + (i + 1));
                writeThreadInDocument(exportParams, reply, p, indentLevel + 1);
                p.println();
            }
        }
        p.println();
    }
}

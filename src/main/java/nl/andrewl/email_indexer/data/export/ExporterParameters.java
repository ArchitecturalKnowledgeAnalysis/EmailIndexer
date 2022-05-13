package nl.andrewl.email_indexer.data.export;

import java.nio.file.Path;
import java.util.List;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;

/**
 * Parameter object for the EmailDatasetExporter.
 */
public class ExporterParameters {
    private String query = "";
    private EmailRepository repository = null;
    private TagRepository tagRepository = null;
    private EmailDataset dataset = null;
    private List<EmailEntryPreview> emails = null;
    private Path outputPath = null;
    private int maxResultCount = 100;
    private String outputFileType = "txt";
    private boolean separateMailingThreads = false;

    public String getQuery() {
        return query;
    }

    public EmailRepository getRepository() {
        return repository;
    }

    public TagRepository getTagRepository() {
        return tagRepository;
    }

    public EmailDataset getDataset() {
        return dataset;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public List<EmailEntryPreview> getEmails() {
        return emails;
    }

    public int getMaxResultCount() {
        return maxResultCount;
    }

    public String getOutputFileType() {
        return outputFileType;
    }

    public boolean mailingThreadsAreSeparate() {
        return separateMailingThreads;
    }

    public ExporterParameters withQuery(String query) {
        this.query = query;
        return this;
    }

    public ExporterParameters withRepository(EmailRepository repository) {
        this.repository = repository;
        return this;
    }

    public ExporterParameters withTagRepository(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
        return this;
    }

    public ExporterParameters withDataset(EmailDataset dataset) {
        this.dataset = dataset;
        return this;
    }

    public ExporterParameters withEmails(List<EmailEntryPreview> emails) {
        this.emails = emails;
        return this;
    }

    public ExporterParameters withOutputPath(Path outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public ExporterParameters withMaxResultCount(int maxResultCount) {
        this.maxResultCount = maxResultCount;
        return this;
    }

    public ExporterParameters withOutputFileType(String fileType) {
        this.outputFileType = fileType;
        return this;
    }

    public ExporterParameters withSeparateMailingThreads(boolean separateMailingThreads) {
        this.separateMailingThreads = separateMailingThreads;
        return this;
    }
}

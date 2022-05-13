package nl.andrewl.email_indexer.data.export.query;

import java.nio.file.Path;
import java.util.List;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;

/**
 * Parameter object for the EmailDatasetExporter.
 */
public class QueryExporterParams {
    private String query = "";
    private EmailRepository repository = null;
    private TagRepository tagRepository = null;
    private EmailDataset dataset = null;
    private List<EmailEntryPreview> emails = null;
    private Path outputDirectory = null;
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

    public Path getOutputDirectory() {
        return outputDirectory;
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

    public QueryExporterParams withQuery(String query) {
        this.query = query;
        return this;
    }

    public QueryExporterParams withRepository(EmailRepository repository) {
        this.repository = repository;
        return this;
    }

    public QueryExporterParams withTagRepository(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
        return this;
    }

    public QueryExporterParams withDataset(EmailDataset dataset) {
        this.dataset = dataset;
        return this;
    }

    public QueryExporterParams withEmails(List<EmailEntryPreview> emails) {
        this.emails = emails;
        return this;
    }

    public QueryExporterParams withOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public QueryExporterParams withMaxResultCount(int maxResultCount) {
        this.maxResultCount = maxResultCount;
        return this;
    }

    public QueryExporterParams withOutputFileType(String fileType) {
        this.outputFileType = fileType;
        return this;
    }

    public QueryExporterParams withSeparateMailingThreads(boolean separateMailingThreads) {
        this.separateMailingThreads = separateMailingThreads;
        return this;
    }
}

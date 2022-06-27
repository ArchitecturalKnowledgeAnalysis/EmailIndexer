package nl.andrewl.email_indexer.data.export;

import java.nio.file.Path;
import java.util.List;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.SearchFilter;

/**
 * Parameter object for the EmailDatasetExporter.
 */
public class ExporterParameters {
    public enum OutputType {
        UNSET,
        DATASET_ZIP,
        QUERY_TXT,
        QUERY_PDF
    }

    private String query = "";
    private EmailRepository repository = null;
    private TagRepository tagRepository = null;
    private EmailDataset dataset = null;
    private List<EmailEntryPreview> emails = null;
    private Path outputPath = null;
    private List<SearchFilter> searchFilters = null;
    private int maxResultCount = 100;
    private OutputType outputType = OutputType.UNSET;
    private boolean separateMailingThreads = false;
    private boolean exportReplies = false;

    public String getQuery() {
        return query;
    }

    public ExporterParameters withQuery(String query) {
        this.query = query;
        return this;
    }

    public EmailRepository getRepository() {
        return repository;
    }

    public ExporterParameters withRepository(EmailRepository repository) {
        this.repository = repository;
        return this;
    }

    public TagRepository getTagRepository() {
        return tagRepository;
    }

    public ExporterParameters withTagRepository(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
        return this;
    }

    public EmailDataset getDataset() {
        return dataset;
    }

    public ExporterParameters withDataset(EmailDataset dataset) {
        this.dataset = dataset;
        return this;
    }

    public List<EmailEntryPreview> getEmails() {
        return emails;
    }

    public ExporterParameters withEmails(List<EmailEntryPreview> emails) {
        this.emails = emails;
        return this;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public ExporterParameters withOutputPath(Path outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public List<SearchFilter> getFilters() {
        return searchFilters;
    }

    public ExporterParameters withSearchFilters(List<SearchFilter> searchFilters) {
        this.searchFilters = searchFilters;
        return this;
    }

    public int getMaxResultCount() {
        return maxResultCount;
    }

    public ExporterParameters withMaxResultCount(int maxResultCount) {
        this.maxResultCount = maxResultCount;
        return this;
    }

    public OutputType getOutputType() {
        return outputType;
    }

    public ExporterParameters withOutputType(OutputType outputType) {
        this.outputType = outputType;
        return this;
    }

    public boolean mailingThreadsAreSeparate() {
        return separateMailingThreads;
    }

    public ExporterParameters withSeparateMailingThreads(boolean separateMailingThreads) {
        this.separateMailingThreads = separateMailingThreads;
        return this;
    }

    public boolean getExportReplies() {
        return exportReplies;
    }

    public ExporterParameters withExportReplies(boolean exportReplies) {
        this.exportReplies = exportReplies;
        return this;
    }
}

package nl.andrewl.email_indexer.data.export.query;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;

/**
 * Parameter object for the EmailDatasetExporter.
 */
public record QueryExporterParams(String query, EmailRepository repository, TagRepository tagRepository,
        EmailDataset dataset, Path directory, int maxResultCount, String fileType, boolean separateThreads,
        List<EmailEntryPreview> emails) {

    private final static String DEFAULT_FILETYPE = "txt";
    private final static boolean DEFAULT_SEPARATE_THREADS = false;

    /**
     * 
     * @param query           the exported query.
     * @param repository      the used repository.
     * @param tagRepository   the used tagrepository.
     * @param dataset         the used dataset.
     * @param directory       the output directory.
     * @param maxResultCount  the maximum number of exported email threads.
     * @param fileType        the targeted filetype (default is txt).
     * @param separateThreads whether threads are separated in different output
     *                        files (default is false).
     * @param emails          the to-be-exported emails (default is null)
     */
    public QueryExporterParams {
        Objects.requireNonNull(query);
        Objects.requireNonNull(repository);
        Objects.requireNonNull(tagRepository);
        Objects.requireNonNull(dataset);
        Objects.requireNonNull(directory);
        Objects.requireNonNull(maxResultCount);
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(separateThreads);
    }

    /**
     * Constructor with default filetype, separateThreads, and emails fields.
     */
    public QueryExporterParams(String query, EmailRepository repository, TagRepository tagRepository,
            EmailDataset dataset, Path directory, int maxResultCount) {
        this(query, repository, tagRepository, dataset, directory, maxResultCount, DEFAULT_FILETYPE,
                DEFAULT_SEPARATE_THREADS, null);
    }

    /**
     * Constructor with default separateThreads, and emails fields.
     */
    public QueryExporterParams(String query, EmailRepository repository, TagRepository tagRepository,
            EmailDataset dataset, Path directory, int maxResultCount, String fileType) {
        this(query, repository, tagRepository, dataset, directory, maxResultCount, fileType, DEFAULT_SEPARATE_THREADS,
                null);
    }

    /**
     * Constructor with all parameters except emails.
     */
    public QueryExporterParams(String query, EmailRepository repository, TagRepository tagRepository,
            EmailDataset dataset, Path directory, int maxResultCount, String fileType, boolean separateThreads) {
        this(query, repository, tagRepository, dataset, directory, maxResultCount, fileType, separateThreads,
                null);
    }

    /**
     * Constructor that copies all fields from the previous parameters and updates
     * the emails field.
     * 
     * @param params target object used for copying fields.
     * @param emails updated list of emails.
     */
    public QueryExporterParams(QueryExporterParams params, List<EmailEntryPreview> emails) {
        this(params.query, params.repository, params.tagRepository, params.dataset, params.directory,
                params.maxResultCount, params.fileType,
                params.separateThreads, emails);
    }
}

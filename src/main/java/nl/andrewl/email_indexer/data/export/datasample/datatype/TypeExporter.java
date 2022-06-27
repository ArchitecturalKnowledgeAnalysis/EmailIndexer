package nl.andrewl.email_indexer.data.export.datasample.datatype;

import java.nio.file.Path;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.export.ExporterParameters;

/**
 * Template interface for concrete data type exporters.
 */
public interface TypeExporter {
	/**
	 * Called before initiating the export; meant for export initialization.
	 *
	 * @param ds     The dataset to export.
	 * @param path   The path to export to.
	 * @param params The used export parameters.
	 * @throws Exception Concrete implementations can throw exceptions.
	 */
	void beforeExport(EmailDataset ds, Path path, ExporterParameters params) throws Exception;

	/**
	 * Called to export a single email.
	 *
	 * @param email     The email to export.
	 * @param rank      The email's rank.
	 * @param emailRepo The used email repository.
	 * @param tagRepo   The used tag repository.
	 * @throws Exception Concrete implementations can throw exceptions.
	 */
	void exportEmail(EmailEntry email, int rank, EmailRepository emailRepo, TagRepository tagRepo) throws Exception;

	/**
	 * Called after performing the complete export; meant for export finalization.
	 *
	 * @throws Exception Concrete implementations can throw exceptions.
	 */
	void afterExport() throws Exception;
}

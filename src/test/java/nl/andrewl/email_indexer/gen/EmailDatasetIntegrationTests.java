package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.export.query.PdfQueryExporter;
import nl.andrewl.email_indexer.data.export.query.PlainTextQueryExporter;
import nl.andrewl.email_indexer.data.export.query.QueryExportParams;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.email_indexer.util.DbUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test which runs through some common dataset workflows.
 */
public class EmailDatasetIntegrationTests {
	private static final Path TEST_DIR = Path.of("__test");

	@BeforeAll
	public static void beforeAll() {
		FileUtils.deleteRecursive(TEST_DIR.toString(), true);
	}

	@Test
	public void testGenerate() throws IOException, ParseException {
		EmailDataset ds = genDataset("__test_gen");
		var results = new EmailIndexSearcher().search(ds, "t*", 100);
		assertTrue(results.size() > 0);

		var optionalEmail = DbUtils.fetchOne(
				ds.getConnection(),
				"SELECT ID FROM EMAIL LIMIT 1",
				rs -> rs.getLong(1)
		);
		assertTrue(optionalEmail.isPresent());
		long id = optionalEmail.get();
		var tagRepo = new TagRepository(ds);
		tagRepo.addTag(id, "test");
		assertTrue(tagRepo.findAll().stream().anyMatch(tag -> tag.name().equals("test")));
		tagRepo.addTag(id, "test");

		ds.close().join();
	}

	@Test
	public void testExports() {
		EmailDataset ds = genDataset("__test_export");
		var params = new QueryExportParams()
				.withQuery("t* r* s* e*")
				.withMaxResultCount(10)
				.withSeparateEmailThreads(true);
		new PlainTextQueryExporter(params).export(ds, TEST_DIR.resolve("export-txt")).join();
		new PdfQueryExporter(params).export(ds, TEST_DIR.resolve("export-pdf")).join();
		ds.close().join();
	}

	private EmailDataset genDataset(String name) {
		var gen = new EmailDatasetGenerator();
		Path dsDir = TEST_DIR.resolve(name);
		gen.generate(Set.of(Path.of("test_emails")), dsDir).join();
		return EmailDataset.open(dsDir).join();
	}
}

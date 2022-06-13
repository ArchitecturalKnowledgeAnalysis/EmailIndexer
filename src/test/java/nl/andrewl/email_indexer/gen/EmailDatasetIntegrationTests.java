package nl.andrewl.email_indexer.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.Tag;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.export.ExporterParameters;
import nl.andrewl.email_indexer.data.export.datasample.datatype.CsvExporter;
import nl.andrewl.email_indexer.data.export.datasample.datatype.PdfExporter;
import nl.andrewl.email_indexer.data.export.datasample.datatype.TxtExporter;
import nl.andrewl.email_indexer.data.export.datasample.sampletype.FilterExporter;
import nl.andrewl.email_indexer.data.export.datasample.sampletype.QueryExporter;
import nl.andrewl.email_indexer.data.export.dataset.ZipExporter;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.email_indexer.data.search.SearchFilter;
import nl.andrewl.email_indexer.data.search.filter.HiddenFilter;
import nl.andrewl.email_indexer.data.search.filter.RootFilter;
import nl.andrewl.email_indexer.data.search.filter.TagFilter;
import nl.andrewl.email_indexer.util.DbUtils;

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
				rs -> rs.getLong(1));
		assertTrue(optionalEmail.isPresent());
		long id = optionalEmail.get();
		var tagRepo = new TagRepository(ds);
		tagRepo.addTag(id, "test");
		assertTrue(tagRepo.hasTag(id, "test"));
		assertTrue(tagRepo.findAll().stream().anyMatch(tag -> tag.name().equals("test")));
		tagRepo.addTag(id, "test");

		ds.close().join();
	}

	@Test
	public void testExportsQuerySeparated() {
		EmailDataset ds = genDataset("__test_export_separated");
		var params = new ExporterParameters()
				.withQuery("t* r* s* e*")
				.withMaxResultCount(10)
				.withSeparateMailingThreads(true);
		new QueryExporter(new TxtExporter(), params).export(ds, TEST_DIR.resolve("__test_query_export_separated_txt"))
				.join();
		new QueryExporter(new PdfExporter(), params).export(ds, TEST_DIR.resolve("__test_query_export_separated_pdf"))
				.join();
		ds.close().join();
	}

	@Test
	public void testExportsQueryMerged() {
		EmailDataset ds = genDataset("__test_export_merged");
		var params = new ExporterParameters()
				.withQuery("t* r* s* e*")
				.withMaxResultCount(50)
				.withSeparateMailingThreads(false);
		new QueryExporter(new TxtExporter(), params).export(ds, TEST_DIR.resolve("__test_query_export_txt.txt")).join();
		new QueryExporter(new PdfExporter(), params).export(ds, TEST_DIR.resolve("__test_query_export_pdf.pdf")).join();
		new QueryExporter(new CsvExporter(), params).export(ds, TEST_DIR.resolve("__test_query_export_csv.csv")).join();
		ds.close().join();
	}

	@Test
	public void testExportsFilterMerged() {
		EmailDataset ds = genDataset("__test_export_filter_merged");
		List<SearchFilter> filters = new ArrayList<>();
		filters.add(new HiddenFilter(false));
		filters.add(new RootFilter(false));
		filters.add(genTagFilter(ds));
		var params = new ExporterParameters()
				.withMaxResultCount(135)
				.withSeparateMailingThreads(false)
				.withSearchFilters(filters);
		new FilterExporter(new TxtExporter(), params).export(ds, TEST_DIR.resolve("__test_filtered_merged_txt.txt"))
				.join();
		new FilterExporter(new PdfExporter(), params).export(ds, TEST_DIR.resolve("__test_filtered_merged_pdf.pdf"))
				.join();
		new FilterExporter(new CsvExporter(), params).export(ds, TEST_DIR.resolve("__test_filter_export_csv.csv"))
				.join();
		ds.close().join();
	}

	@Test
	public void testExportsFilterSeparated() {
		EmailDataset ds = genDataset("__test_export_filter_separated");
		List<SearchFilter> filters = new ArrayList<>();
		filters.add(new HiddenFilter(false));
		filters.add(new RootFilter(false));
		filters.add(genTagFilter(ds));
		var params = new ExporterParameters()
				.withMaxResultCount(10)
				.withSeparateMailingThreads(true)
				.withSearchFilters(filters);
		new FilterExporter(new TxtExporter(), params)
				.export(ds, TEST_DIR.resolve("__test_filtered_export_separated_txt"))
				.join();
		new FilterExporter(new PdfExporter(), params)
				.export(ds, TEST_DIR.resolve("__test_filtered_export_separated_pdf"))
				.join();
		ds.close().join();
	}

	@Test
	public void testZipExporter() throws SQLException {
		EmailDataset ds = genDataset("__test_export_zip");
		Path zipFile = TEST_DIR.resolve("__test_export_zip.zip");
		long emailCount = new EmailRepository(ds).countEmails();
		new ZipExporter().export(ds, zipFile).join();
		assertFalse(ds.getConnection().isClosed(), "ZipExporter should reopen connection after the export.");
		ds.close().join();
		// Reopen and check that it's the same.
		ds = EmailDataset.open(zipFile).join();
		assertEquals(emailCount, new EmailRepository(ds).countEmails());
		ds.close().join();
	}

	/**
	 * Generates a dataset for testing. Includes a large set of emails from
	 * the Hadoop project, and a pseudorandom selection of tags applied to
	 * them.
	 * 
	 * @param name The name of the dataset directory.
	 * @return The dataset.
	 */
	private EmailDataset genDataset(String name) {
		var gen = new EmailDatasetGenerator();
		Path dsDir = TEST_DIR.resolve(name);
		gen.generate(Set.of(Path.of("test_emails")), dsDir).join();
		EmailDataset ds = EmailDataset.open(dsDir).join();

		TagRepository tagRepo = new TagRepository(ds);
		tagRepo.createTag("A", "Sample tag 1");
		tagRepo.createTag("B", "Sample tag 2");
		tagRepo.createTag("C", "Sample tag 3");
		List<Tag> tags = tagRepo.findAll();
		List<Long> emailIds = new ArrayList<>();
		try (var stmt = ds.getConnection().prepareStatement("SELECT ID FROM EMAIL")) {
			var rs = stmt.executeQuery();
			while (rs.next()) {
				emailIds.add(rs.getLong(1));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		Random rand = new Random(0);
		EmailRepository emailRepo = new EmailRepository(ds);
		long emailCount = emailRepo.countEmails();
		for (int i = 0; i < emailCount / 10; i++) {
			for (int j = 0; j < rand.nextInt(0, tags.size()); j++) {
				tagRepo.addTag(
						emailIds.get(rand.nextInt(0, emailIds.size())),
						tags.get(rand.nextInt(0, tags.size())).id());
			}
		}

		return ds;
	}

	/**
	 * Generates a TagFilter using the provided dataset.
	 * 
	 * @param ds used data set.
	 * @return tag filter
	 */
	private TagFilter genTagFilter(EmailDataset ds) {
		TagRepository repo = new TagRepository(ds);
		ArrayList<Integer> filteredTags = new ArrayList<>();
		filteredTags.add(repo.getTagByName("A").orElseThrow().id());
		filteredTags.add(repo.getTagByName("B").orElseThrow().id());
		return new TagFilter(filteredTags, TagFilter.Type.EXCLUDE_ANY);
	}
}

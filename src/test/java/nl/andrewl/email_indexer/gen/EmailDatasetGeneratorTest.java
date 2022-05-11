package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.email_indexer.util.Status;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test which runs through the process of generating a full dataset from
 * some emails.
 */
public class EmailDatasetGeneratorTest {
	private static final Set<Path> pathsToRemove = new HashSet<>();

	@Test
	public void testGenerate() throws IOException, ParseException {
		var status = new Status();
		status.withMessageConsumer(System.out::println);
		var generator = new EmailDatasetGenerator(status);
		Collection<Path> mboxPaths = List.of(Path.of("test_emails"));
		Path dsDir = Path.of("__test_dataset");
		pathsToRemove.add(dsDir);
		generator.generate(mboxPaths, dsDir).join();

		EmailDataset ds = EmailDataset.open(dsDir).join();
		var results = new EmailIndexSearcher().search(ds, "t*");
		assertTrue(results.size() > 0);
	}

	@AfterAll
	public static void cleanUp() {
		for (var p : pathsToRemove) {
			org.h2.store.fs.FileUtils.deleteRecursive(p.toString(), true);
		}
	}
}

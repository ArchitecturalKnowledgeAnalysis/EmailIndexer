package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import org.h2.store.fs.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class EmailDatasetGenerator {
	public void generate(Path mboxFilesDir, Path dsFile) throws Exception {
		Path tmpDir = Files.createTempDirectory("email-dataset-gen");
		System.out.println("Created temporary directory: " + tmpDir);
		try {
			DatabaseGenerator dbGen = new DatabaseGenerator(tmpDir.resolve("database"));
			EmailIndexGenerator indexGen = new EmailIndexGenerator();
			Path indexDir = tmpDir.resolve("index");
			Files.createDirectory(indexDir);
			System.out.println("Created index directory: " + indexDir);
			indexGen.generateIndex(mboxFilesDir, indexDir, dbGen);
			System.out.println("Generated indexes.");
			dbGen.close();
			EmailDataset.buildDatasetZip(tmpDir, dsFile);
			System.out.println("Built dataset ZIP file.");
		} finally {
			FileUtils.deleteRecursive(tmpDir.toAbsolutePath().toString(), true);
			System.out.println("Removed " + tmpDir);
		}
	}
}

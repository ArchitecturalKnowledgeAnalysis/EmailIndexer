package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import org.h2.store.fs.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class EmailDatasetGenerator {
	public void generate(Path mboxFilesDir, Path dsFile) throws Exception {
		Path tmpDir = Files.createTempDirectory("email-dataset-gen");
		try {
			DatabaseGenerator dbGen = new DatabaseGenerator(tmpDir.resolve("database"));
			EmailIndexGenerator indexGen = new EmailIndexGenerator();
			Path indexDir = tmpDir.resolve("index");
			Files.createDirectory(indexDir);
			indexGen.generateIndex(mboxFilesDir, indexDir, dbGen);
			dbGen.close();
			EmailDataset.buildDatasetZip(tmpDir, dsFile);
		} finally {
			FileUtils.deleteRecursive(tmpDir.toAbsolutePath().toString(), true);
		}
	}
}

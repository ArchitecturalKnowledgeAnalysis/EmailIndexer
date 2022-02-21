package nl.andrewl.email_indexer;

import java.io.IOException;
import java.nio.file.Path;

public class EmailIndexerProgram {
	public static void main(String[] args) throws IOException {
		Path inputDir = Path.of(args[0]);
		Path outputDir = Path.of(args[1]);
		new EmailIndexGenerator().generateIndex(inputDir, outputDir);
	}
}

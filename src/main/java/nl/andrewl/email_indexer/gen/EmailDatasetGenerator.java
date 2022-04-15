package nl.andrewl.email_indexer.gen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.mboxparser.MBoxParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Component that generates email datasets from a set of directories containing
 * mbox files.
 */
public class EmailDatasetGenerator {
	/**
	 * Generates a new Email Dataset using mbox files from a given directory,
	 * and places the resulting dataset in the given target location.
	 * @param mboxFileDirs The directories to read mbox files from.
	 * @param dsDir The directory to save the dataset at.
	 * @param messageConsumer A consumer to handle messages emitted during generation.
	 * @return A completion stage that completes when the dataset is created.
	 */
	public CompletionStage<Void> generate(Collection<Path> mboxFileDirs, Path dsDir, Consumer<String> messageConsumer) {
		CompletableFuture<Void> cf = new CompletableFuture<>();
		messageConsumer.accept("Starting dataset generation.");
		ForkJoinPool.commonPool().execute(() -> {
			try {
				Files.createDirectories(dsDir);
				messageConsumer.accept("Created dataset directory: " + dsDir);
				DatabaseGenerator dbGen = new DatabaseGenerator(dsDir.resolve("database"));
				messageConsumer.accept("Initialized embedded database.");
				MBoxParser parser = new MBoxParser(new SanitizingEmailHandler(dbGen));
				for (var dir : mboxFileDirs) {
					parseRecursive(dir, parser, messageConsumer);
				}
				dbGen.close();
				messageConsumer.accept("All emails added to the database.");

				EmailDataset dataset = new EmailDataset(dsDir);
				messageConsumer.accept("Generating index.");
				new EmailIndexGenerator().generateIndex(dataset, messageConsumer);
				messageConsumer.accept("Dataset generation complete.");
				cf.complete(null);
			} catch (Exception e) {
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	private void parseRecursive(Path dir, MBoxParser parser, Consumer<String> messageConsumer) throws IOException {
		messageConsumer.accept("Parsing directory: " + dir);
		try (var s = Files.list(dir)) {
			for (var p : s.toList()) {
				if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
					parseRecursive(p, parser, messageConsumer);
				} else if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(p)) {
					messageConsumer.accept("Parsing file: " + p);
					parser.parse(p);
				}
			}
		}
	}
}

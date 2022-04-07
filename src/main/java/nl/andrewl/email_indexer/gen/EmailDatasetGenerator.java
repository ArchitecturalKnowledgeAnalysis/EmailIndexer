package nl.andrewl.email_indexer.gen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

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
				EmailIndexGenerator indexGen = new EmailIndexGenerator();
				Path indexDir = dsDir.resolve("index");
				Files.createDirectory(indexDir);
				indexGen.generateIndex(mboxFileDirs, indexDir, messageConsumer, dbGen);
				dbGen.close();
				messageConsumer.accept("Dataset generation complete.");
				cf.complete(null);
			} catch (Exception e) {
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}
}

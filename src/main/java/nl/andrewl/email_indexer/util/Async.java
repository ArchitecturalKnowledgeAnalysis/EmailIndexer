package nl.andrewl.email_indexer.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Utility class for painless asynchronous operations.
 */
public final class Async {
	private Async() {}

	/**
	 * An unsafe runnable that might throw a checked exception.
	 */
	@FunctionalInterface
	public interface UnsafeRunnable {
		void run() throws Exception;
	}

	/**
	 * An unsafe supplier that might throw a checked exception.
	 * @param <T> The type that will be supplied.
	 */
	@FunctionalInterface
	public interface UnsafeSupplier<T> {
		T supply() throws Exception;
	}

	/**
	 * Runs the given runnable using {@link ForkJoinPool#commonPool()}.
	 * @param runnable The runnable.
	 * @return A future that completes when the runnable is done.
	 */
	public static CompletableFuture<Void> run(UnsafeRunnable runnable) {
		CompletableFuture<Void> cf = new CompletableFuture<>();
		ForkJoinPool.commonPool().submit(() -> {
			try {
				runnable.run();
				cf.complete(null);
			} catch (Exception e) {
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	/**
	 * Runs the given supplier using {@link ForkJoinPool#commonPool()}.
	 * @param supplier The supplier to run.
	 * @return A future that completes when the supplier is done.
	 * @param <T> The type that will be supplied.
	 */
	public static <T> CompletableFuture<T> supply(UnsafeSupplier<T> supplier) {
		CompletableFuture<T> cf = new CompletableFuture<>();
		ForkJoinPool.commonPool().submit(() -> {
			try {
				cf.complete(supplier.supply());
			} catch (Exception e) {
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	/**
	 * Convenience method for handling an unsafe future that might complete
	 * exceptionally, where exceptions are automatically handled by simply
	 * printing their stack trace.
	 * @param future The future to handle.
	 * @param successHandler A consumer that handles the value obtained when
	 *                       the future completes successfully.
	 * @return A void future that completes when the future is handled.
	 * @param <T> The type that the future returns.
	 */
	public static <T> CompletableFuture<Void> handle(CompletableFuture<T> future, Consumer<T> successHandler) {
		return handle(future, successHandler, Throwable::printStackTrace);
	}

	/**
	 * Wrapper method for handling an unsafe future that might complete
	 * exceptionally, where exceptions are automatically handled by a given
	 * handler.
	 * @param future The future to handle.
	 * @param successHandler A consumer to handle successful futures.
	 * @param exceptionHandler A consumer to handle exceptions.
	 * @return A void future that completes when the future is handled.
	 * @param <T> The type that the future returns.
	 */
	public static <T> CompletableFuture<Void> handle(CompletableFuture<T> future, Consumer<T> successHandler, Consumer<Throwable> exceptionHandler) {
		return future.handle((t, throwable) -> {
			if (throwable != null) {
				exceptionHandler.accept(throwable);
			} else {
				successHandler.accept(t);
			}
			return null;
		});
	}
}

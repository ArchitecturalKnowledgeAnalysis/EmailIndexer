package nl.andrewl.email_indexer.data.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public final class Async {
	private Async() {}

	@FunctionalInterface
	public interface UnsafeRunnable {
		void run() throws Exception;
	}

	@FunctionalInterface
	public interface UnsafeSupplier<T> {
		T supply() throws Exception;
	}

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

	public static <T> CompletableFuture<Void> handle(CompletableFuture<T> future, Consumer<T> successHandler) {
		return handle(future, successHandler, Throwable::printStackTrace);
	}

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

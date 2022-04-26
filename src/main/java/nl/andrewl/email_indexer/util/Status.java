package nl.andrewl.email_indexer.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A thread-safe object that can be used to track the status of an asynchronous
 * task, including messages emitted by that task, and possibly an indication of
 * the progress of the task.
 */
public class Status {
	private final Set<Consumer<String>> messageConsumers = new HashSet<>();
	private final Set<Consumer<Float>> progressConsumers = new HashSet<>();

	private Integer totalSteps;
	private int stepsDone = 0;

	public Status(Integer totalSteps) {
		this.totalSteps = totalSteps;
	}

	public Status() {
		this(null);
	}

	public Status withMessageConsumer(Consumer<String> consumer) {
		messageConsumers.add(consumer);
		return this;
	}

	public Status withProgressConsumer(Consumer<Float> consumer) {
		progressConsumers.add(consumer);
		return this;
	}

	public void setTotalSteps(int totalSteps) {
		this.totalSteps = totalSteps;
	}

	public synchronized void sendMessage(String message) {
		messageConsumers.forEach(c -> c.accept(message));
	}

	public synchronized void incrementStepsDone() {
		if (!supportsProgress()) {
			throw new UnsupportedOperationException("This status object does not support progress tracking.");
		}
		stepsDone++;
		progressConsumers.forEach(c -> c.accept(getProgress()));
	}

	public boolean supportsProgress() {
		return totalSteps != null;
	}

	public float getProgress() {
		if (!supportsProgress()) return 0;
		return (float) stepsDone / totalSteps;
	}

	public static Status noOp() {
		return new Status();
	}
}

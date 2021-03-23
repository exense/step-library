package ch.exense.step.examples.selenium.helper;

import java.util.function.Supplier;

/**
 * Poller class to check the valid status of a condition every 100ms, used to avoid the most frequent Selenium exceptions encountered
 */
public class Poller {
	/**
	 * Static method to retry the execution of a predicate over a timeout
	 * @param <T> the type of object returned by the predicate execution
	 * @param predicate the predicate to execute
	 * @param timeout the duration to retry to execute the predicate in second
	 * @return the object returned by the predicate execution
	 */
	public static <T> T retryIfFails(Supplier<T> predicate, long timeout) {
		long t1 = System.currentTimeMillis();
		Exception lastException = null;
		while (timeout == 0 || System.currentTimeMillis() < t1 + (timeout * 1000)) {
			try {
				T result = predicate.get();
				if (result != null) {
					return result;
				}
			} catch (Exception e) {
				lastException = e;
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		throw new RuntimeException("Timeout while waiting for condition to apply.", lastException);
	}
}

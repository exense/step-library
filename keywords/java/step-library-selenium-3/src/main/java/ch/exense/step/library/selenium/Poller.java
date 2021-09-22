/*******************************************************************************
 * Copyright 2021 exense GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package ch.exense.step.library.selenium;

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
		do {
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
		} while (System.currentTimeMillis() < t1 + (timeout * 1000));
		throw new RuntimeException("Timeout while waiting for condition to apply.", lastException);
	}

	public static void retryWhileFalse(Supplier<Boolean> condition, long timeout) {
		long t1 = System.currentTimeMillis();
		Exception lastException = null;
		do {
			try {
				if (condition.get()) {
					return;
				}
			} catch (Exception e) {
				lastException = e;
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} while (System.currentTimeMillis() < t1 + (timeout * 1000));
		throw new RuntimeException("Timeout while waiting for condition to apply.", lastException);
	}
}

package ch.exense.step.examples.selenium.helper;

import java.io.Closeable;
import java.io.IOException;

import org.openqa.selenium.WebDriver;

/**
 * Wrapper class for WebDriver instance. 
 * The class implements the Closeable interface in order to easily manage the browser instance closing.
 * @author rubieroj
 *
 */
public class DriverWrapper implements Closeable {
	/**
	 * The WebDriver instance to be wrapped.
	 */
	final WebDriver driver;

	/**
	 * Constructor for DriverWrapper
	 * @param driver the WebDriver instance to be wrapped
	 */
	public DriverWrapper(WebDriver driver) {
		super();
		this.driver = driver;
	}

	/**
	 * Method to automatically and properly close the wrapped WebDriver when not used anymore
	 */
	@Override
	public void close() throws IOException {
		driver.quit();
	}

	/**
	 * Getter to retrieve the wrapped driver
	 * @return the wrapped WebDriver instance
	 */
	public WebDriver getDriver() {
		return driver;
	}
}
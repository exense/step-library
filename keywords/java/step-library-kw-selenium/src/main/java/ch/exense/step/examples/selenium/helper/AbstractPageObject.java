package ch.exense.step.examples.selenium.helper;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Base class gathering utility methods to work on pages
 * @author rubieroj
 */
public class AbstractPageObject {
	/**
	 * The WebDriver used to created the PageObject instance
	 */
	protected WebDriver driver;
	/**
	 * Specific object to interact with JavaScript
	 */
	protected JSWaiter jsWaiter;
	/**
	 * Selenium object to wait on conditions
	 */
	protected WebDriverWait webDriverWait;
	
	/**
	 * Default timeout values on actions executed on PageObject
	 */
	protected static long DEFAULT_TIMEOUT = 30;

	/**
	 * Constructor for the PageObject
	 * @param driver the WebDriver instance to create the page with
	 */
	public AbstractPageObject(WebDriver driver) {
		this.driver = driver;
		this.jsWaiter = new JSWaiter(driver);
		this.webDriverWait = new WebDriverWait(driver, getDefaultTimeout());
	}

	/**
	 * Getter to return the WebDriver instance used by the PageObject
	 * @return the WebDriver used to create the PageObject
	 */
	public WebDriver getDriver() {
		return driver;
	}
	
	/**
	 * Setter for the WebDriver instance used by the PageObject
	 * @param driver the WebDriver instance to set
	 */
	public void setDriver(WebDriver driver) {
		this.driver = driver;
	}
	
	/**
	 * Getter to return the WebDriverWait instance used by the PageObject
	 * @return the WebDriverWait used to create the PageObject
	 */
	public WebDriverWait getWebDriverWait() {
		return this.webDriverWait;
	}
	
	/**
	 * Getter to return the JSWaiter instance used by the PageObject
	 * @return the JSWaiter used to create the PageObject
	 */
	public JSWaiter getJSWaiter() {
		return this.jsWaiter;
	}
	
	/**
	 * Method used to wait for an IFrame to be available. First switch to default page content.
	 * @param by the IFrame locator
	 */
	public void waitForFrame(By by) {
		this.driver.switchTo().defaultContent();
		this.webDriverWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(by));
	}
	
	/**
	 * Method used to switch the WebDriver content to an IFrame. First switch to default page content and wait for the IFrame to be available.
	 * @param by the IFrame locator
	 * @see #waitForFrame(By)
	 */
	public void waitForFrameAndSwitchDriver(By by) {
		waitForFrame(by);
		driver.switchTo().frame(findBy(by));
	}

	/**
	 * Perform a mouse hover on a web element
	 * @param by the web element locator
	 * @param timeout the maximal amount of time in milliseconds to wait when searching for the web element
	 */
	public void hover(By by, long timeout) {
		new Actions(driver).moveToElement(findBy(by, timeout)).build().perform();
	}
	
	/**
	 * Perform a mouse hover on a web element, using the default class timeout
	 * @param by the web element locator
	 * @see #hover(By, long)
	 */
	public void hover(By by) {
		hover(by, getDefaultTimeout());
	}
	
	/**
	 * Method to find a web element by locator 
	 * @param by the web element locator
	 * @param timeout the maximal amount of time in milliseconds to wait when searching for the web element 
	 * @return the web element
	 * @see #doWithoutImplicitWait(Callable)
	 * @see Poller#retryIfFails(Supplier, long)
	 */
	public WebElement findBy(By by, long timeout) {
		return doWithoutImplicitWait(()-> {
			return Poller.retryIfFails(new Supplier<WebElement>() {
				@Override
				public WebElement get() {
					return driver.findElement(by);
				}
			}, timeout);
		});
	}
	
	/**
	 * Method to find a web element by locator, using the default class timeout
	 * @param by the web element locator
	 * @return the web element
	 * @see #findBy(By, long)
	 */
	public WebElement findBy(By by) {
		return findBy(by, getDefaultTimeout());
	}

	/**
	 * Method to find a list of web elements by locator 
	 * @param by the web elements locator
	 * @param timeout the maximal amount of time in milliseconds to wait when searching for the web elements 
	 * @return a list of web elements
	 * @see #doWithoutImplicitWait(Callable)
	 * @see Poller#retryIfFails(Supplier, long)
	 */
	public List<WebElement> findAllBy(By by, long timeout) {
		return doWithoutImplicitWait(()-> {
			return Poller.retryIfFails(new Supplier<List<WebElement>>() {
				@Override
				public List<WebElement> get() {
					return driver.findElements(by);
				}
			}, timeout);
		});
	}
	
	/**
	 * Method to find a list of web elements by locator, using the default class timeout 
	 * @param by the web elements locator
	 * @return a list of web elements
	 * @see #findAllBy(By, long)
	 */
	public List<WebElement> findAllBy(By by) {
		return findAllBy(by, getDefaultTimeout());
	}
	
	/**
	 * Method to check the validity of a boolean condition
	 * @param condition the boolean condition to check
	 * @param timeout the maximal amount of time in milliseconds to wait when trying to check the condition validity
	 */
	public void safeWait(Supplier<Boolean> condition, long timeout) {
		Poller.retryIfFails(condition, timeout);
	}
	
	/**
	 * Method to check the validity of a boolean condition, using the default class timeout
	 * @param condition the boolean condition to check
	 * @see #safeWait(Supplier, long)
	 */
	public void safeWait(Supplier<Boolean> condition) {
		safeWait(condition, getDefaultTimeout());
	}
	
	/**
	 * Method to wait for the AJAX and JavaScript calls to be completed
	 * @param timeout the maximal amount of time to wait for the calls to be completed
	 * @see JSWaiter#waitAllRequest()
	 */
	public void safeWaitDocumentReadyState(long timeout) {
		safeWait(() -> {
			jsWaiter.waitAllRequest();
			return ((JavascriptExecutor) driver).executeScript("return document.readyState").toString().equals("complete");
		}, timeout);
	}
	
	/**
	 * Method to wait for the AJAX and JavaScript calls to be completed, using the default class timeout
	 * @see #safeWaitDocumentReadyState(long)
	 */
	public void safeWaitDocumentReadyState() {
		safeWaitDocumentReadyState(getDefaultTimeout());
	}
	
	/**
	 * Method used to click on a web element in a safe manner
	 * @param by the web element locator
	 * @param timeout the maximal amount of time to wait when trying to click on the web element
	 * @see Poller#retryIfFails(Supplier, long)
	 */
	public void safeClick(By by, long timeout) {
		Poller.retryIfFails(()-> {
			WebElement element = driver.findElement(by);
			element.click();
			return true;
		}, timeout);
	}
	
	/**
	 * Method used to click on a web element in a safe manner, using the default class timeout
	 * @param by the web element locator
	 * @see #safeClick(By, long)
	 */
	public void safeClick(By by) {
		safeClick(by, getDefaultTimeout());
	}
	
	public void executeJavascript(String javascriptToExecute) {
		((JavascriptExecutor) driver).executeScript(javascriptToExecute);
		
	}
	
	public void javascriptClick(WebElement element) {
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
	}
	
	public void javascriptClick(String xpath) {
		javascriptClick(findBy(By.xpath(xpath)));
	}
	
	/**
	 * Method used to fulfill an input web element in a safe manner. First clear the input web element content.
	 * @param element the element to send the keys to
	 * @param keys the value to insert
	 * @param timeout the maximal amount of time to wait when trying to send the keys to the web element
	 * @see AbstractPageObject#safeWait(Supplier, long)
	 */
	public void safeSendKeys(WebElement element, String keys, long timeout) {
		safeWait(() -> {
			element.clear();
			element.sendKeys(keys);
			return element.getText().equals(keys);
		}, timeout);
	}
	
	public void safeSendKeys(WebElement element, Keys keys) {
		Poller.retryIfFails(() -> {
			element.sendKeys(keys);
			return true;
		}, getDefaultTimeout());
	}
	
	/**
	 * Method used to fulfill an input web element in a safe manner, using the default class timeout
	 * @param element the element to send the keys to
	 * @param keys the value to insert
	 * @see #safeSendKeys(WebElement, String, long)
	 */
	public void safeSendKeys(WebElement element, String keys) {
		safeSendKeys(element, keys, getDefaultTimeout());
	}
	
	public void safeSendKeys(By by, String keys) {
		safeSendKeys(findBy(by), keys);
	}
	
	public void safeSendKeys(By by, Keys keys) {
		safeSendKeys(findBy(by), keys);
	}
	
	/**
	 * Method used to perform an action through a Callable instance bypassing the default class timeout
	 * @param <T> the type of object returned by the callable
	 * @param callable the Callable instance to be called
	 * @return the object returned by the Callable execution
	 * @see java.util.concurrent.Callable
	 */
	public <T>T doWithoutImplicitWait(Callable<T> callable) {
		driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
		try {
			return callable.call();
		} catch(Exception e) {
			throw new RuntimeException(e);
		} finally {
			driver.manage().timeouts().implicitlyWait(getDefaultTimeout(), TimeUnit.SECONDS);
		}
	}
	
	/**
	 * Method used to perform an action through a Runnable instance bypassing the default class timeout
	 * @param runnable the runnable to execute
	 * @see java.lang.Runnable
	 */
	public void doWithoutImplicitWait(Runnable runnable) {
		driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
		try {
			runnable.run();
		} catch(Exception e) {
			throw new RuntimeException(e);
		} finally {
			driver.manage().timeouts().implicitlyWait(getDefaultTimeout(), TimeUnit.SECONDS);
		}
	}

	public static long getDefaultTimeout() {
		return DEFAULT_TIMEOUT;
	}

	/**
	 * Generic method to hover on an element on a page, then wait for the page to be loaded. Used for instance to expand a menu
	 * @param hoverElementXPath the xpath to the element menu to click on
	 * @param xpathToCheck the xpath to check once the DOM is ready
	 * @see AbstractPageObject#hover(By)
	 * @see AbstractPageObject#safeWaitDocumentReadyState()
	 */
	public void hoverElement(String hoverElementXPath, String xpathToCheck) {
		hover(By.xpath(hoverElementXPath));
		safeWaitDocumentReadyState();
		safeWait(() -> {
			return findBy(By.xpath(xpathToCheck)).isEnabled();
		});
	}

	/**
	 * Method to exit a iframe
	 * @see AbstractPageObject#waitForFrameAndSwitchDriver(By)
	 */
	public void switchToDefaultContent() {
		driver.switchTo().defaultContent();
	}

	public void switchToWindow(String handle) {
		driver.switchTo().window(handle);
	}
}

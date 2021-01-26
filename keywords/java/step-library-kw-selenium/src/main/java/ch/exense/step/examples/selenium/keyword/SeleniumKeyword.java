package ch.exense.step.examples.selenium.keyword;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ch.exense.step.examples.selenium.helper.AbstractPageObject;
import ch.exense.step.examples.selenium.helper.DriverWrapper;
import ch.exense.step.examples.selenium.helper.ProxyWrapper;
import ch.exense.step.examples.selenium.helper.TransactionalKeyword;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.proxy.CaptureType;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.Keyword;

/**
 * Central class containing the STEP Selenium Keywords and helper methods used to start / stop a Chrome instance via chromedriver.
 * Please have a look at the <a href="https://step.exense.ch/knowledgebase/3.12/userdocs/keywords/">Exense documentation</a> to learn how to use Keywords.
 * @author rubieroj
 */
public class SeleniumKeyword extends TransactionalKeyword {
	final String chromeDriverProperty = "webdriver.chrome.driver";
	/*Options used for local testing with chrome devtools
	 * final List<String> defaultOptions = Arrays.asList("auto-open-devtools-for-tabs""disable-infobars","ignore-certificate-errors", 
			"no-zygote", "disable-extensions", "auto-open-devtools-for-tabs");*/
	final List<String> defaultOptions = Arrays.asList("disable-infobars","ignore-certificate-errors", "no-zygote", "disable-extensions");
	final List<String> headlessOptions = Arrays.asList("headless", "disable-gpu", "disable-software-rasterizer", "no-sandbox");
	
	/**
	 * <p>Method used for lazy initialization of a page object in a generic way</p>
	 * @param poClass the class of the page object to retrieve and instantiate if required
	 * @param driver to be associated with the page object
	 * @return the page object
	 */
	protected <T extends AbstractPageObject> T getPageObject(Class<T> poClass)  {
		T po = session.get(poClass);
		if (po == null) {
			try {
				po = poClass.getDeclaredConstructor(WebDriver.class).newInstance(getDriver());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException("Unable to instantiate the page object",e);
			}
			session.put(po);
		}
		return po; 
	}
	
	/**
	 * <p>Keyword used to configure and enable HTTP and HTTPS proxy on the STEP agent (not on chrome, affect the whole Agent JVM)</p>
	 * Inputs (default):
	 * <ul>
	 * <li>proxyHost: define Agent proxy host
	 * <li>proxyPort: define Agent proxy port
	 * <li>nonProxyHosts (*.exense.ch|grid): define the Agent nonProxy host</li>
	 * </ul>
	 * @deprecated proxy is now directly set in Open_chrome keyword
	 * @see #Open_chrome()
	 */
	/*Now removed to avoid any confutions
	@Deprecated
	@Keyword
	public void Enable_proxy() {
		System.setProperty("http.proxyHost", input.getString("proxyHost");
	    System.setProperty("http.proxyPort", String.valueOf(input.getInt("proxyPort")));
	    System.setProperty("http.nonProxyHosts", input.getString("nonProxyHosts","*.exense.ch|grid"));
	    System.setProperty("https.proxyHost", input.getString("proxyHost");
	    System.setProperty("https.proxyPort", String.valueOf(input.getInt("proxyPort")));
	    System.setProperty("https.nonProxyHosts", input.getString("nonProxyHosts","*.exense.ch|grid"));
	    System.setProperty("java.net.preferIPv4Stack", "true");
	}*/
		
	/**
	 * <p>Keyword used to create a selenium chrome driver and start a corresponding chrome instance. The driver is stored in the current STEP session and is automatically closed once the session ends.</p>
	 * Required properties:
	 * <ul>
	 * 	<li>chromedriver: path to the chrome driver (usually set in the agent properties)</li>
	 * </ul>
	 * Inputs (default values):
	 * <ul>
	 * <li>headless (false): boolean toggle for the headless mode (headless required on server/kubernetes)
	 * <li>disableShm (false): boolean toggle for Shm partition usage (disable on unix/kubernetes)
	 * <li>proxyHost: define chrome proxy host if set
	 * <li>proxyPort: define chrome proxy port if set
	 * <li>enableHarCapture (false): boolean to enable the capture of HTTP requests and create custom measurements
	 * <li>browserProxyPort (0): set the browser proxy port to be used (0 means automatically selected by the JVM/system) 
	 * <li>readBytesPerSecond: if set, limit the rate of read bytes by second
	 * <li>writeBytesPerSecond: if set, limit the rate of write bytes by second
	 * <li>user-data-dir: if set, define a specific chrome data folder to be used (by default a new temporary folder is used when starting chrome)
	 * <li>additionalOptions: if set, this list of options is added to ChromeOptions
	 * <li>implicitlyWait (10): timeout in seconds to load a page
	 * <li>pageLoadTimeout (10): timeout in seconds when waiting for a DOM element
	 * <li>maximize (false): toggle to maximum the chrome windows
	 * </ul>
	 */
	@Keyword (schema = "{ \"properties\": { "
			+ "\"headless\": {  \"type\": \"boolean\"},"
			+ "\"disableShm\": {\"type\": \"boolean\"},"
			+ "\"proxyHost\": {\"type\": \"string\"},"
			+ "\"proxyPort\": {\"type\": \"integer\"},"
			+ "\"enableHarCapture\": {\"type\": \"boolean\"},"
			+ "\"browserProxyPort\": {\"type\": \"integer\"},"
			+ "\"readBytesPerSecond\": {\"type\": \"integer\"},"
			+ "\"writeBytesPerSecond\": {\"type\": \"integer\"},"
			+ "\"user-data-dir\": {  \"type\": \"string\"},"
			+ "\"additionalOptions\": {  \"type\": \"string\"},"
			+ "\"implicitlyWait\": {  \"type\": \"integer\"},"
			+ "\"pageLoadTimeout\": {  \"type\": \"integer\"},"
			+ "\"maximize\": {  \"type\": \"boolean\"}"
			+ "}, \"required\" : []}", properties = { "" })
	public void Open_chrome() {
		if (properties.containsKey("chromedriver")) {
			File chromeDriverBin = new File(properties.get("chromedriver"));
			if (chromeDriverBin.exists()) {
				System.setProperty(chromeDriverProperty, chromeDriverBin.getAbsolutePath());
			}
		}

		boolean enableHarCapture = input.getBoolean("enableHarCapture", false);
		session.put("enableHarCapture", enableHarCapture);


		ChromeOptions options = new ChromeOptions();
		options.setAcceptInsecureCerts(true);
		options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);

		options.addArguments(defaultOptions);
		options.setExperimentalOption("w3c", false);

		if (input.getBoolean("headless", false)) {
			options.addArguments(headlessOptions);
		}

		if (input.getBoolean("disableShm",false)) {
			options.addArguments("disable-dev-shm-usage");
		}


		long readBytesPerSecond = input.getInt("readBytesPerSecond", 0);
		long writeBytesPerSecond = input.getInt("writeBytesPerSecond", 0);

		if(enableHarCapture || readBytesPerSecond > 0 || writeBytesPerSecond > 0) {
			BrowserMobProxy browserProxy = new BrowserMobProxyServer();
			browserProxy.setTrustAllServers(true);
			if (input.containsKey("proxyHost") && input.containsKey("proxyPort")) {
				browserProxy.setChainedProxy(
					new InetSocketAddress(
							input.getString("proxyHost"), input.getInt("proxyPort")));
				if(input.containsKey("noProxy")) {
					String noProxy = input.getString("noProxy").replaceAll(",", "|");
					System.setProperty("http.nonProxyHosts", noProxy);
					System.setProperty("https.nonProxyHosts", noProxy);
				}
			}

			if (readBytesPerSecond > 0) {
				browserProxy.setReadBandwidthLimit(readBytesPerSecond);
				output.add("readBytesPerSecond", readBytesPerSecond);
			}
			if (writeBytesPerSecond > 0) {
				browserProxy.setWriteBandwidthLimit(writeBytesPerSecond);
				output.add("writeBytesPerSecond", writeBytesPerSecond);
			}
			if (!enableHarCapture) {
				browserProxy.disableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
			}

			/*browserProxy.addResponseFilter(new ResponseFilter() {
				@Override
				public void filterResponse(HttpResponse response, HttpMessageContents contents, HttpMessageInfo messageInfo) {
					if(response.headers().containsValue("Content-Type", "text/css", true)) {
						return ;
					}

				}
			});*/
			//Could be used to skip certain request like google analytics
			/*if (input.getBoolean("disableGA",false)) {
				browserProxy.blacklistRequests("https://www.google.*", 404);
			}*/

			browserProxy.start(input.getInt("browserProxyPort", 0));

			Proxy seleniumProxy = ClientUtil.createSeleniumProxy(browserProxy);
			//seleniumProxy.setHttpProxy();
			//seleniumProxy.setSslProxy();
			DesiredCapabilities seleniumCapabilities = new DesiredCapabilities();
			seleniumCapabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
			options.merge(seleniumCapabilities);
			setProxy(browserProxy);
		} else if (input.containsKey("proxyHost") && input.containsKey("proxyPort")) {
			Proxy proxy = new Proxy();
			proxy.setHttpProxy(input.getString("proxyHost")+":"+input.getInt("proxyPort"));
			proxy.setSslProxy(input.getString("proxyHost")+":"+input.getInt("proxyPort"));
			proxy.setNoProxy(input.getString("noProxy",""));
			options.setCapability(CapabilityType.PROXY, proxy);
		}

		//final WebDriver driver = new ChromeDriver(options);

		// Custom profile settings
		if(input.containsKey("user-data-dir")) {
			options.addArguments("user-data-dir=" + input.getString("user-data-dir"));
			options.addArguments("--profile-directory=MyProfile");
		}

		if(input.containsKey("additionalOptions")) {
			options.addArguments(Arrays.asList(input.getString("additionalOptions").split(",")));
		}

		/*setOnlyHTML(false);
		if(input.containsKey("onlyHTML") && input.getBoolean("onlyHTML")) {
			HashMap<String, Object> prefs = new HashMap<String, Object>();
			prefs.put("profile.default_content_setting_values.javascript", 2);
			prefs.put("profile.default_content_setting_values.images", 2);
			options.setExperimentalOption("prefs", prefs);
			setOnlyHTML(true);
		}*/
		String transactionName = "Open_chrome";
		startTransaction(transactionName);

		final WebDriver driver = new ChromeDriver(options);

		driver.manage().timeouts().implicitlyWait(input.getInt("implicitlyWait", 10), TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(input.getInt("pageLoadTimeout", 10), TimeUnit.SECONDS);
		driver.manage().window().setSize(new Dimension(1920, 1080));

		if (input.getBoolean("maximize", false)) {
			driver.manage().window().maximize();
		}

		setDriver(driver);
		stopTransaction(transactionName);
	}
	
	/**
	 * <p>Keyword used to navigate to a page</p>
	 * Inputs (default values):
	 * <ul>
	 * <li>url (https://www.exense.ch): the url to navigate to
	 * </ul>
	 */
	@Keyword
	public void Navigate_to_page()  {
		String transactionName = input.getString("transactionName", "Navigate_to_page");
		String url = input.getString("url", "https://www.exense.ch");
		WebDriver driver = getDriver();
		
		startTransaction(transactionName);
		driver.get(url);
		stopTransaction(transactionName);	
	}

	/**
	 * <p>Keyword used to set the scroll top position of any web element
	 * Inputs (default values):
	 * <ul>
	 * <li>xpath of the element to scroll</li>
	 * <li>scrollTop value to be applied (0 is top, large value fall back to max. i.e. end of the element) 
	 * </ul>
	*/
	@Keyword
	public void Set_ScrollTop() {
		JavascriptExecutor jse = (JavascriptExecutor) this.getDriver();
		String xpath = input.getString("xpath");
		WebElement scrollElement = getDriver().findElement(By.xpath(xpath));
		int scrollTop = Integer.parseInt(input.getString("scrollTop","0"));
		jse.executeScript("arguments[0].scrollTop=arguments[1];", scrollElement,scrollTop);
	}

	
	/**
	 * <p>Keyword used to explicitly close the driver and related Chrome browser. The driver and browser automatically close when the step session ends.</p>
	 */
	@Keyword
	public void Close_chrome() {
		String transactionName = input.getString("transactionName", "Close_chrome");
		WebDriver driver = getDriver();
		startTransaction(transactionName);
		driver.close();
		stopTransaction(transactionName);
	}
	
	/**
	 * <p>Hook method that can be used to manage Keyword unhandled exception</p>
	 * @param e the Exception thrown by the Keyword
	 * @return true to re-throw the Exception e, false to not re-throw 
	 * @see <a href="https://step.exense.ch/knowledgebase/3.12/devdocs/keywordAPI/#onerror-hook">Exense documentation</a>
	 */
	@Override
	public boolean onError(Exception e) {
		if (isDriverCreated()) {
			attachScreenshot();
			attachLogs();
		}
		setFailure();
		return super.onError(e);
	}

	/**
	 * <p>Helper method used to attach the WebDriver and Selenium logs when an error occurs</p>
	 */
	private void attachLogs() {
		Set<String> logTypes = null;
		try {
			logTypes = getDriver().manage().logs().getAvailableLogTypes();
		} catch (ClassCastException e) {
			output.add("Driver returned unexpected logs", getDriver().manage().logs().toString());
			output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
		}
		if (logTypes != null) {
			for (String type: logTypes) {
				LogEntries entries = getDriver().manage().logs().get(type);
				String logs = "";
	
				for (LogEntry entry: entries.getAll()) {
					logs = logs+ new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(entry.getTimestamp()))+";"+entry.getLevel()+";"+entry.getMessage()+"\n";
				}
				if (!"".equals(logs)) {
					output.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(logs.getBytes(), "selenium_"+type+".log"));
				}
			}
		}
	}

	/**
	 * <p>Helper method to attach a screenshot to a Keyword execution with a default name "screenshot.jpg"</p>
	 */
	protected void attachScreenshot() {
		attachScreenshot("screenshot.jpg");
	}

	/**
	 * <p>Helper method to attach a screenshot to a Keyword execution</p>
	 * @param screenshotName the name of the screenshot to attach
	 */
	protected void attachScreenshot(String screenshotName) {
		try {
			byte[] bytes = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.BYTES);
			Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(bytes, screenshotName);
			output.addAttachment(attachment);
		} catch (Exception ex) {
			output.appendError("Unable to generate screenshot");
		}
	}
	
	/**
	 * <p>Helper method to check if a WebDriver has been created and put to a STEP session</p>
	 * @return true if the WebDriver instance is created, otherwise false
	 */
	private boolean isDriverCreated() {
		return (session.get("DriverWrapper") != null);
	}
	
	/**
	 * <p>Helper method to get a WebDriver instance from a STEP session</p>
	 * @return the WebDriver instance from the STEP session
	 */
	protected WebDriver getDriver() {	
		return ((DriverWrapper) session.get("DriverWrapper")).getDriver();
	}
	
	/**
	 * <p>Helper method to put a WebDriver instance into a STEP session</p>
	 * @param driver the WebDriver instance to put in session
	 */
	protected void setDriver(WebDriver driver) {
		session.put("DriverWrapper", new DriverWrapper(driver));
	}
	
	/**
	 * <p>Helper method to put a BrowserMobProxy instance into a STEP session</p>
	 * @param proxy the BrowserMobProxy instance to put in session
	 */
	protected void setProxy(BrowserMobProxy proxy) {
		session.put("ProxyWrapper", new ProxyWrapper(proxy));
	}
	
	/**
	 * Helper method used to stop a Keyword custom transaction. An optional map of measurements data can be passed to add details on the custom transaction.
	 * @param transactionName the name of the custom transaction to stop
	 * @param additionnalMeasurementData the optional map of measurements data to insert into the custom transaction
	 */
	@Override
	protected void stopTransaction(String transactionNameTmp, Map<String, Object> additionnalMeasurementData) {
		super.stopTransaction(transactionNameTmp, additionnalMeasurementData);
		String transactionName = getFullTransactionName(transactionNameTmp);
		if(isDebug()) attachScreenshot(transactionName +".jpg");
	}
	
	private boolean isDebug() {
		return Boolean.valueOf(properties.getOrDefault("debug", "false"));
	}


}
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

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import ch.exense.step.library.commons.BusinessException;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.Keyword;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Central class containing the STEP Selenium Keywords and helper methods used to start / stop a Chrome instance via chromedriver.
 * Please have a look at the <a href="https://step.exense.ch/knowledgebase/3.15/userdocs/keywords/">Exense documentation</a> to learn how to use Keywords.
 */
public class AbstractSeleniumKeyword extends AbstractEnhancedKeyword {

	/**
	 * <p>Method used for lazy initialization of a page object in a generic way</p>
	 * @return the page object
	 */
	protected <T extends AbstractPageObject> T getPageObject()  {
		T po = (T) session.get("pageObject");
		if (po == null) {
			AbstractPageObject apo = new AbstractPageObject(getDriver());
			session.put("pageObject",new AbstractPageObject(getDriver()));
			return (T) apo;
		}
		return po; 
	}

	/**
	 * <p>Method used for lazy initialization of a page object in a generic way</p>
	 * @return the page object
	 */
	protected <T extends AbstractPageObject> void setPageObject(T setPageObject)  {
		session.put("pageObject",setPageObject);
	}

	private static final String INPUT_TIMEOUT = "Timeout";
	private static final String INPUT_XPATH = "Xpath";
	private static final String INPUT_ID = "Id";
	private static final String INPUT_NAME = "Name";
	protected static final String INPUT_WAIT_FOR_PREFIX = "Wait_For_";

	protected static final String SELENIUM_DEFAULT_TIMEOUT_INPUT = ""
			+"\""+INPUT_TIMEOUT+"\": {\"type\": \"integer\"}";

	protected static final String SELENIUM_DEFAULT_ELEMENT_INPUTS = ""
			+"\""+INPUT_XPATH+"\": {\"type\": \"string\"},"
			+"\""+INPUT_ID+"\": {\"type\": \"string\"},"
			+"\""+INPUT_NAME+"\": {\"type\": \"string\"}";

	protected static final String SELENIUM_DEFAULT_WAIT_FOR_INPUTS = ""
			+"\""+INPUT_WAIT_FOR_PREFIX+INPUT_XPATH+"\": {\"type\": \"string\"},"
			+"\""+INPUT_WAIT_FOR_PREFIX+INPUT_ID+"\": {\"type\": \"string\"},"
			+"\""+INPUT_WAIT_FOR_PREFIX+INPUT_NAME+"\": {\"type\": \"string\"}";

	private static final String INPUT_ACTION_NAME = "Action_Name";
	protected static final String SELENIUM_DEFAULT_ACTION_NAME_INPUT = ""
			+"\""+INPUT_ACTION_NAME+"\": {\"type\": \"string\"}";

	protected static final String SELENIUM_DEFAULT_INPUTS = ""
			+SELENIUM_DEFAULT_ELEMENT_INPUTS +","
			+SELENIUM_DEFAULT_WAIT_FOR_INPUTS +","
			+SELENIUM_DEFAULT_TIMEOUT_INPUT +","
			+SELENIUM_DEFAULT_ACTION_NAME_INPUT;

	protected By getElementFromInput() {
		return getElementFromInput("",true);
	}

	protected By getElementToWaitForFromInput() {
		return getElementFromInput(INPUT_WAIT_FOR_PREFIX,false);
	}

	private By getElementFromInput(String prefix, boolean errorIfNotFound) {
		if (input.containsKey(prefix+INPUT_XPATH)) {
			return By.xpath(input.getString(prefix+INPUT_XPATH));
		} else if (input.containsKey(prefix+INPUT_ID)) {
			return By.id(input.getString(prefix+INPUT_ID));
		} else if (input.containsKey(prefix+INPUT_NAME)) {
			return By.name(input.getString(prefix+INPUT_NAME));
		} else if (errorIfNotFound) {
			throw new BusinessException("Error: could not get the element from the input");
		}
		return null;
	}

	protected long getTimeoutFromInput() {
		return input.containsKey(INPUT_TIMEOUT) ? input.getInt(INPUT_TIMEOUT) : AbstractPageObject.DEFAULT_TIMEOUT;
	}

	protected void waitForElement(AbstractPageObject page, long timeout) {
		By element = getElementToWaitForFromInput();
		if(element!= null) {
			page.safeWait(() -> page.findBy(element).isDisplayed(), timeout);
		}
	}

	/**
	 * <p>Hook method that can be used to manage Keyword unhandled exception</p>
	 * @param e the Exception thrown by the Keyword
	 * @return true to re-throw the Exception e, false to not re-throw 
	 * @see <a href="https://step.exense.ch/knowledgebase/3.15/devdocs/keywordAPI/#onerror-hook">Exense documentation</a>
	 */
	@Override
	public boolean onError(Exception e) {
		if (isDriverCreated()) {
			attachScreenshot();
			attachLogs();
		}
		return super.onError(e);
	}

	/**
	 * <p>Hook method that attach a screenshot after the keyword execution, if the debug mode is activated</p>
	 * @param keywordName the keyword method that was called
	 * @param annotation the annotation of this keyword
	 */
	@Override
	public void afterKeyword(String keywordName, Keyword annotation) {
		if (isDriverCreated() && isDebug()) {
			attachScreenshot();
		}
		super.beforeKeyword(keywordName,annotation);
	}


	/**
	 * <p>Helper method used to attach the WebDriver and Selenium logs when an error occurs</p>
	 */
	private void attachLogs() {
		Set<String> logTypes = getDriver().manage().logs().getAvailableLogTypes();
		if (logTypes != null) {
			for (String type: logTypes) {
				LogEntries entries = getDriver().manage().logs().get(type);
				StringBuilder logs = new StringBuilder();

				for (LogEntry entry: entries.getAll()) {
					logs.append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(entry.getTimestamp()))).
							append(";").append(entry.getLevel()).append(";").append(entry.getMessage()).append("\n");
				}
				if (!"".equals(logs.toString())) {
					output.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(logs.toString().getBytes(), "selenium_"+type+".log"));
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

	protected void closeDriver() {
		WebDriver driver = getDriver();
		startTransaction();
		driver.quit();
		Boolean debug = Boolean.parseBoolean(properties.getOrDefault("debug_selenium", "false"));
		if (debug) {
			properties.put("debug_selenium", "false");
		}
		stopTransaction();
		if (debug) {
			properties.put("debug_selenium", "true");
		}
	}
	
	/**
	 * <p>Helper method to check if a WebDriver has been created and put to a STEP session</p>
	 * @return true if the WebDriver instance is created, otherwise false
	 */
	protected boolean isDriverCreated() {
		return (session.get(DriverWrapper.class) != null);
	}
	
	/**
	 * <p>Helper method to get a WebDriver instance from a STEP session</p>
	 * @return the WebDriver instance from the STEP session
	 */
	protected WebDriver getDriver() {
		WebDriver result = session.get(DriverWrapper.class).getDriver();
		if (result==null) {
			throw new BusinessException("The driver was not created. Please call on of the 'Open_Chrome' or 'Open_Edge' keywords to create a session");
		}
		return result;
	}

	protected void removeDriver() {
		session.put(DriverWrapper.class.getName(),null);
	}
	
	/**
	 * <p>Helper method to put a WebDriver instance into a STEP session</p>
	 * @param driver the WebDriver instance to put in session
	 */
	protected void setDriver(WebDriver driver) {
		session.put(new DriverWrapper(driver));
	}

	public boolean isDebug() {
		return Boolean.parseBoolean(properties.getOrDefault("debug_selenium", "false"));
	}

	/**
	 * Helper method to build the transaction name base on the user inputs
	 * @param transactionName the transaction name to be updated if necessary
	 * @return the potentially updated transaction name
	 */
	protected String getActualTransactionName(String transactionName) {
		String result = transactionName;
		if (input.containsKey(INPUT_ACTION_NAME)) {
			result = input.getString(INPUT_ACTION_NAME);
		} else {
			if (input.containsKey("Name_Prefix")) {
				result = input.getString("Name_Prefix") + result;
			}
			if (input.containsKey("Name_Suffix")) {
				result = result + input.getString("Name_Suffix");
			}
		}
		return result;
	}

	/**
	 * Helper method used to start a Keyword custom transaction
	 * @param defaultTransactionName the name of the custom transaction to start
	 */
	protected void startTransaction(String defaultTransactionName){
		output.startMeasure(getActualTransactionName(defaultTransactionName));
	}

	/**
	 * Helper method used to start a Keyword custom transaction
	 */
	protected void startTransaction(){
		startTransaction(properties.getOrDefault("$keywordName",""));
	}

	/**
	 * Helper method used to stop a Keyword custom transaction. An optional map of measurements data can be passed to add details on the custom transaction.
	 * @param additionalMeasurementData the optional map of measurements data to insert into the custom transaction
	 */
	protected void stopTransaction(Map<String, Object> additionalMeasurementData) {

		Map<String, Object> data = new HashMap<>();
		if(additionalMeasurementData != null && !additionalMeasurementData.isEmpty()) {
			data.putAll(additionalMeasurementData);
		}
		output.stopMeasure(data);

		if (isDebug()) {
			attachScreenshot();
		}
	}

	/**
	 * Helper method used to stop a Keyword custom transaction.
	 * @see #stopTransaction(Map)
	 */
	protected void stopTransaction() {
		stopTransaction(null);
	}

}
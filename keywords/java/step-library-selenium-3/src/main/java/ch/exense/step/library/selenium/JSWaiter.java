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

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Helper class used to wait upon different JavaScript technology executions
 */
public class JSWaiter {
	/**
	 * The WebDriver used to construct the waiter
	 */
	private final WebDriver driver;
	/**
	 * Selenium object used to wait on conditions
	 */
	private final WebDriverWait jsWait;
	/**
	 * Selenium object used to interact with JavaScript
	 */
	private final JavascriptExecutor jsExec;

	/**
	 * Constructor for the JsWaiter
	 * @param driver the WebDriver used to construct a class instance
	 */
	public JSWaiter(WebDriver driver) {
		this.driver = driver;
		jsWait = new WebDriverWait(this.driver, 10);
		jsExec = (JavascriptExecutor) this.driver;
	}


	public void waitAllRequest() {
		waitUntilJSReady();
		ajaxComplete();
		waitUntilJQueryReady();
		//waitUntilAngularReady(); // THROW JQLITE ERROR
		waitUntilAngular5Ready();
	}
	
	/**
	 * Method waiting for JavaScript to complete
	 */
	public boolean waitUntilJSReady() {
		return jsExec.executeScript("return document.readyState").toString().equals("complete");
	}
	
	/**
	 * Helper method waiting for the Ajax calls to be completed
	 */
	public void ajaxComplete() {
		jsExec.executeScript("var callback = arguments[arguments.length - 1];"
				+ "var xhr = new XMLHttpRequest();" + "xhr.open('GET', '/Ajax_call', true);"
				+ "xhr.onreadystatechange = function() {" + "  if (xhr.readyState == 4) {"
				+ "    callback(xhr.responseText);" + "  }" + "};" + "xhr.send();");
	}

	/**
	 * Method waiting for JQuery activity to end, only if enabled
	 */
	public boolean waitUntilJQueryReady() {
		return waitUntilJQueryReady(0);
	}

	/**
	 * Method waiting for JQuery activity to end, only if enabled
	 */
	public boolean waitUntilJQueryReady(int defaultActive) {
		boolean jQueryDefined = (boolean) jsExec.executeScript("return typeof jQuery != 'undefined'");
		boolean dollarDefined = (boolean) jsExec.executeScript("return typeof $ != 'undefined'");
		if(jQueryDefined) {
			return (boolean) jsExec.executeScript("return jQuery.active=="+defaultActive);
		} else if (dollarDefined) {
			return (boolean) jsExec.executeScript("return $.active=="+defaultActive);
		} else {
			return true;
		}
	}

	/**
	 * Method waiting for Angular (version under 5) activity to end, only if enabled
	 */
	public void waitUntilAngularReady() {
		try {
			Boolean angularUnDefined = (Boolean) jsExec.executeScript("return window.angular === undefined");
			if (!angularUnDefined) {
				Boolean angularInjectorUnDefined = (Boolean) jsExec.executeScript("return angular.element(document).injector() === undefined");
				if (!angularInjectorUnDefined) {
					poll(20);
					String angularReadyScript = "return angular.element(document).injector().get('$http').pendingRequests.length === 0";
					angularLoads(angularReadyScript);
					final String javaScriptToLoadAngular =
			                "var injector = window.angular.element('body').injector();" + 
			                "var $http = injector.get('$http');" + 
			                "return ($http.pendingRequests.length === 0)";
					angularLoads(javaScriptToLoadAngular);
					poll(20);
				}
			}
		} catch (WebDriverException ignored) {
		}
	}
	
	/**
	 * Method waiting for Angular version 5 activity to end, only if enabled
	 */
	public boolean waitUntilAngular5Ready() {
		try {
			Object angular5Check = jsExec.executeScript("return getAllAngularRootElements()[0].attributes['ng-version']");
			if (angular5Check == null) return true;
		} catch (WebDriverException e) {
			return true;
		}
		return(boolean) jsExec.executeScript("return window.getAllAngularTestabilities().findIndex(x=>!x.isStable()) === -1");
	}
	
	/**
	 * Helper method waiting for Angular activity to end
	 * @deprecated
	 * @see #waitUntilAngularReady()
	 */
	@Deprecated
	private void waitForAngular() {
        final String javaScriptToLoadAngular =
                "var injector = window.angular.element('body').injector();" + 
                "var $http = injector.get('$http');" + 
                "return ($http.pendingRequests.length === 0)";

        ExpectedCondition<Boolean> pendingHttpCallsCondition =
				driver -> ((JavascriptExecutor) driver).executeScript(javaScriptToLoadAngular).equals(true);
        WebDriverWait wait = new WebDriverWait(driver, 20);
        wait.until(pendingHttpCallsCondition);
    }

	/**
	 * Helper method to load a script
	 * @param angularReadyScript - the script to load
	 */
	private void angularLoads(String angularReadyScript) {
		try {
			ExpectedCondition<Boolean> angularLoad = driver -> Boolean.valueOf(((JavascriptExecutor) driver)
					.executeScript(angularReadyScript).toString());

			boolean angularReady = Boolean.parseBoolean(jsExec.executeScript(angularReadyScript).toString());

			if (!angularReady) {
				jsWait.until(angularLoad);
			}
		} catch (WebDriverException ignored) {
		}
	}


	/**
	 * Helper method used to wait
	 * @param milis - the amount of milliseconds to wait
	 */
	private void poll(long milis) {
		try {
			Thread.sleep(milis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

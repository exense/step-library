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

/**
 * Helper class used to wait upon different JavaScript technology executions
 */
public class JSWaiter {
	/**
	 * The WebDriver used to construct the waiter
	 */
	private final WebDriver driver;

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
		jsExec = (JavascriptExecutor) this.driver;
	}


	public void waitAllRequest(long timeout) {
		Poller.retryWhileFalse(this::waitUntilJSReady, timeout);
		Poller.retryWhileFalse(this::waitUntilJQueryReady, timeout);
		Poller.retryWhileFalse(this::waitUntilAngularJSReady, timeout);
		Poller.retryWhileFalse(this::waitUntilAngular5Ready, timeout);
	}
	
	/**
	 * Method waiting for JavaScript to complete
	 */
	public boolean waitUntilJSReady() {
		return jsExec.executeScript("return document.readyState").toString().equals("complete");
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
	 * Method waiting for AngularJS activity to end, only if enabled
	 */
	public boolean waitUntilAngularJSReady() {
		try {
			Boolean angularUnDefined = (Boolean) jsExec.executeScript("return window.angular === undefined");
			if(angularUnDefined) return true;

			Boolean angularInjectorUnDefined = (Boolean) jsExec.executeScript("return window.angular.element(document).injector() === undefined");
			if(angularInjectorUnDefined) return true;

			String angularReadyScript = "return window.angular.element(document).injector().get('$http').pendingRequests.length === 0";
			return Boolean.parseBoolean(jsExec.executeScript(angularReadyScript).toString());
		} catch (WebDriverException ignored) {
			return true;
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
		return(boolean) jsExec.executeScript("return window.getAllAngularTestabilities().findIndex(x=>!x.isStable()) === -1")
				|| (boolean) jsExec.executeScript("return window.getAllAngularTestabilities().findIndex(x=>!x.isStable()) === 0");
	}
}

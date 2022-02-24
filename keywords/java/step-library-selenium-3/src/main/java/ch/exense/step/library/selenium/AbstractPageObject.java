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

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Base class gathering utility methods to work on pages
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
    public final static long DEFAULT_TIMEOUT = 30;
    protected long timeout = DEFAULT_TIMEOUT;

    /**
     * Constructor for the PageObject
     *
     * @param driver the WebDriver instance to create the page with
     */
    public AbstractPageObject(WebDriver driver) {
        this.driver = driver;
        this.jsWaiter = new JSWaiter(driver);
        this.webDriverWait = new WebDriverWait(driver, getDefaultTimeout());
    }

    /**
     * Constructor for the PageObject
     *
     * @param driver the WebDriver instance to create the page with
     */
    public AbstractPageObject(WebDriver driver, long timeout) {
        this.timeout = timeout;
        this.driver = driver;
        this.jsWaiter = new JSWaiter(driver);
        this.webDriverWait = new WebDriverWait(driver, getDefaultTimeout());
    }

    public long getDefaultTimeout() {
        return timeout;
    }

    public void setDefaultTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Getter to return the WebDriver instance used by the PageObject
     *
     * @return the WebDriver used to create the PageObject
     */
    public WebDriver getDriver() {
        return driver;
    }

    /**
     * Setter for the WebDriver instance used by the PageObject
     *
     * @param driver the WebDriver instance to set
     */
    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Getter to return the WebDriverWait instance used by the PageObject
     *
     * @return the WebDriverWait used to create the PageObject
     */
    public WebDriverWait getWebDriverWait() {
        return this.webDriverWait;
    }

    /**
     * Getter to return the JSWaiter instance used by the PageObject
     *
     * @return the JSWaiter used to create the PageObject
     */
    public JSWaiter getJSWaiter() {
        return this.jsWaiter;
    }

    /**
     * This method is called after each selenium interaction.
     * It can be override to wait for a on the page
     */
    protected void customWait() {
    }

    /**
     * Method used to wait for an IFrame to be available. First switch to default page content.
     *
     * @param by the IFrame locator
     */
    public void waitForFrame(By by) {
        this.driver.switchTo().defaultContent();
        this.webDriverWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(by));
    }

    /**
     * Method used to switch the WebDriver content to an IFrame. First switch to default page content and wait for the IFrame to be available.
     *
     * @param by the IFrame locator
     * @see #waitForFrame(By)
     */
    public void waitForFrameAndSwitchDriver(By by) {
        waitForFrame(by);
        driver.switchTo().frame(findBy(by));
    }

    /**
     * Perform a mouse hover on a web element
     *
     * @param by      the web element locator
     * @param timeout the maximal amount of time in milliseconds to wait when searching for the web element
     */
    public void hover(By by, long timeout) {
        new Actions(driver).moveToElement(findBy(by, timeout)).build().perform();
        customWait();
    }

    /**
     * Perform a mouse hover on a web element, using the default class timeout
     *
     * @param by the web element locator
     * @see #hover(By, long)
     */
    public void hover(By by) {
        hover(by, getDefaultTimeout());
    }

    /**
     * Method to find a web element by locator
     *
     * @param by      the web element locator
     * @param timeout the maximal amount of time in milliseconds to wait when searching for the web element
     * @return the web element
     * @see #doWithoutImplicitWait(Callable)
     * @see Poller#retryIfFails(Supplier, long)
     */
    public WebElement findBy(By by, long timeout) {
        return Poller.retryIfFails(() -> driver.findElement(by), timeout);
    }

    /**
     * Method to find a web element by locator, using the default class timeout
     *
     * @param by the web element locator
     * @return the web element
     * @see #findBy(By, long)
     */
    public WebElement findBy(By by) {
        return doWithoutImplicitWait(() -> Poller.retryIfFails(() -> driver.findElement(by), 0));
    }

    /**
     * Method to find a list of web elements by locator
     *
     * @param by      the web elements locator
     * @param timeout the maximal amount of time in milliseconds to wait when searching for the web elements
     * @return a list of web elements
     * @see #doWithoutImplicitWait(Callable)
     * @see Poller#retryIfFails(Supplier, long)
     */
    public List<WebElement> findAllBy(By by, long timeout) {
        return Poller.retryIfFails(() -> driver.findElements(by), timeout);
    }

    /**
     * Method to find a list of web elements by locator, using the default class timeout
     *
     * @param by the web elements locator
     * @return a list of web elements
     * @see #findAllBy(By, long)
     */
    public List<WebElement> findAllBy(By by) {
        return doWithoutImplicitWait(() -> Poller.retryIfFails(() -> driver.findElements(by), 0));
    }

    /**
     * Method to check the validity of a boolean condition
     *
     * @param condition the boolean condition to check
     * @param timeout   the maximal amount of time in milliseconds to wait when trying to check the condition validity
     */
    public void safeWait(Supplier<Boolean> condition, long timeout) {
        Poller.retryWhileFalse(condition, timeout);
    }

    /**
     * Method to check the validity of a boolean condition, using the default class timeout
     *
     * @param condition the boolean condition to check
     * @see #safeWait(Supplier, long)
     */
    public void safeWait(Supplier<Boolean> condition) {
        safeWait(condition, getDefaultTimeout());
    }

    /**
     * Method to wait for the AJAX and JavaScript calls to be completed
     *
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
     *
     * @see #safeWaitDocumentReadyState(long)
     */
    public void safeWaitDocumentReadyState() {
        safeWaitDocumentReadyState(getDefaultTimeout());
    }

    /**
     * Method used to click on a web element in a safe manner
     *
     * @param by      the web element locator
     * @param timeout the maximal amount of time to wait when trying to click on the web element
     * @see Poller#retryIfFails(Supplier, long)
     */
    public void safeClick(By by, long timeout) {
        Poller.retryIfFails(() -> {
            WebElement element = driver.findElement(by);
            element.click();
            customWait();
            return true;
        }, timeout);
    }

    /**
     * Method used to click on a web element in a safe manner, using the default class timeout
     *
     * @param by the web element locator
     * @see #safeClick(By, long)
     */
    public void safeClick(By by) {
        safeClick(by, getDefaultTimeout());
    }

    public void safeClick(String[] selectors, long timeout) {
        Poller.retryIfFails(() -> {
            WebElement element = expandShadowPath(selectors);
            element.click();
            customWait();
            return true;
        }, timeout);
    }

    public void safeClick(String[] selectors) {
        safeClick(selectors, getDefaultTimeout());
    }

    /**
     * Method used to hover on a web element in a safe manner
     *
     * @param by      the web element locator
     * @param timeout the maximal amount of time to wait when trying to click on the web element
     * @see Poller#retryIfFails(Supplier, long)
     */
    public void safeHover(By by, long timeout) {
        Actions actions = new Actions(driver);
        Poller.retryIfFails(() -> {
            WebElement element = driver.findElement(by);
            actions.moveToElement(element).build().perform();
            customWait();
            return true;
        }, timeout);
    }

    public void safeHover(String[] selectors, long timeout) {
        Actions actions = new Actions(driver);
        Poller.retryIfFails(() -> {
            WebElement element = expandShadowPath(selectors);
            actions.moveToElement(element).build().perform();
            customWait();
            return true;
        }, timeout);
    }

    /**
     * Method used to click on a web element in a safe manner, using the default class timeout
     *
     * @param by the web element locator
     * @see #safeHover(By, long)
     */
    public void safeHover(By by) {
        safeHover(by, getDefaultTimeout());
    }

    public void safeHover(String[] selectors) {
        safeHover(selectors, getDefaultTimeout());
    }

    public void executeJavascript(String javascriptToExecute,WebElement element) {
        ((JavascriptExecutor) driver).executeScript(javascriptToExecute, element);
        customWait();
    }

    public void executeJavascript(String javascriptToExecute) {
        ((JavascriptExecutor) driver).executeScript(javascriptToExecute);
        customWait();
    }

    public void javascriptClick(By by) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", findBy(by));
        customWait();
    }

    public void javascriptClick(String[] selectors) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", expandShadowPath(selectors));
        customWait();
    }

    public void javascriptDoubleClick(By by) {
        ((JavascriptExecutor) driver).executeScript("var clickEvent  = document.createEvent ('MouseEvents');\n" +
                "clickEvent.initEvent ('dblclick', true, true);\n" +
                "arguments[0].dispatchEvent (clickEvent);", findBy(by));
        customWait();
    }

    public void javascriptDoubleClick(String[] selectors) {
        ((JavascriptExecutor) driver).executeScript("var clickEvent  = document.createEvent ('MouseEvents');\n" +
                "clickEvent.initEvent ('dblclick', true, true);\n" +
                "arguments[0].dispatchEvent (clickEvent);", expandShadowPath(selectors));
        customWait();
    }

    /**
     * Method used to fulfill an input web element in a safe manner. First clear the input web element content.
     *
     * @param by      the element to send the keys to
     * @param timeout the maximal amount of time to wait when trying to send the keys to the web element
     * @see AbstractPageObject#safeWait(Supplier, long)
     */
    public void safeDoubleClick(By by, long timeout) {
        safeWait(() -> {
            WebElement element = driver.findElement(by);
            Actions actions = new Actions(driver);
            actions.doubleClick(element).perform();
            customWait();
            return true;
        }, timeout);
    }

    public void safeDoubleClick(String[] selectors, long timeout) {
        safeWait(() -> {
            WebElement element = expandShadowPath(timeout, selectors);
            Actions actions = new Actions(driver);
            actions.doubleClick(element).perform();
            customWait();
            return true;
        }, timeout);
    }

    /**
     * Method used to fulfill an input web element in a safe manner. First clear the input web element content.
     *
     * @param by      the element to send the keys to
     * @param keys    the value to insert
     * @param timeout the maximal amount of time to wait when trying to send the keys to the web element
     * @see AbstractPageObject#safeWait(Supplier, long)
     */
    public void safeSendKeys(By by, String keys, Supplier<Boolean> condition, long timeout) {
        safeWait(() -> {
            WebElement element = driver.findElement(by);
            element.clear();
            element.sendKeys(keys);
            customWait();
            return (condition != null) ? condition.get() : true;
        }, timeout);
    }

    public void safeSendKeys(String[] selectors, String keys, Supplier<Boolean> condition, long timeout) {
        safeWait(() -> {
            WebElement element = expandShadowPath(selectors);
            element.clear();
            element.sendKeys(keys);
            customWait();
            return (condition != null) ? condition.get() : true;
        }, timeout);
    }

    public void safeSendKeys(By by, Keys keys, Supplier<Boolean> condition, long timeout) {
        safeWait(() -> {
            WebElement element = driver.findElement(by);
            element.clear();
            element.sendKeys(keys);
            customWait();
            return (condition != null) ? condition.get() : true;
        }, timeout);
    }

    public void safeSendKeys(By by, String keys, long timeout) {
        safeSendKeys(by, keys, () -> findBy(by).getAttribute("value").equals(keys), timeout);
    }

    public void safeSendKeys(By by, Keys keys, long timeout) {
        safeSendKeys(by, keys, null, timeout);
    }

    public void safeSendKeys(String[] selectors, String keys, long timeout) {
        safeSendKeys(selectors, keys, () -> expandShadowPath(selectors).getAttribute("value").equals(keys), timeout);
    }

    /**
     * Method used to fulfill an input web element in a safe manner, using the default class timeout
     *
     * @param by   the element to send the keys to
     * @param keys the value to insert
     * @see #safeSendKeys(By, String, long)
     */
    public void safeSendKeys(By by, String keys) {
        safeSendKeys(by, keys, getDefaultTimeout());
    }

    public void safeSendKeys(By by, Keys keys) {
        safeSendKeys(by, keys, getDefaultTimeout());
    }

    /**
     * Method used to perform an action through a Callable instance bypassing the default class timeout
     *
     * @param <T>      the type of object returned by the callable
     * @param callable the Callable instance to be called
     * @return the object returned by the Callable execution
     * @see java.util.concurrent.Callable
     */
    public <T> T doWithoutImplicitWait(Callable<T> callable) {
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            driver.manage().timeouts().implicitlyWait(getDefaultTimeout(), TimeUnit.SECONDS);
        }
    }

    /**
     * Method used to perform an action through a Runnable instance bypassing the default class timeout
     *
     * @param runnable the runnable to execute
     * @see java.lang.Runnable
     */
    public void doWithoutImplicitWait(Runnable runnable) {
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            driver.manage().timeouts().implicitlyWait(getDefaultTimeout(), TimeUnit.SECONDS);
        }
    }

    /**
     * Generic method to hover on an element on a page, then wait for the page to be loaded. Used for instance to expand a menu
     *
     * @param hoverElementXPath the xpath to the element menu to click on
     * @param xpathToCheck      the xpath to check once the DOM is ready
     * @see AbstractPageObject#hover(By)
     * @see AbstractPageObject#safeWaitDocumentReadyState()
     */
    public void hoverElement(String hoverElementXPath, String xpathToCheck) {
        hover(By.xpath(hoverElementXPath));
        safeWaitDocumentReadyState();
        safeWait(() -> findBy(By.xpath(xpathToCheck)).isEnabled());
    }

    public WebElement expandShadowPath(String[]... cssSelectorPath) {
        return expandShadowPath(getDefaultTimeout(), cssSelectorPath);
    }

    public WebElement expandShadowPath(long timeout, String[]... cssSelectorPath) {
        List<String> fullPath = toFullPathList(cssSelectorPath);
        WebDriver driver = getDriver();
        return doWithoutImplicitWait(() -> Poller.retryIfFails(() -> expandShadowPath(fullPath, driver), timeout));
    }

    private WebElement expandShadowPath(List<String> cssSelectorPath, WebDriver driver) {
        return expandShadowPath(cssSelectorPath, driver, null);
    }

    private WebElement expandShadowPath(List<String> cssSelectorPath, WebDriver driver, WebElement fromElement) {
        ArrayList<String> pathWithoutLastElement = new ArrayList<>(cssSelectorPath);
        String lastSelector = pathWithoutLastElement.remove(pathWithoutLastElement.size() - 1);

        WebElement current = fromElement;
        for (String cssSelector : pathWithoutLastElement) {
            current = expandRootElement(driver, current, cssSelector);
        }
        return lastSelector == null || lastSelector.isEmpty() ? current : current.findElement(By.cssSelector(lastSelector));
    }

    private WebElement expandRootElement(WebDriver driver, WebElement element, String cssSelector) {
        if (element == null) {
            return expandRootElement(driver, driver.findElement(By.cssSelector(cssSelector)));
        } else {
            return expandRootElement(driver, element.findElement(By.cssSelector(cssSelector)));
        }
    }

    public WebElement expandRootElement(WebDriver driver, WebElement element) {
        Object shadowRoot = ((JavascriptExecutor) driver).executeScript("return arguments[0].shadowRoot", element);
        return shadowRootToWebElement(shadowRoot);
    }

    private WebElement shadowRootToWebElement(Object shadowRoot) {
        WebElement returnObj;

        if (shadowRoot instanceof WebElement) {
            // Chromedriver 95-
            returnObj = (WebElement) shadowRoot;
        } else if (shadowRoot instanceof Map) {
            // Chromedriver 96+
            @SuppressWarnings("unchecked")
            Map<String, Object> shadowRootMap = (Map<String, Object>) shadowRoot;
            String shadowRootKey = (String) shadowRootMap.keySet().toArray()[0];
            String id = (String) shadowRootMap.get(shadowRootKey);
            RemoteWebElement remoteWebElement = new RemoteWebElement();
            remoteWebElement.setParent((RemoteWebDriver) driver);
            remoteWebElement.setId(id);
            returnObj = remoteWebElement;
        } else {
            throw new RuntimeException("Unexpected return type for shadowRoot in expandRootElement");
        }
        return returnObj;
    }

    private ArrayList<String> toFullPathList(String[][] cssSelectorPath) {
        ArrayList<String> fullPath = new ArrayList<>();
        Arrays.stream(cssSelectorPath).forEach(partialPath -> fullPath.addAll(List.of(partialPath)));
        return fullPath;
    }


    /**
     * Method to exit a iframe
     *
     * @see AbstractPageObject#waitForFrameAndSwitchDriver(By)
     */
    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    public void switchToWindow(String handle) {
        driver.switchTo().window(handle);
    }
}
